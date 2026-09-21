// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
@file:Suppress("ConstructorParameterNaming")

package pucgo.joaopedrogmsilva.brainout.core.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TaskEntity

/**
 * Testes transacionais da **RN03 (E2.5)** — conclusão cascata.
 *
 * Roda em Robolectric contra o [BrainOutDatabase] em memória. Cobre o
 * caminho transacional de [ProjectDao.cascadeCompleteTask] e
 * [ProjectDao.cascadeReopenTask]:
 *
 * - Concluir a **última** tarefa ativa do projeto → projeto fica
 *   `is_completed = true` (cascata acionada).
 * - Concluir a **penúltima** tarefa → projeto permanece
 *   `is_completed = false` (cascata NÃO acionada).
 * - Reabrir uma tarefa em projeto concluído → projeto volta a
 *   `is_completed = false` (cascata inversa).
 * - Excluir a última tarefa ativa → projeto é concluído
 *   (RN03 cobre "exclusão da última tarefa ativa também conclui").
 *
 * Esses testes são a ponta do iceberg — a garantia completa está
 * também no `ProjectCompletionTest` em `:core:domain`, que
 * valida os use cases com mocks.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ProjectTaskCascadeTest {

    private lateinit var database: BrainOutDatabase
    private lateinit var projectDao: ProjectDao
    private lateinit var taskDao: TaskDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, BrainOutDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        projectDao = database.projectDao()
        taskDao = database.taskDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun sampleProject(
        id: String = "p-1",
        ownerId: String = "u-1",
        isCompleted: Boolean = false,
    ): ProjectEntity = ProjectEntity(
        id = id,
        ownerId = ownerId,
        name = "Projeto",
        description = null,
        createdAt = Instant.parse("2026-09-21T10:00:00Z"),
        isCompleted = isCompleted,
    )

    private fun sampleTask(
        id: String = "t-1",
        projectId: String = "p-1",
        status: String = "TODO",
        completedAt: Instant? = null,
    ): TaskEntity = TaskEntity(
        id = id,
        projectId = projectId,
        title = "Tarefa $id",
        priorityCode = 1,
        status = status,
        assigneeId = null,
        dueDate = null,
        createdAt = Instant.parse("2026-09-21T10:00:00Z"),
        completedAt = completedAt,
    )

    @Test
    fun `concluir ultima tarefa ativa marca projeto como concluido`() = runTest {
        projectDao.insert(sampleProject())
        taskDao.insert(sampleTask(id = "t-1", status = "DOING"))

        // Simula o caminho "concluir tarefa" do use case: tarefa
        // persistida como DOING → virar DONE; se for a última
        // ativa, projeto também concluído.
        projectDao.cascadeCompleteTask(
            taskDao = taskDao,
            taskId = "t-1",
            newStatus = "DONE",
            completedAt = Instant.parse("2026-09-21T11:00:00Z"),
            projectId = "p-1",
        )

        val updatedTask = taskDao.findById("t-1")
        assertThat(updatedTask?.status).isEqualTo("DONE")
        assertThat(updatedTask?.completedAt).isNotNull()

        val project = projectDao.findById("p-1")
        assertThat(project?.isCompleted).isTrue()
    }

    @Test
    fun `concluir penultima tarefa ativa NAO marca projeto como concluido`() = runTest {
        projectDao.insert(sampleProject())
        taskDao.insert(sampleTask(id = "t-1", status = "DOING"))
        taskDao.insert(sampleTask(id = "t-2", status = "TODO"))

        projectDao.cascadeCompleteTask(
            taskDao = taskDao,
            taskId = "t-1",
            newStatus = "DONE",
            completedAt = Instant.parse("2026-09-21T11:00:00Z"),
            projectId = "p-1",
        )

        val project = projectDao.findById("p-1")
        assertThat(project?.isCompleted).isFalse()
        // Restam t-2 ativa e t-1 done.
        assertThat(taskDao.countActiveByProject("p-1")).isEqualTo(1)
    }

    @Test
    fun `reabrir tarefa em projeto concluido desmarca conclusao`() = runTest {
        // Cenário: projeto já marcado como concluído; tarefa
        // reaberta para DOING desmarca a flag do projeto.
        projectDao.insert(sampleProject(isCompleted = true))
        taskDao.insert(
            sampleTask(
                id = "t-1",
                status = "DONE",
                completedAt = Instant.parse("2026-09-21T11:00:00Z"),
            ),
        )

        projectDao.cascadeReopenTask(
            taskDao = taskDao,
            taskId = "t-1",
            newStatus = "DOING",
            projectId = "p-1",
        )

        val task = taskDao.findById("t-1")
        assertThat(task?.status).isEqualTo("DOING")
        assertThat(task?.completedAt).isNull()
        val project = projectDao.findById("p-1")
        assertThat(project?.isCompleted).isFalse()
    }

    @Test
    fun `excluir ultima tarefa ativa conclui projeto (RN03)`() = runTest {
        projectDao.insert(sampleProject())
        taskDao.insert(sampleTask(id = "t-last", status = "DOING"))

        projectDao.cascadeDeleteTask(
            taskDao = taskDao,
            taskId = "t-last",
            projectId = "p-1",
            wasActive = true,
        )

        assertThat(taskDao.findById("t-last")).isNull()
        val project = projectDao.findById("p-1")
        assertThat(project?.isCompleted).isTrue()
    }

    @Test
    fun `excluir tarefa ja concluida nao dispara cascata`() = runTest {
        // Tarefa DONE não conta como ativa; excluí-la não muda a
        // contagem de tarefas ativas.
        projectDao.insert(sampleProject())
        taskDao.insert(sampleTask(id = "t-done", status = "DONE"))
        taskDao.insert(sampleTask(id = "t-active", status = "TODO"))

        projectDao.cascadeDeleteTask(
            taskDao = taskDao,
            taskId = "t-done",
            projectId = "p-1",
            wasActive = false,
        )

        assertThat(taskDao.findById("t-done")).isNull()
        val project = projectDao.findById("p-1")
        assertThat(project?.isCompleted).isFalse()
    }

    @Test
    fun `observer de tarefas ainda emite o estado consistente pos cascata`() = runTest {
        // Verifica que o Flow observado reage às mudanças em
        // uma única transação: após cascadeCompleteTask, o
        // estado listado pelo Flow deve bater com `is_completed`
        // do projeto.
        projectDao.insert(sampleProject())
        taskDao.insert(sampleTask(id = "t-1", status = "DOING"))

        projectDao.cascadeCompleteTask(
            taskDao = taskDao,
            taskId = "t-1",
            newStatus = "DONE",
            completedAt = Instant.parse("2026-09-21T11:00:00Z"),
            projectId = "p-1",
        )

        val tasksAfter = taskDao.observeForProject("p-1").first()
        assertThat(tasksAfter.map { it.status }).containsExactly("DONE")

        val projectAfter = projectDao.findById("p-1")
        assertThat(projectAfter?.isCompleted).isTrue()
    }
}
