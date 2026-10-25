// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.backup

import android.database.sqlite.SQLiteConstraintException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase
import pucgo.joaopedrogmsilva.brainout.core.data.local.LocalTransaction
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.BackupDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectTagCrossRef
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TagEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TaskEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.UserEntity
import pucgo.joaopedrogmsilva.brainout.core.data.session.SessionStore
import pucgo.joaopedrogmsilva.brainout.core.data.util.logDebug
import pucgo.joaopedrogmsilva.brainout.core.data.util.logError
import pucgo.joaopedrogmsilva.brainout.core.domain.error.BackupRestoreException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.BackupTooLargeException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.BackupSummary
import pucgo.joaopedrogmsilva.brainout.core.domain.model.ThemeMode
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupReminder
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupRestoreResult
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ListingPreferencesRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.PasswordHasher
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ThemePreferencesRepository
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BrainOut:BackupRepo"

/**
 * Implementação Room do backup local exportável (issue #214 — LOCAL-03).
 *
 * ## Exportação
 *
 * Lê o conteúdo do banco e das preferências, serializa em JSON,
 * cifra com AES-256-GCM e escreve o envelope no [OutputStream]
 * fornecido pelo chamador (Storage Access Framework). O hash de senha
 * **nunca** entra no arquivo — a conta é exportada sem material de
 * credencial.
 *
 * ## Restauração
 *
 * Valida o arquivo **antes** de tocar no banco e grava o conteúdo
 * restaurado em uma única transação Room: ou entra tudo, ou nada
 * entra. Depois da transação, as preferências são reaplicadas e a
 * sessão ativa é invalidada (o usuário precisa entrar de novo com a
 * senha do backup ou a senha que já existia no aparelho).
 *
 * ## Threads
 *
 * As operações de leitura/escrita nos `InputStream`/`OutputStream` são
 * bloqueantes e por isso o chamador deve invocar este caso de uso
 * dentro de um contexto de I/O (`Dispatchers.IO`). A camada de dados
 * não troca o dispatcher — o acesso ao Room já é `suspend` e roda no
 * dispatcher interno do Room.
 */
