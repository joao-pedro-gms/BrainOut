// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.backup

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.data.local.RoomLocalTransaction
import pucgo.joaopedrogmsilva.brainout.core.data.local.inMemoryDatabase
import pucgo.joaopedrogmsilva.brainout.core.data.repository.ProjectRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.repository.TagRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.repository.TaskRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.repository.UserRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.repository.testSession
import pucgo.joaopedrogmsilva.brainout.core.data.security.FixedPepperProvider
import pucgo.joaopedrogmsilva.brainout.core.data.security.PasswordHasherImpl
import pucgo.joaopedrogmsilva.brainout.core.data.session.AccountAccess
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.AuthenticateUserUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CreateUserUseCase
import java.io.ByteArrayOutputStream
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class LocalBackupJourneyTest {
    @Test
    fun `cadastro crud exportacao e importacao preservam acesso no novo aparelho`() =
        runTest {
            inMemoryDatabase().use { source ->
                inMemoryDatabase().use { target ->
                    val sourceUsers = UserRepositoryImpl(source.userDao())
                    val targetUsers = UserRepositoryImpl(target.userDao())
                    val sourceHasher = PasswordHasherImpl(FixedPepperProvider(ByteArray(16) { 1 }))
                    val targetHasher = PasswordHasherImpl(FixedPepperProvider(ByteArray(16) { 2 }))
                    val sourceUser = CreateUserUseCase(sourceUsers, sourceHasher)("Joao", "fixture-source-input")
                    val targetUser = CreateUserUseCase(targetUsers, targetHasher)("joao", "fixture-target-input")
                    val sourceSession = testSession(sourceUser.id)
                    val targetSession = testSession(targetUser.id)
                    val sourceAccess = AccountAccess(sourceSession, RoomLocalTransaction(source))
                    val tags = TagRepositoryImpl(source.tagDao(), sourceAccess, source.projectDao())
                    val projects = ProjectRepositoryImpl(source.projectDao(), source.tagDao(), sourceAccess)
                    val tasks = TaskRepositoryImpl(source.taskDao(), source.projectDao(), sourceAccess)
                    val tag = tags.create(Tag.create(sourceUser.id, "Urgente", "#123456"))
                    val project = projects.create(Project.create("Projeto", sourceUser.id), listOf(tag.id))
                    tasks.create(Task.create(project.id, "Tarefa", dueDate = Instant.now().plusSeconds(7200)))
                    val prefs = BackupListingPreferences()
                    val output = ByteArrayOutputStream()
                    BackupRepositoryImpl(source, sourceAccess, prefs).exportTo(output, "fixture-backup-input")
                    val targetAccess = AccountAccess(targetSession, RoomLocalTransaction(target))
                    val result =
                        BackupRepositoryImpl(
                            target,
                            targetAccess,
                            prefs,
                        ).restoreFrom(output.toByteArray().inputStream(), "fixture-backup-input")
                    val login = AuthenticateUserUseCase(targetUsers, targetHasher)("joao", "fixture-target-input")
                    assertThat(login.id).isEqualTo(targetUser.id)
                    assertThat(targetSession.currentUserId()).isEqualTo(targetUser.id)
                    assertThat(result.reminders).hasSize(1)
                    val importedProject = target.backupDao().getAllProjects().single()
                    assertThat(importedProject.ownerId).isEqualTo(targetUser.id)
                    assertThat(
                        target
                            .backupDao()
                            .getAllProjectTags()
                            .single()
                            .project_id,
                    ).isEqualTo(importedProject.id)
                    assertThat(target.userDao().findByUsername("Joao")).isNull()
                }
            }
        }
}
