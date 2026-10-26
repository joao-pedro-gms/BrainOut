// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.data.local.RoomLocalTransaction
import pucgo.joaopedrogmsilva.brainout.core.data.local.inMemoryDatabase
import pucgo.joaopedrogmsilva.brainout.core.data.session.AccountAccess
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class AccountIsolationTest {
    @Test
    fun `conta diferente nao le detalhes nem altera dados por ids conhecidos`() =
        runTest {
            val db = inMemoryDatabase()
            try {
                val session = testSession("u1")
                val access = AccountAccess(session, RoomLocalTransaction(db))
                val projects = ProjectRepositoryImpl(db.projectDao(), db.tagDao(), access)
                val tasks = TaskRepositoryImpl(db.taskDao(), db.projectDao(), access)
                val tags = TagRepositoryImpl(db.tagDao(), access, db.projectDao())
                val tag = tags.create(Tag.create("u1", "Privada", "#123456"))
                val project = projects.create(Project.create("Privado", "u1"), listOf(tag.id))
                val task = tasks.create(Task.create(project.id, "Privada"))
                session.saveUserId("u2")
                assertThat(projects.findById(project.id)).isNull()
                assertThat(tasks.findById(task.id)).isNull()
                assertThat(tags.findById(tag.id)).isNull()
                assertThat(tasks.observeForProject(project.id).first()).isEmpty()
                assertThat(projects.observeAllForOwner("u1").first()).isEmpty()
                assertThat(tags.observeByProjectIds(setOf(project.id)).first()[project.id]).isEmpty()
                assertThat(
                    runCatching { projects.update(project.copy(ownerId = "u2"), emptyList()) }.isFailure,
                ).isTrue()
                assertThat(runCatching { projects.delete(project.id) }.isFailure).isTrue()
                assertThat(runCatching { tasks.update(task) }.isFailure).isTrue()
                assertThat(runCatching { tasks.completeAndCascade(task.id) }.isFailure).isTrue()
                assertThat(runCatching { tags.delete(tag.id) }.isFailure).isTrue()
                assertThat(runCatching { tasks.create(Task.create(project.id, "Invasora")) }.isFailure).isTrue()
                assertThat(db.taskDao().findById(task.id)?.status).isEqualTo("TODO")
            } finally {
                db.close()
            }
        }

    @Test
    fun `sair preserva permissao mas impede acoes antigas e novas atribuicoes externas`() =
        runTest {
            val db = inMemoryDatabase()
            try {
                val session = testSession("u1")
                val access = AccountAccess(session, RoomLocalTransaction(db))
                val projects = ProjectRepositoryImpl(db.projectDao(), db.tagDao(), access)
                val tasks = TaskRepositoryImpl(db.taskDao(), db.projectDao(), access)
                val project = projects.create(Project.create("Pessoal", "u1"), emptyList())
                assertThat(
                    runCatching { tasks.create(Task.create(project.id, "Externa", assigneeId = "u2")) }.isFailure,
                ).isTrue()
                val task = tasks.create(Task.create(project.id, "Pessoal", assigneeId = "u1"))
                session.markNotificationPermissionAsked()
                session.clearSession()
                assertThat(session.wasNotificationPermissionAsked()).isTrue()
                assertThat(tasks.findById(task.id)).isNull()
                assertThat(runCatching { tasks.changeStatus(task.id, TaskStatus.DOING) }.isFailure).isTrue()
                session.saveUserId("u1")
                assertThat(tasks.findById(task.id)?.id).isEqualTo(task.id)
            } finally {
                db.close()
            }
        }
}