@Singleton
class BackupRepositoryImpl
    @Inject
    constructor(
        private val database: BrainOutDatabase,
        private val localTransaction: LocalTransaction,
        private val passwordHasher: PasswordHasher,
        private val themePreferencesRepository: ThemePreferencesRepository,
        private val listingPreferencesRepository: ListingPreferencesRepository,
        private val sessionStore: SessionStore,
    ) : BackupRepository {
        /** DAO de operações em massa do backup. */
        private val backupDao: BackupDao get() = database.backupDao()

        override suspend fun exportTo(
            output: OutputStream,
            passphrase: String,
        ): BackupSummary {
            val payload = readPayload()
            val plaintext = BackupPayloadCodec.encode(payload)
            val encrypted = BackupCrypto.encrypt(plaintext, passphrase)
            val createdAt = System.currentTimeMillis()
            val envelope = BackupFormat.encodeEnvelope(createdAt, encrypted)
            if (envelope.size > BackupFormat.MAX_BACKUP_BYTES) {
                throw BackupTooLargeException(BackupFormat.MAX_BACKUP_BYTES)
            }
            output.write(envelope)
            output.flush()
            logDebug(
                TAG,
                "Backup exportado: ${payload.users.size} contas, ${payload.projects.size} projetos, " +
                    "${payload.tasks.size} tarefas, ${payload.tags.size} etiquetas",
            )
            return BackupSummary(
                createdAt = Instant.ofEpochMilli(createdAt),
                users = payload.users.size,
                projects = payload.projects.size,
                tasks = payload.tasks.size,
                tags = payload.tags.size,
            )
        }

        override suspend fun restoreFrom(
            input: InputStream,
            passphrase: String,
        ): BackupRestoreResult {
            val bytes = readBounded(input)
            val envelope = BackupFormat.decodeEnvelope(bytes)
            val decrypted =
                BackupCrypto.decrypt(
                    EncryptedPayload(
                        salt = envelope.salt,
                        iv = envelope.iv,
                        ciphertext = envelope.ciphertext,
                    ),
                    passphrase,
                )
            val payload = BackupPayloadCodec.decode(decrypted)
            validate(payload)
            return applyPayload(payload, passphrase)
        }

        override suspend fun currentTaskIds(): Set<String> = backupDao.getAllTaskIds().toSet()

        // --- Exportação ---

        /** Monta o payload lógico a partir do estado atual do aparelho. */
        private suspend fun readPayload(): BackupPayload {
            val users = backupDao.getAllUsers()
            val projects = backupDao.getAllProjects()
            val tasks = backupDao.getAllTasks()
            val tags = backupDao.getAllTags()
            val projectTags = backupDao.getAllProjectTags()
            val themeMode = themePreferencesRepository.observe().first().toStorageKey()
            val sortOrderByUser =
                users.associate { user ->
                    val order = listingPreferencesRepository.observe(user.id).first().sortOrder
                    user.id to order.toStorageKey()
                }
            return BackupPayload(
                users = users.map { it.toBackup() },
                projects = projects.map { it.toBackup() },
                tasks = tasks.map { it.toBackup() },
                tags = tags.map { it.toBackup() },
                projectTags = projectTags.map { it.toBackup() },
                preferences =
                    BackupPreferences(
                        themeMode = themeMode,
                        sortOrderByUser = sortOrderByUser,
                    ),
            )
        }

        // --- Restauração ---

        /**
         * Lê o arquivo respeitando o limite de tamanho.
         *
         * A leitura para assim que o limite é ultrapassado — um
         * arquivo enorme não é carregado inteiro na memória.
         */
        private suspend fun readBounded(input: InputStream): ByteArray {
            val buffer = ByteArrayOutputStream()
            val chunk = ByteArray(READ_CHUNK_BYTES)
            var total = 0
            while (true) {
                val read = input.read(chunk)
                if (read <= 0) break
                total += read
                if (total > BackupFormat.MAX_BACKUP_BYTES) {
                    throw BackupTooLargeException(BackupFormat.MAX_BACKUP_BYTES)
                }
                buffer.write(chunk, 0, read)
            }
            return buffer.toByteArray()
        }

        /**
         * Confere que os vínculos do backup são coerentes.
         *
         * Roda **antes** de qualquer escrita: um arquivo adulterado
         * (projeto sem dono, tarefa sem projeto) é recusado sem tocar
         * nos dados existentes.
         */
        private fun validate(payload: BackupPayload) {
            val userIds = payload.users.map { it.id }.toSet()
            val projectIds = payload.projects.map { it.id }.toSet()
            val tagIds = payload.tags.map { it.id }.toSet()
            val orphanProject = payload.projects.firstOrNull { it.ownerId !in userIds }
            val orphanTask = payload.tasks.firstOrNull { it.projectId !in projectIds }
            val orphanTag = payload.tags.firstOrNull { it.ownerId !in userIds }
            val orphanLink = payload.projectTags.firstOrNull { it.projectId !in projectIds || it.tagId !in tagIds }
            if (orphanProject != null || orphanTask != null || orphanTag != null || orphanLink != null) {
                throw BackupRestoreException("O backup contém referências para registros inexistentes.")
            }
        }

        /** Grava o payload restaurado em uma transação única. */
        private suspend fun applyPayload(
            payload: BackupPayload,
            passphrase: String,
        ): BackupRestoreResult {
            val restoredUsers = buildUsers(payload, passphrase)
            val restoredProjects = payload.projects.map { it.toEntity() }
            val restoredTasks = payload.tasks.map { it.toEntity() }
            val restoredTags = payload.tags.map { it.toEntity() }
            val restoredLinks = payload.projectTags.map { it.toEntity() }
            writeAll(restoredUsers, restoredProjects, restoredTasks, restoredTags, restoredLinks)
            applyPreferences(payload)
            invalidateSession()
            return BackupRestoreResult(
                summary =
                    BackupSummary(
                        createdAt = Instant.now(),
                        users = restoredUsers.size,
                        projects = restoredProjects.size,
                        tasks = restoredTasks.size,
                        tags = restoredTags.size,
                    ),
                taskIds = restoredTasks.map { it.id }.toSet(),
                reminders =
                    restoredTasks.mapNotNull { task ->
                        task.dueDate?.let { dueDate -> BackupReminder(taskId = task.id, dueDate = dueDate) }
                    },
            )
        }

        /**
         * Reconstrói as contas restauradas.
         *
         * A conta que **já existe** no aparelho conserva o hash local
         * (ele foi derivado com o pepper deste aparelho); a conta nova
         * recebe a senha do backup, derivada com o pepper local. O
         * pepper do Android Keystore nunca sai do aparelho.
         */
        private suspend fun buildUsers(
            payload: BackupPayload,
            passphrase: String,
        ): List<UserEntity> {
            val existingHashes = backupDao.getAllUsers().associate { it.id to it.passwordHash }
            return payload.users.map { user ->
                val hash = existingHashes[user.id] ?: passwordHasher.hash(passphrase)
                user.toEntity(hash)
            }
        }

        /** Substitui todo o conteúdo local em uma única transação. */
        private suspend fun writeAll(
            users: List<UserEntity>,
            projects: List<ProjectEntity>,
            tasks: List<TaskEntity>,
            tags: List<TagEntity>,
            links: List<ProjectTagCrossRef>,
        ) {
            try {
                localTransaction.run {
                    backupDao.deleteAllProjectTags()
                    backupDao.deleteAllTasks()
                    backupDao.deleteAllProjects()
                    backupDao.deleteAllTags()
                    backupDao.deleteAllUsers()
                    backupDao.insertUsers(users)
                    backupDao.insertProjects(projects)
                    backupDao.insertTasks(tasks)
                    backupDao.insertTags(tags)
                    backupDao.insertProjectTags(links)
                }
            } catch (error: SQLiteConstraintException) {
                logError(TAG, "Backup viola uma restrição do banco local", error)
                throw BackupRestoreException("O backup contém registros que violam as regras do aplicativo.")
            }
        }

        /**
         * Reaplica o tema e a ordenação gravados no backup.
         *
         * Melhor-esforço: a restauração já gravou o banco, então uma
         * preferência que falha de Apply não deve desfazer o sucesso —
         * mas o cancelamento da corrotina **propaga**, senão a
         * restauração seria reportada como bem-sucedida em uma
         * corrotina que já estava morta.
         */
        private suspend fun applyPreferences(payload: BackupPayload) {
            try {
                themePreferencesRepository.set(ThemeMode.fromStorageKey(payload.preferences.themeMode))
                payload.preferences.sortOrderByUser.forEach { (userId, order) ->
                    listingPreferencesRepository.setSortOrder(userId, SortOrder.fromStorageKey(order))
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logError(TAG, "Falha ao reaplicar as preferências do backup", error)
            }
        }

        /**
         * Invalida a sessão ativa.
         *
         * O backup substitui as contas do aparelho — a sessão
         * persistida pode apontar para um registro que não existe
         * mais. Encerrá-la é a decisão segura: o usuário entra de
         * novo. A flag de permissão de notificação **não** é removida
         * (é independente da sessão).
         */
        private suspend fun invalidateSession() {
            try {
                sessionStore.clearSession()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logError(TAG, "Falha ao invalidar a sessão após a restauração", error)
            }
        }

        private companion object {
            /** Tamanho do bloco de leitura do arquivo (64 KiB). */
            const val READ_CHUNK_BYTES: Int = 64 * 1024
        }
    }
