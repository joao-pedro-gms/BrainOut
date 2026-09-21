// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Testes do [ProjectDetailViewModel] — Task 8 do plano E2.1/E2.2.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.error.ProjectTaskLimitReachedException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.ChangeTaskStatusUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CheckDeadlineUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CreateTaskUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.DeleteProjectUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.DeleteTaskUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.MAX_ACTIVE_TASKS_PER_PROJECT
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.UpdateTaskUseCase

/**
 * Cobre o contrato do [ProjectDetailViewModel]:
 * - `addTask` delega ao [CreateTaskUseCase] com o `projectId` extraído
 *   do `SavedStateHandle`.
 * - RN01 (limite de tarefas ativas) é convertido em `errorMessage`
 *   no `StateFlow`, sem explodir exceção.
 * - `changeStatus` delega ao [ChangeTaskStatusUseCase].
 * - `deleteProject` chama o [DeleteProjectUseCase] seguido do callback
 *   `onDone`.
 * - `clearError` zera a mensagem de erro.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProjectDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val projectId: String = "p1"

    private lateinit var taskRepository: TaskRepository
    private lateinit var createTask: CreateTaskUseCase
    private lateinit var updateTask: UpdateTaskUseCase
    private lateinit var changeStatus: ChangeTaskStatusUseCase
    private lateinit var deleteProject: DeleteProjectUseCase
    private lateinit var deleteTask: DeleteTaskUseCase
    private lateinit var checkDeadline: CheckDeadlineUseCase

    private fun viewModel(): ProjectDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf(ProjectDetailViewModel.PROJECT_ID_ARG to projectId))
        return ProjectDetailViewModel(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepository,
            createTask = createTask,
            updateTask = updateTask,
            changeStatus = changeStatus,
            deleteProject = deleteProject,
            deleteTask = deleteTask,
            checkDeadline = checkDeadline,
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        taskRepository = mockk(relaxed = true)
        coEvery { taskRepository.observeForProject(projectId) } returns flowOf(emptyList())

        createTask = mockk()
        updateTask = mockk()
        changeStatus = mockk()
        deleteProject = mockk(relaxed = true)
        deleteTask = mockk(relaxed = true)
        checkDeadline = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `addTask dispara CreateTaskUseCase com projectId correto`() = runTest {
        coEvery {
            createTask.invoke(
                projectId = any(),
                title = any(),
                priority = any(),
                dueDate = any(),
                assigneeId = any(),
            )
        } returns Task.create(projectId = projectId, title = "ok")

        val vm = viewModel()
        advanceUntilIdle()

        vm.addTask("nova tarefa")
        advanceUntilIdle()

        coVerify {
            createTask.invoke(
                projectId = projectId,
                title = "nova tarefa",
                priority = any(),
            )
        }
    }

    @Test
    fun `addTask com titulo vazio nao chama CreateTaskUseCase e seta errorMessage`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.addTask("   ")
        advanceUntilIdle()

        coVerify(exactly = 0) { createTask.invoke(any(), any(), any(), any(), any()) }
        assertThat(vm.errorMessage.value).isEqualTo("Título da tarefa não pode ser vazio")
    }

    @Test
    fun `addTask quando RN01 dispara popula errorMessage com limite`() = runTest {
        coEvery {
            createTask.invoke(
                projectId = any(),
                title = any(),
                priority = any(),
                dueDate = any(),
                assigneeId = any(),
            )
        } throws ProjectTaskLimitReachedException(projectId, MAX_ACTIVE_TASKS_PER_PROJECT)

        val vm = viewModel()
        advanceUntilIdle()

        vm.addTask("qualquer")
        advanceUntilIdle()

        assertThat(vm.errorMessage.value)
            .isEqualTo("Projeto $projectId já atingiu o limite de $MAX_ACTIVE_TASKS_PER_PROJECT tarefas ativas")
    }

    @Test
    fun `changeStatus delega ao ChangeTaskStatusUseCase`() = runTest {
        val updated = Task.create(projectId = projectId, title = "ok").transitionTo(TaskStatus.DOING)
        coEvery { changeStatus.invoke(any(), any()) } returns updated

        val vm = viewModel()
        advanceUntilIdle()

        vm.changeStatus("t1", TaskStatus.DOING)
        advanceUntilIdle()

        coVerify { changeStatus.invoke("t1", TaskStatus.DOING) }
    }

    @Test
    fun `deleteProject chama use case e onDone`() = runTest {
        var doneCalled = false
        val vm = viewModel()
        advanceUntilIdle()

        vm.deleteProject { doneCalled = true }
        advanceUntilIdle()

        coVerify { deleteProject.invoke(projectId) }
        assertThat(doneCalled).isTrue()
    }

    @Test
    fun `clearError zera a mensagem de erro`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.addTask("   ") // gera errorMessage
        advanceUntilIdle()
        assertThat(vm.errorMessage.value).isNotNull()

        vm.clearError()
        assertThat(vm.errorMessage.value).isNull()
    }

    @Test
    fun `uiState inicial reflete isLoading true e projectId correto`() = runTest {
        val vm = viewModel()
        // Sem advanceUntilIdle — primeira leitura do stateIn.
        val initial = vm.uiState.value
        assertThat(initial.projectId).isEqualTo(projectId)
        assertThat(initial.isLoading).isTrue()
        assertThat(initial.tasks).isEmpty()
    }

    @Test
    fun `uiState apos receber tasks do repo reflete lista e isLoading false`() = runTest {
        val now = Instant.parse("2026-09-19T00:00:00Z")
        val tasks = listOf(
            Task(
                id = "t1",
                projectId = projectId,
                title = "A",
                priority = TaskPriority.MEDIUM,
                status = TaskStatus.TODO,
                assigneeId = null,
                dueDate = null,
                createdAt = now,
            ),
        )
        coEvery { taskRepository.observeForProject(projectId) } returns flowOf(tasks)

        val vm = viewModel()

        // WhileSubscribed só inicia a coleta do upstream quando há
        // pelo menos um assinante; por isso usamos Turbine para
        // forçar a assinatura e consumir a emissão do Flow.
        vm.uiState.test {
            advanceUntilIdle()
            val emitted = expectMostRecentItem()
            assertThat(emitted.tasks).isEqualTo(tasks)
            assertThat(emitted.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
