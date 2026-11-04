// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Testes do [ProjectDetailViewModel] — Task 8 do plano E2.1/E2.2.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.error.OwnershipException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.ProjectNotFoundException
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
import java.time.Instant

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
    fun `formulario fecha apenas depois da gravacao e permanece aberto se ela falhar`() =
        runTest {
            val persisted = kotlinx.coroutines.CompletableDeferred<Task>()
            coEvery { createTask.invoke(any(), any(), any(), any(), any()) } coAnswers { persisted.await() }
            val vm = viewModel()
            var closed = false
            vm.addTask("Nova tarefa", onSuccess = { closed = true })
            runCurrent()
            assertThat(closed).isFalse()
            persisted.complete(Task.create(projectId = projectId, title = "Nova tarefa"))
            advanceUntilIdle()
            assertThat(closed).isTrue()
            closed = false
            coEvery { createTask.invoke(any(), any(), any(), any(), any()) } throws java.io.IOException("Falha local")
            vm.addTask("Preservar texto", onSuccess = { closed = true })
            advanceUntilIdle()
            assertThat(closed).isFalse()
            assertThat(vm.errorMessage.value).isNotNull()
        }

    @Test
    fun `addTask dispara CreateTaskUseCase com projectId correto`() =
        runTest {
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
    fun `addTask com titulo vazio nao chama CreateTaskUseCase e seta errorMessage`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()

            vm.addTask("   ")
            advanceUntilIdle()

            coVerify(exactly = 0) { createTask.invoke(any(), any(), any(), any(), any()) }
            assertThat(vm.errorMessage.value).isEqualTo(ProjectDetailError.EmptyTitle)
        }

    @Test
    fun `addTask quando RN01 dispara popula errorMessage com limite`() =
        runTest {
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
                .isEqualTo(ProjectDetailError.TaskLimitReached(MAX_ACTIVE_TASKS_PER_PROJECT))
        }

    @Test
    fun `changeStatus delega ao ChangeTaskStatusUseCase`() =
        runTest {
            val updated = Task.create(projectId = projectId, title = "ok").transitionTo(TaskStatus.DOING)
            coEvery { changeStatus.invoke(any(), any()) } returns updated

            val vm = viewModel()
            advanceUntilIdle()

            vm.changeStatus("t1", TaskStatus.DOING)
            advanceUntilIdle()

            coVerify { changeStatus.invoke("t1", TaskStatus.DOING) }
        }

    @Test
    fun `changeStatus ao reabrir no teto de RN01 expoe a mensagem do limite`() =
        runTest {
            // DEF-20: a rejeição chega do domínio como
            // ProjectTaskLimitReachedException e o ViewModel precisa
            // exibir a MESMA mensagem do caminho de criação — não o
            // fallback genérico (ERROR_LOAD_FAILED), que descreve uma
            // falha de leitura e seria mentira aqui.
            coEvery { changeStatus.invoke("t1", TaskStatus.DOING) } throws
                ProjectTaskLimitReachedException(projectId, MAX_ACTIVE_TASKS_PER_PROJECT)

            val vm = viewModel()
            advanceUntilIdle()

            vm.changeStatus("t1", TaskStatus.DOING)
            advanceUntilIdle()

            assertThat(vm.errorMessage.value)
                .isEqualTo(ProjectDetailError.TaskLimitReached(MAX_ACTIVE_TASKS_PER_PROJECT))
            assertThat(vm.errorMessage.value)
                .isNotEqualTo(ProjectDetailError.LoadFailed)
        }

    @Test
    fun `deleteProject chama use case e onDone`() =
        runTest {
            var doneCalled = false
            val vm = viewModel()
            advanceUntilIdle()

            vm.deleteProject { doneCalled = true }
            advanceUntilIdle()

            coVerify { deleteProject.invoke(projectId) }
            assertThat(doneCalled).isTrue()
        }

    /**
     * DEF-36 — o domínio recusa a exclusão quando o papel do usuário
     * ativo não tem `DELETE_PROJECT`; a tela precisa dizer isso, e não
     * a mensagem genérica de falha.
     */
    @Test
    fun `exclusao recusada por falta de permissao vira erro tipado`() =
        runTest {
            coEvery { deleteProject.invoke(projectId) } throws
                OwnershipException()
            val vm = viewModel()
            advanceUntilIdle()

            vm.deleteProject {}
            advanceUntilIdle()

            assertThat(vm.errorMessage.value).isEqualTo(ProjectDetailError.PermissionDenied)
        }

    @Test
    fun `clearError zera a mensagem de erro`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()

            vm.addTask("   ") // gera errorMessage
            advanceUntilIdle()
            assertThat(vm.errorMessage.value).isNotNull()

            vm.clearError()
            assertThat(vm.errorMessage.value).isNull()
        }

    @Test
    fun `uiState inicial reflete isLoading true e projectId correto`() =
        runTest {
            val vm = viewModel()
            // Sem advanceUntilIdle — primeira leitura do stateIn.
            val initial = vm.uiState.value
            assertThat(initial.projectId).isEqualTo(projectId)
            assertThat(initial.isLoading).isTrue()
            assertThat(initial.tasks).isEmpty()
        }

    @Test
    fun `uiState apos receber tasks do repo reflete lista e isLoading false`() =
        runTest {
            val now = Instant.parse("2026-09-19T00:00:00Z")
            val tasks =
                listOf(
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

    // === E2.8 — estados de erro e retry ==========================================

    /**
     * Simula primeira emissão lenta do Flow de tarefas. Enquanto o
     * Flow não emite, `uiState.isLoading = true` e a lista está
     * vazia. Após a primeira emissão, `isLoading` vai para `false`
     * com a lista carregada.
     */
    @Test
    fun `E2 8 uiState permanece em loading ate observeForProject emitir`() =
        runTest {
            val tasksFlow = MutableStateFlow<List<Task>>(emptyList())
            coEvery { taskRepository.observeForProject(projectId) } returns tasksFlow

            val vm = viewModel()

            vm.uiState.test {
                var state = awaitItem()
                assertThat(state.isLoading).isTrue()
                assertThat(state.tasks).isEmpty()

                // Emite a primeira lista — loader some.
                val now = Instant.parse("2026-09-19T00:00:00Z")
                tasksFlow.value =
                    listOf(
                        Task.create(projectId = projectId, title = "T", now = now),
                    )
                advanceUntilIdle()
                state = expectMostRecentItem()
                assertThat(state.isLoading).isFalse()
                assertThat(state.tasks).hasSize(1)
                cancelAndIgnoreRemainingEvents()
            }
        }

    /**
     * Simula falha do Room no `observeForProject`: o `.catch`
     * converte em `ERROR_LOAD_FAILED` + `isLoading = false`.
     */
    @Test
    fun `E2 8 erro do Flow de tarefas popula errorMessage de carga`() =
        runTest {
            coEvery { taskRepository.observeForProject(projectId) } returns
                flow {
                    throw IllegalStateException("disk full")
                }

            val vm = viewModel()

            vm.uiState.test {
                advanceUntilIdle()
                val state = expectMostRecentItem()
                assertThat(state.errorMessage).isEqualTo(ProjectDetailError.LoadFailed)
                assertThat(state.isLoading).isFalse()
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(vm.errorMessage.value).isEqualTo(ProjectDetailError.LoadFailed)
        }

    /**
     * `retry()` re-assina o Flow após falha. Reconfiguramos o mock
     * para emitir lista vazia (sucesso) e verificamos que
     * `errorMessage` é limpo.
     *
     * E2.8 — usamos `uiState.test` para forçar a inscrição no
     * `stateIn` antes de ler `errorMessage.value`. Sem um
     * assinante, o upstream nem chega a iniciar (e o `catch`
     * nunca dispara).
     */
    @Test
    fun `E2 8 retry re-assina Flow apos erro e limpa errorMessage`() =
        runTest {
            val tasksFlow = MutableStateFlow<List<Task>>(emptyList())
            coEvery { taskRepository.observeForProject(projectId) } returns
                flow {
                    throw IllegalStateException("boom")
                }

            val vm = viewModel()

            vm.uiState.test {
                advanceUntilIdle()
                assertThat(vm.errorMessage.value).isEqualTo(ProjectDetailError.LoadFailed)

                // Reconfigura o mock para emitir lista vazia.
                coEvery { taskRepository.observeForProject(projectId) } returns tasksFlow

                vm.retry()
                advanceUntilIdle()

                assertThat(vm.errorMessage.value).isNull()
                val state = expectMostRecentItem()
                assertThat(state.isLoading).isFalse()
                assertThat(state.errorMessage).isNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    /**
     * Falha genérica em `addTask` (não mapeada por exceção de
     * domínio) deve ser convertida em `errorMessage` via o catch
     * `Throwable` adicionado em E2.8 — sem crash.
     *
     * É uma falha de **gravação**, não de leitura: por isso o tipo é
     * `ActionFailed` e não `LoadFailed` — o primeiro vai para o
     * snackbar, o segundo para o banner com "Tentar novamente".
     */
    @Test
    fun `E2 8 falha generica em addTask popula errorMessage de acao`() =
        runTest {
            coEvery {
                createTask.invoke(any(), any(), any(), any(), any())
            } throws IllegalStateException("write failed")

            val vm = viewModel()
            vm.addTask("qualquer")
            advanceUntilIdle()

            assertThat(vm.errorMessage.value).isEqualTo(ProjectDetailError.ActionFailed)
        }

    /**
     * DEF-56 — o projeto foi excluído com a tela aberta.
     *
     * O domínio barra a criação com `ProjectNotFoundException` antes do
     * `@Insert` (que devolveria uma `SQLiteConstraintException` crua).
     * A tela mostra uma mensagem própria, e não a genérica de falha de
     * gravação.
     */
    @Test
    fun `E2 8 projeto inexistente em addTask popula erro de projeto nao encontrado`() =
        runTest {
            coEvery {
                createTask.invoke(any(), any(), any(), any(), any())
            } throws ProjectNotFoundException(projectId)

            val vm = viewModel()
            vm.addTask("qualquer")
            advanceUntilIdle()

            assertThat(vm.errorMessage.value).isEqualTo(ProjectDetailError.ProjectNotFound)
        }
}
