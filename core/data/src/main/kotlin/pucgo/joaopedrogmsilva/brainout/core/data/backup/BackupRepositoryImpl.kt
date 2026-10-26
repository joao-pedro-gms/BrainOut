// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.backup

import kotlinx.coroutines.flow.first
import pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase
import pucgo.joaopedrogmsilva.brainout.core.data.session.AccountAccess
import pucgo.joaopedrogmsilva.brainout.core.domain.error.BackupRestoreException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.BackupTooLargeException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.BackupSummary
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupAccountOption
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupReminder
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupRestoreResult
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ListingPreferencesRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Backup pessoal cifrado. A restauração nunca cria, apaga ou modifica credenciais. */
@Singleton
class BackupRepositoryImpl
    @Inject
    constructor(
        private val database: BrainOutDatabase,
        private val access: AccountAccess,
        private val listingPreferencesRepository: ListingPreferencesRepository,
    ) : BackupRepository {
        private val dao get() = database.backupDao()

        override suspend fun exportTo(
            output: OutputStream,
            passphrase: String,
        ): BackupSummary {
            val owner = access.currentId()
            val payload = access.write(owner) { readPayload(owner) }
            val now = Instant.now()
            val encrypted = BackupCrypto.encrypt(BackupPayloadCodec.encode(payload), passphrase)
            val envelope = BackupFormat.encodeEnvelope(now.toEpochMilli(), encrypted)
            if (envelope.size >
                BackupFormat.MAX_BACKUP_BYTES
            ) {
                throw BackupTooLargeException(BackupFormat.MAX_BACKUP_BYTES)
            }
            access.write(owner) {
                output.write(envelope)
                output.flush()
            }
            return payload.summary(now)
        }

        override suspend fun inspect(
            input: InputStream,
            passphrase: String,
        ): List<BackupAccountOption> {
            val owner = access.currentId()
            val payload = read(input, passphrase)
            access.requireOwner(owner)
            return payload.users.map { user ->
                BackupAccountOption(user.id, user.username.ifEmpty { "${user.name} (${user.email})" })
            }
        }

        override suspend fun restoreFrom(
            input: InputStream,
            passphrase: String,
            sourceUserId: String?,
            expectedUserId: String?,
        ): BackupRestoreResult {
            val owner = expectedUserId ?: access.currentId()
            access.requireOwner(owner)
            val payload = read(input, passphrase)
            val source =
                sourceUserId ?: payload.users.singleOrNull()?.id
                    ?: throw BackupRestoreException("Selecione uma conta do arquivo")
            val selected = payload.forAccount(source)
            val remapped = selected.remapTo(owner)
            access.write(owner) {
                dao.deleteProjectsForOwner(owner)
                dao.deleteTagsForOwner(owner)
                dao.insertProjects(remapped.projects.map { it.toEntity().also { row -> row.toDomain() } })
                dao.insertTasks(remapped.tasks.map { it.toEntity().also { row -> row.toDomain() } })
                dao.insertTags(remapped.tags.map { it.toEntity().also { row -> row.toDomain() } })
                dao.insertProjectTags(remapped.projectTags.map { it.toEntity() })
                val order = selected.preferences.sortOrderByUser[source] ?: SortOrder.CreatedDesc.toStorageKey()
                listingPreferencesRepository.setSortOrder(owner, SortOrder.fromStorageKey(order))
                listingPreferencesRepository.setSearchQuery(owner, "")
                listingPreferencesRepository.setSelectedTagId(owner, null)
            }
            return BackupRestoreResult(
                ownerId = owner,
                summary = remapped.summary(Instant.now()),
                taskIds = remapped.tasks.map { it.id }.toSet(),
                reminders =
                    remapped.tasks.filter { it.status != "DONE" }.mapNotNull { task ->
                        task.dueDateEpochMillis?.let { BackupReminder(task.id, Instant.ofEpochMilli(it)) }
                    },
            )
        }

        override suspend fun currentTaskIds(): Set<String> {
            val owner = access.currentId()
            return access.write(owner) {
                val projects =
                    dao
                        .getAllProjects()
                        .filter { it.ownerId == owner }
                        .map { it.id }
                        .toSet()
                dao
                    .getAllTasks()
                    .filter { it.projectId in projects }
                    .map { it.id }
                    .toSet()
            }
        }

        private suspend fun readPayload(owner: String): BackupPayload {
            val user = database.userDao().findById(owner) ?: throw BackupRestoreException("Conta não encontrada")
            val projects = dao.getAllProjects().filter { it.ownerId == owner }
            val projectIds = projects.map { it.id }.toSet()
            val tags = dao.getAllTags().filter { it.ownerId == owner }
            val tagIds = tags.map { it.id }.toSet()
            return BackupPayload(
                users = listOf(user.toBackup()),
                projects = projects.map { it.toBackup() },
                tasks =
                    dao
                        .getAllTasks()
                        .filter { it.projectId in projectIds }
                        .map { it.toBackup().copy(assigneeId = it.assigneeId?.takeIf { id -> id == owner }) },
                tags = tags.map { it.toBackup() },
                projectTags =
                    dao
                        .getAllProjectTags()
                        .filter { it.project_id in projectIds && it.tag_id in tagIds }
                        .map { it.toBackup() },
                preferences =
                    BackupPreferences(
                        "system",
                        mapOf(
                            owner to
                                listingPreferencesRepository
                                    .observe(owner)
                                    .first()
                                    .sortOrder
                                    .toStorageKey(),
                        ),
                    ),
            )
        }

        private suspend fun read(
            input: InputStream,
            passphrase: String,
        ): BackupPayload {
            val bytes = readBackupBytes(input)
            val envelope = BackupFormat.decodeEnvelope(bytes)
            val decrypted =
                BackupCrypto.decrypt(
                    EncryptedPayload(envelope.salt, envelope.iv, envelope.ciphertext),
                    passphrase,
                )
            val payload = BackupPayloadCodec.decode(decrypted)
            payload.validateAccounts()
            if (envelope.formatVersion == 2 &&
                payload.users.size != 1
            ) {
                throw BackupRestoreException("Backup pessoal deve conter uma conta")
            }
            return payload
        }
    }

private const val READ_CHUNK_BYTES = 8192

private fun readBackupBytes(input: InputStream): ByteArray {
    val buffer = ByteArrayOutputStream()
    val chunk = ByteArray(READ_CHUNK_BYTES)
    while (true) {
        val count = input.read(chunk)
        if (count < 0) break
        if (buffer.size() + count >
            BackupFormat.MAX_BACKUP_BYTES
        ) {
            throw BackupTooLargeException(BackupFormat.MAX_BACKUP_BYTES)
        }
        buffer.write(chunk, 0, count)
    }
    return buffer.toByteArray()
}
