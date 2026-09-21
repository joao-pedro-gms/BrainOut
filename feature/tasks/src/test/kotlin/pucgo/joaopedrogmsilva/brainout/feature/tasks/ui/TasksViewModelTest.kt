// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Testes do [TasksViewModel] — Task 9 do plano E2.6.
//
// Cobre o contrato mínimo:
// - Estado vazio e `isLoading = false` quando não há owner ativo.
// - Estado carrega lista de tarefas quando owner existe.

package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.data.session.ActiveUserProvider
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository

/**
 * Verifica o contrato do [TasksViewModel]:
 * - Sem owner ativo (`observeActiveUserId` emite `null`) → `rows = []`,
 *   `isLoading = false`. A UI renderiza o estado vazio sem spinner.
 * - Com owner ativo → o ViewModel observa os repositórios e a lista
 *   flui pelo `StateFlow`.
 *
 * Os testes usam [app.cash.turbine.test] para forçar a assinatura do
 * `StateFlow` — `WhileSubscribed(5_000)` só inicia a coleta do
 * upstream quando há pelo menos um subscriber, e `uiState.value` não
 * conta como subscriber.
 *
 * O [Dispatchers.setMain] é necessário porque [TasksViewModel.uiState]
 * é construído em `viewModelScope`, que usa `Dispatchers.Main.immediate`
 * como dispatcher default. Sem isso o Android mock retorna `null` em
 * `Looper.getMainLooper()` e o `runTest` falha ao inicializar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `estado vazio quando nao ha owner ativo`() = runTest(testDispatcher) {
        val activeUserProvider: ActiveUserProvider = mockk()
        coEvery { activeUserProvider.observeActiveUserId() } returns flowOf(null)

        val viewModel = TasksViewModel(
            taskRepository = mockk(relaxed = true),
            projectRepository = mockk(relaxed = true),
            activeUserProvider = activeUserProvider,
        )

        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertThat(state.rows).isEmpty()
            assertThat(state.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `estado carrega lista de tarefas quando owner existe`() = runTest(testDispatcher) {
        val activeUserProvider: ActiveUserProvider = mockk()
        coEvery { activeUserProvider.observeActiveUserId() } returns flowOf("u1")

        val taskRepository: TaskRepository = mockk()
        coEvery { taskRepository.observeAllForOwner("u1") } returns flowOf(emptyList())

        val projectRepository: ProjectRepository = mockk()
        coEvery { projectRepository.observeAllForOwner("u1") } returns flowOf(emptyList())

        val viewModel = TasksViewModel(
            taskRepository = taskRepository,
            projectRepository = projectRepository,
            activeUserProvider = activeUserProvider,
        )

        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertThat(state.rows).isEmpty()
            assertThat(state.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // === E2.8 — estados de erro e retry ==========================================

    /**
     * Primeira emissão lenta: `uiState.isLoading = true` enquanto
     * o Flow não emite. Após a primeira emissão, `isLoading` cai
     * para `false` com a lista carregada.
     */
    @Test
    fun `E2 8 uiState permanece em loading ate Flow de tasks emitir`() = runTest(testDispatcher) {
        val activeUserProvider: ActiveUserProvider = mockk()
        coEvery { activeUserProvider.observeActiveUserId() } returns flowOf("u1")

        val tasksFlow = MutableStateFlow<List<pucgo.joaopedrogmsilva.brainout.core.domain.model.Task>>(emptyList())
        val taskRepository: TaskRepository = mockk()
        coEvery { taskRepository.observeAllForOwner("u1") } returns tasksFlow

        val projectRepository: ProjectRepository = mockk()
        coEvery { projectRepository.observeAllForOwner("u1") } returns flowOf(emptyList())

        val viewModel = TasksViewModel(
            taskRepository = taskRepository,
            projectRepository = projectRepository,
            activeUserProvider = activeUserProvider,
        )

        viewModel.uiState.test {
            var state = awaitItem()
            assertThat(state.isLoading).isTrue()
            assertThat(state.rows).isEmpty()

            // Emite a primeira lista.
            tasksFlow.value = listOf(
                pucgo.joaopedrogmsilva.brainout.core.domain.model.Task.create(
                    projectId = "p1",
                    title = "T",
                ),
            )
            advanceUntilIdle()
            state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.rows).hasSize(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Falha do Room em `observeAllForOwner` é convertida em
     * `ERROR_LOAD_FAILED` + `isLoading = false`.
     */
    @Test
    fun `E2 8 erro do Flow de tasks popula errorMessage de carga`() = runTest(testDispatcher) {
        val activeUserProvider: ActiveUserProvider = mockk()
        coEvery { activeUserProvider.observeActiveUserId() } returns flowOf("u1")

        val taskRepository: TaskRepository = mockk()
        coEvery { taskRepository.observeAllForOwner("u1") } returns flow {
            throw IllegalStateException("disk full")
        }

        val projectRepository: ProjectRepository = mockk()
        coEvery { projectRepository.observeAllForOwner("u1") } returns flowOf(emptyList())

        val viewModel = TasksViewModel(
            taskRepository = taskRepository,
            projectRepository = projectRepository,
            activeUserProvider = activeUserProvider,
        )

        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertThat(state.errorMessage).isEqualTo(TasksViewModel.ERROR_LOAD_FAILED)
            assertThat(state.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(viewModel.errorMessage.value).isEqualTo(TasksViewModel.ERROR_LOAD_FAILED)
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
    fun `E2 8 retry re-assina Flow apos erro e limpa errorMessage`() = runTest(testDispatcher) {
        val activeUserProvider: ActiveUserProvider = mockk()
        coEvery { activeUserProvider.observeActiveUserId() } returns flowOf("u1")

        val tasksFlow = MutableStateFlow<List<pucgo.joaopedrogmsilva.brainout.core.domain.model.Task>>(emptyList())
        val taskRepository: TaskRepository = mockk()
        coEvery { taskRepository.observeAllForOwner("u1") } returns flow {
            throw IllegalStateException("boom")
        }

        val projectRepository: ProjectRepository = mockk()
        coEvery { projectRepository.observeAllForOwner("u1") } returns flowOf(emptyList())

        val viewModel = TasksViewModel(
            taskRepository = taskRepository,
            projectRepository = projectRepository,
            activeUserProvider = activeUserProvider,
        )

        viewModel.uiState.test {
            advanceUntilIdle()
            assertThat(viewModel.errorMessage.value).isEqualTo(TasksViewModel.ERROR_LOAD_FAILED)

            // Reconfigura o mock para emitir lista vazia.
            coEvery { taskRepository.observeAllForOwner("u1") } returns tasksFlow

            viewModel.retry()
            advanceUntilIdle()

            assertThat(viewModel.errorMessage.value).isNull()
            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.errorMessage).isNull()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `E2 8 clearError zera errorMessage`() = runTest(testDispatcher) {
        val activeUserProvider: ActiveUserProvider = mockk()
        coEvery { activeUserProvider.observeActiveUserId() } returns flowOf("u1")

        val taskRepository: TaskRepository = mockk()
        coEvery { taskRepository.observeAllForOwner("u1") } returns flow {
            throw IllegalStateException("boom")
        }
        val projectRepository: ProjectRepository = mockk()
        coEvery { projectRepository.observeAllForOwner("u1") } returns flowOf(emptyList())

        val viewModel = TasksViewModel(
            taskRepository = taskRepository,
            projectRepository = projectRepository,
            activeUserProvider = activeUserProvider,
        )

        viewModel.uiState.test {
            advanceUntilIdle()
            assertThat(viewModel.errorMessage.value).isEqualTo(TasksViewModel.ERROR_LOAD_FAILED)

            viewModel.clearError()
            assertThat(viewModel.errorMessage.value).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
