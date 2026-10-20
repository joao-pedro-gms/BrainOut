// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectTagCrossRef
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TagEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TaskEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.UserEntity
import pucgo.joaopedrogmsilva.brainout.core.data.preferences.ListingPreferencesRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.preferences.ThemePreferencesRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.security.FixedPepperProvider
import pucgo.joaopedrogmsilva.brainout.core.data.security.PasswordHasherImpl
import pucgo.joaopedrogmsilva.brainout.core.data.session.SessionStore
import pucgo.joaopedrogmsilva.brainout.core.data.session.authDataStore
import pucgo.joaopedrogmsilva.brainout.core.domain.error.BackupFormatException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.BackupIntegrityException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.BackupRestoreException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.BackupTooLargeException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.UnsupportedBackupVersionException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.ThemeMode
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.PasswordHasher
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.Instant

/**
 * Testes do [BackupRepositoryImpl] — o backup local exportável da
 * issue #214 (LOCAL-03).
 *
 * O cenário central é a **instalação distinta**: o backup exportado
 * de um banco é restaurado em outro banco (outra "instalação"), com
 * outro pepper, e os ids/vínculos precisam sobreviver. Os demais
 * testes cobrem a recusa de arquivo inválido, truncado, com versão
 * desconhecida ou acima do limite — sempre **sem** alterar os dados
 * já existentes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class BackupRepositoryImplTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        runTest { SessionStore(context.authDataStore).clear() }
    }

    @Test
    fun `export e restore preservam ids e vinculos em outra instalacao`() =
        runTest {
            val source = newDatabase()
            val target = newDatabase()
            try {
                seed(source)
                val exported = ByteArrayOutputStream()
                newRepository(source, PEPPER_SOURCE).exportTo(exported, PASSPHRASE)

                val result =
                    newRepository(
                        target,
                        PEPPER_TARGET,
                    ).restoreFrom(exported.toByteArray().inputStream(), PASSPHRASE)

                assertThat(result.summary.users).isEqualTo(1)
                assertThat(result.summary.projects).isEqualTo(1)
                assertThat(result.summary.tasks).isEqualTo(1)
                assertThat(result.summary.tags).isEqualTo(1)
                assertThat(target.backupDao().getAllUsers().map { it.id }).containsExactly(USER_ID)
                assertThat(target.backupDao().getAllProjects().map { it.id }).containsExactly(PROJECT_ID)
                assertThat(target.backupDao().getAllTasks().map { it.id }).containsExactly(TASK_ID)
                assertThat(target.backupDao().getAllTags().map { it.id }).containsExactly(TAG_ID)
                assertThat(target.backupDao().getAllProjectTags())
                    .containsExactly(ProjectTagCrossRef(project_id = PROJECT_ID, tag_id = TAG_ID))
                val task = target.taskDao().findById(TASK_ID)
                assertThat(task?.projectId).isEqualTo(PROJECT_ID)
                assertThat(task?.title).isEqualTo("Tarefa")
                assertThat(task?.dueDate).isEqualTo(DUE_DATE)
            } finally {
                source.close()
                target.close()
            }
        }

    @Test
    fun `conta restaurada autentica com a senha do backup no aparelho novo`() =
        runTest {
            val source = newDatabase()
            val target = newDatabase()
            try {
                seed(source)
                val exported = ByteArrayOutputStream()
                newRepository(source, PEPPER_SOURCE).exportTo(exported, PASSPHRASE)

                newRepository(target, PEPPER_TARGET).restoreFrom(exported.toByteArray().inputStream(), PASSPHRASE)

                val restored = target.userDao().findById(USER_ID)
                assertThat(restored).isNotNull()
                val hasher = PasswordHasherImpl(FixedPepperProvider(PEPPER_TARGET))
                assertThat(hasher.verify(PASSPHRASE, restored!!.passwordHash)).isTrue()
                assertThat(hasher.verify("outra-senha", restored.passwordHash)).isFalse()
            } finally {
                source.close()
                target.close()
            }
        }

    @Test
    fun `conta que ja existe no aparelho mantem o hash local`() =
        runTest {
            val source = newDatabase()
            val target = newDatabase()
            try {
                seed(source)
                val localHasher = PasswordHasherImpl(FixedPepperProvider(PEPPER_TARGET))
                target.userDao().insert(
                    UserEntity(
                        id = USER_ID,
                        name = "Nome local",
                        email = "joao@example.com",
                        passwordHash = localHasher.hash("senha-local"),
                        role = "OWNER",
                        createdAt = CREATED_AT,
                    ),
                )
                val exported = ByteArrayOutputStream()
                newRepository(source, PEPPER_SOURCE).exportTo(exported, PASSPHRASE)

                newRepository(target, PEPPER_TARGET).restoreFrom(exported.toByteArray().inputStream(), PASSPHRASE)

                val restored = target.userDao().findById(USER_ID)
                assertThat(localHasher.verify("senha-local", restored!!.passwordHash)).isTrue()
            } finally {
                source.close()
                target.close()
            }
        }

    @Test
    fun `arquivo que nao e backup falha sem alterar os dados`() =
        runTest {
            val target = newDatabase()
            try {
                seed(target)
                val error =
                    runCatching {
                        newRepository(target, PEPPER_TARGET)
                            .restoreFrom("isto nao e um backup".toByteArray().inputStream(), PASSPHRASE)
                    }.exceptionOrNull()

                assertThat(error).isInstanceOf(BackupFormatException::class.java)
                assertThat(target.backupDao().getAllProjects()).hasSize(1)
                assertThat(target.backupDao().getAllTasks()).hasSize(1)
            } finally {
                target.close()
            }
        }

    @Test
    fun `arquivo truncado falha sem alterar os dados`() =
        runTest {
            val target = newDatabase()
            try {
                seed(target)
                val exported = ByteArrayOutputStream()
                newRepository(target, PEPPER_TARGET).exportTo(exported, PASSPHRASE)
                val truncated = exported.toByteArray().copyOf(exported.size() / 2)

                val error =
                    runCatching {
                        newRepository(target, PEPPER_TARGET).restoreFrom(truncated.inputStream(), PASSPHRASE)
                    }.exceptionOrNull()

                assertThat(error).isInstanceOf(BackupFormatException::class.java)
                assertThat(target.backupDao().getAllProjects()).hasSize(1)
            } finally {
                target.close()
            }
        }

    @Test
    fun `versao desconhecida falha sem alterar os dados`() =
        runTest {
            val target = newDatabase()
            try {
                seed(target)
                val exported = ByteArrayOutputStream()
                newRepository(target, PEPPER_TARGET).exportTo(exported, PASSPHRASE)
                val json = JSONObject(String(exported.toByteArray(), Charsets.UTF_8))
                json.put("formatVersion", 99)
                val future = json.toString().toByteArray(Charsets.UTF_8)

                val error =
                    runCatching {
                        newRepository(target, PEPPER_TARGET).restoreFrom(future.inputStream(), PASSPHRASE)
                    }.exceptionOrNull()

                assertThat(error).isInstanceOf(UnsupportedBackupVersionException::class.java)
                assertThat(target.backupDao().getAllProjects()).hasSize(1)
            } finally {
                target.close()
            }
        }

    @Test
    fun `senha errada falha sem alterar os dados`() =
        runTest {
            val target = newDatabase()
            try {
                seed(target)
                val exported = ByteArrayOutputStream()
                newRepository(target, PEPPER_TARGET).exportTo(exported, PASSPHRASE)

                val error =
                    runCatching {
                        newRepository(target, PEPPER_TARGET)
                            .restoreFrom(exported.toByteArray().inputStream(), "senha-errada")
                    }.exceptionOrNull()

                assertThat(error).isInstanceOf(BackupIntegrityException::class.java)
                assertThat(target.backupDao().getAllProjects()).hasSize(1)
            } finally {
                target.close()
            }
        }

    @Test
    fun `arquivo acima do limite falha sem alterar os dados`() =
        runTest {
            val target = newDatabase()
            try {
                seed(target)

                val error =
                    runCatching {
                        newRepository(target, PEPPER_TARGET).restoreFrom(EndlessInputStream(), PASSPHRASE)
                    }.exceptionOrNull()

                assertThat(error).isInstanceOf(BackupTooLargeException::class.java)
                assertThat(target.backupDao().getAllProjects()).hasSize(1)
            } finally {
                target.close()
            }
        }

    @Test
    fun `backup com referencia orfa e recusado sem alterar os dados`() =
        runTest {
            val target = newDatabase()
            try {
                seed(target)
                val orphaned =
                    BackupPayload(
                        users = emptyList(),
                        projects =
                            listOf(
                                BackupProject(
                                    id = PROJECT_ID,
                                    ownerId = "conta-inexistente",
                                    name = "Projeto",
                                    description = null,
                                    createdAtEpochMillis = CREATED_AT.toEpochMilli(),
                                    isCompleted = false,
                                ),
                            ),
                        tasks = emptyList(),
                        tags = emptyList(),
                        projectTags = emptyList(),
                        preferences = BackupPreferences(themeMode = "system", sortOrderByUser = emptyMap()),
                    )
                val envelope =
                    BackupFormat.encodeEnvelope(
                        createdAtEpochMillis = 0L,
                        payload = BackupCrypto.encrypt(BackupPayloadCodec.encode(orphaned), PASSPHRASE),
                    )

                val error =
                    runCatching {
                        newRepository(target, PEPPER_TARGET).restoreFrom(envelope.inputStream(), PASSPHRASE)
                    }.exceptionOrNull()

                assertThat(error).isInstanceOf(BackupRestoreException::class.java)
                assertThat(target.backupDao().getAllProjects()).hasSize(1)
                assertThat(target.backupDao().getAllUsers()).hasSize(1)
            } finally {
                target.close()
            }
        }

    @Test
    fun `sessao e invalidada depois da restauracao`() =
        runTest {
            val source = newDatabase()
            val target = newDatabase()
            try {
                seed(source)
                val sessionStore = SessionStore(context.authDataStore)
                sessionStore.saveUserId(USER_ID)
                val exported = ByteArrayOutputStream()
                newRepository(source, PEPPER_SOURCE).exportTo(exported, PASSPHRASE)

                newRepository(target, PEPPER_TARGET).restoreFrom(exported.toByteArray().inputStream(), PASSPHRASE)

                assertThat(sessionStore.currentUserId()).isNull()
            } finally {
                source.close()
                target.close()
            }
        }

    @Test
    fun `tema e ordenacao do backup sao reaplicados`() =
        runTest {
            val source = newDatabase()
            val target = newDatabase()
            try {
                seed(source)
                val dataStore = context.authDataStore
                ThemePreferencesRepositoryImpl(dataStore).set(ThemeMode.DARK)
                ListingPreferencesRepositoryImpl(dataStore).setSortOrder(USER_ID, SortOrder.NameAsc)
                val exported = ByteArrayOutputStream()
                newRepository(source, PEPPER_SOURCE).exportTo(exported, PASSPHRASE)

                newRepository(target, PEPPER_TARGET).restoreFrom(exported.toByteArray().inputStream(), PASSPHRASE)

                assertThat(ThemePreferencesRepositoryImpl(dataStore).observe().first()).isEqualTo(ThemeMode.DARK)
                assertThat(ListingPreferencesRepositoryImpl(dataStore).observe(USER_ID).first().sortOrder)
                    .isEqualTo(SortOrder.NameAsc)
            } finally {
                source.close()
                target.close()
            }
        }

    @Test
    fun `tarefas com prazo geram lembretes e as demais nao`() =
        runTest {
            val source = newDatabase()
            val target = newDatabase()
            try {
                seed(source)
                val exported = ByteArrayOutputStream()
                newRepository(source, PEPPER_SOURCE).exportTo(exported, PASSPHRASE)

                val result =
                    newRepository(
                        target,
                        PEPPER_TARGET,
                    ).restoreFrom(exported.toByteArray().inputStream(), PASSPHRASE)

                assertThat(result.taskIds).containsExactly(TASK_ID)
                assertThat(result.reminders).hasSize(1)
                assertThat(result.reminders.first().taskId).isEqualTo(TASK_ID)
                assertThat(result.reminders.first().dueDate).isEqualTo(DUE_DATE)
            } finally {
                source.close()
                target.close()
            }
        }

    @Test
    fun `currentTaskIds devolve os ids das tarefas gravadas`() =
        runTest {
            val database = newDatabase()
            try {
                seed(database)

                assertThat(newRepository(database, PEPPER_SOURCE).currentTaskIds()).containsExactly(TASK_ID)
            } finally {
                database.close()
            }
        }

    @Test
    fun `exportacao nao inclui hash de senha no arquivo`() =
        runTest {
            val database = newDatabase()
            try {
                seed(database)
                val exported = ByteArrayOutputStream()

                newRepository(database, PEPPER_SOURCE).exportTo(exported, PASSPHRASE)

                val raw = String(exported.toByteArray(), Charsets.UTF_8)
                assertThat(raw).doesNotContain("pbkdf2_sha256")
                assertThat(raw).doesNotContain("joao@example.com")
            } finally {
                database.close()
            }
        }

    // --- helpers ---

    private fun newDatabase(): BrainOutDatabase =
        Room
            .inMemoryDatabaseBuilder(context, BrainOutDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private fun newRepository(
        database: BrainOutDatabase,
        pepper: ByteArray,
    ): BackupRepositoryImpl {
        val dataStore = context.authDataStore
        return BackupRepositoryImpl(
            database = database,
            passwordHasher = passwordHasher(pepper),
            themePreferencesRepository = ThemePreferencesRepositoryImpl(dataStore),
            listingPreferencesRepository = ListingPreferencesRepositoryImpl(dataStore),
            sessionStore = SessionStore(dataStore),
        )
    }

    private fun passwordHasher(pepper: ByteArray): PasswordHasher = PasswordHasherImpl(FixedPepperProvider(pepper))

    private suspend fun seed(database: BrainOutDatabase) {
        val hasher = passwordHasher(PEPPER_SOURCE)
        database.userDao().insert(
            UserEntity(
                id = USER_ID,
                name = "João",
                email = "joao@example.com",
                passwordHash = hasher.hash("senha-original"),
                role = "OWNER",
                createdAt = CREATED_AT,
            ),
        )
        database.projectDao().insert(
            ProjectEntity(
                id = PROJECT_ID,
                ownerId = USER_ID,
                name = "Projeto",
                description = "Descrição",
                createdAt = CREATED_AT,
                isCompleted = false,
            ),
        )
        database.taskDao().insert(
            TaskEntity(
                id = TASK_ID,
                projectId = PROJECT_ID,
                title = "Tarefa",
                priorityCode = 2,
                status = "TODO",
                assigneeId = null,
                dueDate = DUE_DATE,
                createdAt = CREATED_AT,
                completedAt = null,
            ),
        )
        database.tagDao().insert(
            TagEntity(
                id = TAG_ID,
                ownerId = USER_ID,
                name = "Urgente",
                color = "#FF0000",
                createdAt = CREATED_AT,
            ),
        )
        database.projectDao().insertProjectTags(
            listOf(ProjectTagCrossRef(project_id = PROJECT_ID, tag_id = TAG_ID)),
        )
    }

    /**
     * Fluxo que nunca termina — força o teste do limite de tamanho
     * sem alocar dezenas de megabytes.
     */
    private class EndlessInputStream : InputStream() {
        override fun read(): Int = 'x'.code

        override fun read(
            buffer: ByteArray,
            offset: Int,
            length: Int,
        ): Int {
            for (index in offset until offset + length) {
                buffer[index] = 'x'.code.toByte()
            }
            return length
        }
    }

    private companion object {
        const val USER_ID: String = "user-1"
        const val PROJECT_ID: String = "project-1"
        const val TASK_ID: String = "task-1"
        const val TAG_ID: String = "tag-1"
        const val PASSPHRASE: String = "senha-do-backup"

        val CREATED_AT: Instant = Instant.parse("2026-10-01T10:00:00Z")
        val DUE_DATE: Instant = Instant.parse("2026-10-25T18:00:00Z")

        val PEPPER_SOURCE: ByteArray = ByteArray(16) { 1 }
        val PEPPER_TARGET: ByteArray = ByteArray(16) { 2 }
    }
}
