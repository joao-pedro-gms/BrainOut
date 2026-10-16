// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase
import pucgo.joaopedrogmsilva.brainout.core.data.local.RoomLocalTransaction
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
            val db = newDatabase()
            try {
                val projects = ProjectRepositoryImpl(db.projectDao(), db.tagDao(), RoomLocalTransaction(db))
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
            val db = newDatabase()
            try {
                val tx = RoomLocalTransaction(db)
                val projects = ProjectRepositoryImpl(db.projectDao(), db.tagDao(), tx)
                val tags = TagRepositoryImpl(db.tagDao(), db.projectDao(), tx)
                val tasks = TaskRepositoryImpl(db.taskDao(), db.projectDao(), tx)
                val tag =
                    tags.create(
                        Tag.create(ownerId = "u1", name = "Urgente", color = "#123456", now = Instant.EPOCH),
                    )
                val project = projects.create(Project.create(name = "Projeto", ownerId = "u1"), listOf(tag.id))
                assertThat(projects.observeTagsFor(project.id).first()).containsExactly(tag)
                val task = tasks.create(Task.create(projectId = project.id, title = "Tarefa"))
                tasks.completeAndCascade(task.id)
                assertThat(projects.findById(project.id)?.isCompleted).isTrue()
                tasks.reopenAndCascade(task.id, TaskStatus.DOING)
                assertThat(projects.findById(project.id)?.isCompleted).isFalse()
                tasks.delete(task.id)
                assertThat(projects.findById(project.id)?.isCompleted).isTrue()
                projects.delete(project.id)
                assertThat(projects.findById(project.id)).isNull()
                assertThat(projects.observeTagsFor(project.id).first()).isEmpty()
                assertThat(tags.findById(tag.id)).isNotNull()
            } finally {
                db.close()
            }
        }

    private fun newDatabase(): BrainOutDatabase =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), BrainOutDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}
