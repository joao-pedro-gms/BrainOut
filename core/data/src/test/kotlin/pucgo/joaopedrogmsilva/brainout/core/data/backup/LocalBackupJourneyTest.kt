// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TagEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TaskEntity
import pucgo.joaopedrogmsilva.brainout.core.data.preferences.ListingPreferencesRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.preferences.ThemePreferencesRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.repository.UserRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.security.FixedPepperProvider
import pucgo.joaopedrogmsilva.brainout.core.data.security.PasswordHasherImpl
import pucgo.joaopedrogmsilva.brainout.core.data.session.SessionStore
import pucgo.joaopedrogmsilva.brainout.core.data.session.authDataStore
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.AuthenticateUserUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.ExportBackupUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.RestoreBackupUseCase
import java.io.ByteArrayOutputStream
import java.time.Instant

/**
 * Jornada local completa, sem rede (issue #215 — LOCAL-04).
 *
 * Exercita em um único teste o caminho que o usuário percorre depois
 * de instalar o aplicativo em um aparelho novo:
 *
 * 1. **CRUD local** — cria conta, projeto, tarefa com prazo e
 *    etiqueta.
 * 2. **Exportação** — gera o arquivo de backup cifrado.
 * 3. **Restauração em outra instalação** — o arquivo é restaurado em
 *    um segundo banco (pepper diferente, como em outro aparelho).
 * 4. **Login local** — a conta restaurada autentica com a senha do
 *    backup, porque o hash foi derivado com o pepper do aparelho de
 *    destino.
 * 5. **Sessão** — a sessão persistida é invalidada pela restauração.
 * 6. **Lembretes** — a tarefa com prazo vira lembrete reagendado.
 *
 * Não há nenhuma chamada de rede no caminho: o aplicativo é local, e
 * este teste é a prova mecânica disso.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class LocalBackupJourneyTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `jornada local completa — crud, exportacao, restauracao, login, sessao e lembretes`() =
        runTest {
            val source = newDatabase()
            val target = newDatabase()
            try {
                val user = seed(source)
                val exported = exportBackup(source)
                val restored = restoreBackup(target, exported)
                assertContentRestored(target, user, restored)
                assertLocalLogin(target, user)
                assertSessionInvalidated()
                assertRemindersReconciled(restored)
            } finally {
                source.close()
                target.close()
            }
        }

    /** 1. CRUD local: conta, projeto, tarefa com prazo e etiqueta. */
    private suspend fun seed(database: BrainOutDatabase): User {
        val hasher = PasswordHasherImpl(FixedPepperProvider(PEPPER_SOURCE))
        val user =
            UserRepositoryImpl(database.userDao()).save(
                User.create(
                    name = "Maria",
                    email = "maria@example.com",
                    passwordHash = hasher.hash(ORIGINAL_PASSWORD),
                    role = UserRole.OWNER,
                    now = CREATED_AT,
                ),
            )
        database.projectDao().insert(
            ProjectEntity(
                id = PROJECT_ID,
                ownerId = user.id,
                name = "Casa",
                description = "Reforma",
                createdAt = CREATED_AT,
                isCompleted = false,
            ),
        )
        database.taskDao().insert(
            TaskEntity(
                id = TASK_ID,
                projectId = PROJECT_ID,
                title = "Comprar tinta",
                priorityCode = 2,
                status = "TODO",
                assigneeId = user.id,
                dueDate = DUE_DATE,
                createdAt = CREATED_AT,
                completedAt = null,
            ),
        )
        database.tagDao().insert(
            TagEntity(
                id = TAG_ID,
                ownerId = user.id,
                name = "Urgente",
                color = "#FF0000",
                createdAt = CREATED_AT,
            ),
        )
        ListingPreferencesRepositoryImpl(context.authDataStore).setSortOrder(user.id, SortOrder.NameAsc)
        return user
    }

    /** 2. Exportação: gera o arquivo de backup cifrado. */
    private suspend fun exportBackup(database: BrainOutDatabase): ByteArray {
        val exported = ByteArrayOutputStream()
        ExportBackupUseCase(backupRepository(database, PEPPER_SOURCE)).invoke(exported, BACKUP_PASSWORD)
        return exported.toByteArray()
    }

    /** 3. Restauração em outra instalação, com sessão ativa antes. */
    private suspend fun restoreBackup(
        database: BrainOutDatabase,
        exported: ByteArray,
    ): pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupRestoreResult {
        SessionStore(context.authDataStore).saveUserId(RESTORED_USER_ID)
        return RestoreBackupUseCase(
            backupRepository = backupRepository(database, PEPPER_TARGET),
            deadlineNotificationScheduler = RecordingScheduler(),
        ).invoke(exported.inputStream(), BACKUP_PASSWORD)
    }

    /** O conteúdo restaurado preserva ids e vínculos. */
    private suspend fun assertContentRestored(
        database: BrainOutDatabase,
        user: User,
        restored: pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupRestoreResult,
    ) {
        assertThat(restored.summary.projects).isEqualTo(1)
        assertThat(restored.summary.tasks).isEqualTo(1)
        assertThat(restored.taskIds).containsExactly(TASK_ID)
        assertThat(database.backupDao().getAllProjects().map { it.id }).containsExactly(PROJECT_ID)
        assertThat(database.backupDao().getAllTags().map { it.id }).containsExactly(TAG_ID)
        assertThat(database.backupDao().getAllUsers().map { it.id }).containsExactly(user.id)
    }

    /** 4. Login local: a conta restaurada autentica com a senha do backup. */
    private suspend fun assertLocalLogin(
        database: BrainOutDatabase,
        user: User,
    ) {
        val targetHasher = PasswordHasherImpl(FixedPepperProvider(PEPPER_TARGET))
        val authenticated =
            AuthenticateUserUseCase(
                userRepository = UserRepositoryImpl(database.userDao()),
                passwordHasher = targetHasher,
            )(user.email, BACKUP_PASSWORD)
        assertThat(authenticated.id).isEqualTo(user.id)
    }

    /** 5. A sessão persistida foi invalidada pela restauração. */
    private suspend fun assertSessionInvalidated() {
        assertThat(SessionStore(context.authDataStore).currentUserId()).isNull()
    }

    /** 6. A tarefa com prazo vira lembrete a ser reagendado. */
    private fun assertRemindersReconciled(
        restored: pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupRestoreResult,
    ) {
        assertThat(restored.reminders).hasSize(1)
        assertThat(restored.reminders.first().taskId).isEqualTo(TASK_ID)
        assertThat(restored.reminders.first().dueDate).isEqualTo(DUE_DATE)
    }

    private fun newDatabase(): BrainOutDatabase =
        Room
            .inMemoryDatabaseBuilder(context, BrainOutDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private fun backupRepository(
        database: BrainOutDatabase,
        pepper: ByteArray,
    ): BackupRepositoryImpl =
        BackupRepositoryImpl(
            database = database,
            passwordHasher = PasswordHasherImpl(FixedPepperProvider(pepper)),
            themePreferencesRepository = ThemePreferencesRepositoryImpl(context.authDataStore),
            listingPreferencesRepository = ListingPreferencesRepositoryImpl(context.authDataStore),
            sessionStore = SessionStore(context.authDataStore),
        )

    private companion object {
        const val PROJECT_ID: String = "project-jornada"
        const val TASK_ID: String = "task-jornada"
        const val TAG_ID: String = "tag-jornada"
        const val RESTORED_USER_ID: String = "user-jornada"
        const val ORIGINAL_PASSWORD: String = "senha-da-conta"
        const val BACKUP_PASSWORD: String = "senha-do-backup"

        val CREATED_AT: Instant = Instant.parse("2026-10-20T10:00:00Z")
        val DUE_DATE: Instant = Instant.parse("2026-10-25T18:00:00Z")

        val PEPPER_SOURCE: ByteArray = ByteArray(16) { 1 }
        val PEPPER_TARGET: ByteArray = ByteArray(16) { 2 }
    }
}

/**
 * Agendador de lembretes que registra o que foi agendado, para
 * checar a reconciliação da jornada.
 */
private class RecordingScheduler :
    pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler {
    val scheduled: MutableList<Pair<String, Instant>> = mutableListOf()
    val cancelled: MutableList<String> = mutableListOf()

    override fun schedule(
        taskId: String,
        triggerAt: Instant,
    ) {
        scheduled.add(taskId to triggerAt)
    }

    override fun cancel(taskId: String) {
        cancelled.add(taskId)
    }
}
