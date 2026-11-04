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
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TagNotFoundException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class LocalPersistenceTest {
    @Test
    fun `falha ao associar tag desfaz inclusive o projeto inserido na transacao`() =
        runTest {
            val db = inMemoryDatabase()
            try {
                val projects = ProjectRepositoryImpl(db.projectDao(), db.tagDao(), testAccess(RoomLocalTransaction(db)))
                val project = Project.create(name = "Projeto", ownerId = "u1")
                val error = runCatching { projects.create(project, listOf("inexistente")) }.exceptionOrNull()
                assertThat(error).isInstanceOf(TagNotFoundException::class.java)
                assertThat(projects.findById(project.id)).isNull()
            } finally {
                db.close()
            }
        }

    @Test
    fun `crud local preserva tags e cascatas de conclusao reabertura e exclusao`() =
        runTest {
            val db = inMemoryDatabase()
            try {
                val tx = RoomLocalTransaction(db)
                val projects = ProjectRepositoryImpl(db.projectDao(), db.tagDao(), testAccess(tx))
                val tags = TagRepositoryImpl(db.tagDao(), testAccess(tx), db.projectDao())
                val tasks = TaskRepositoryImpl(db.taskDao(), db.projectDao(), testAccess(tx))
                val tag =
                    tags.create(
                        Tag.create(ownerId = "u1", name = "Urgente", color = "#123456", now = Instant.EPOCH),
                    )
                val project = projects.create(Project.create(name = "Projeto", ownerId = "u1"), listOf(tag.id))
                assertThat(tags.observeByProjectIds(setOf(project.id)).first()[project.id]).containsExactly(tag)
                val task = tasks.create(Task.create(projectId = project.id, title = "Tarefa"))
                tasks.completeAndCascade(task.id)
                assertThat(projects.findById(project.id)?.isCompleted).isTrue()
                tasks.reopenAndCascade(task.id, TaskStatus.DOING)
                assertThat(projects.findById(project.id)?.isCompleted).isFalse()
                tasks.delete(task.id)
                assertThat(projects.findById(project.id)?.isCompleted).isTrue()
                projects.delete(project.id)
                assertThat(projects.findById(project.id)).isNull()
                assertThat(tags.observeByProjectIds(setOf(project.id)).first()[project.id]).isEmpty()
                assertThat(tags.findById(tag.id)).isNotNull()
            } finally {
                db.close()
            }
        }

    @Test
    fun `consulta de tags por projetos com conjunto vazio emite mapa vazio`() =
        runTest {
            // Contrato de `TagRepository.observeByProjectIds`: conjunto
            // vazio devolve mapa vazio — sem "bail-out" que congelaria a
            // listagem da Home para quem nao tem projetos. Exercitado
            // contra o Room de verdade, porque a consulta usa `IN (:ids)`.
            val db = inMemoryDatabase()
            try {
                val tags = TagRepositoryImpl(db.tagDao(), testAccess(RoomLocalTransaction(db)), db.projectDao())

                assertThat(tags.observeByProjectIds(emptySet()).first()).isEmpty()
            } finally {
                db.close()
            }
        }
}
