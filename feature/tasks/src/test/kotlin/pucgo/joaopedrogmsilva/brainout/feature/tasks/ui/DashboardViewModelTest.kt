// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Testes do [DashboardViewModel] — E2.7 do ROADMAP.
//
// Cobre:
// - Estado inicial de loading e valores zerados sem owner ativo.
// - Contagem de projetos por estado (ativos/concluídos).
// - Histograma de prioridades normalizado para 5 níveis (0..4),
//   com níveis ausentes recebendo zero.
// - Taxas de conclusão semanal e global (incl. divisão por zero).
// - Reatividade: mudanças nos Flows dos repositórios re-emitem o
//   estado sem retry (invalidação do Room).
// - E2.8: falha do Room popula `errorMessage`; `retry()` re-assina e
//   limpa o erro; `clearError()` zera a mensagem.

package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
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
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskCompletionStats
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskPriorityCount
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository

/**
 * Verifica o contrato do [DashboardViewModel] (E2.7 + E2.8).
 *
 * Os testes usam [app.cash.turbine.test] para forçar a assinatura do
 * `StateFlow` — `WhileSubscribed(5_000)` só inicia a coleta do
 * upstream quando há pelo menos um subscriber. O `Dispatchers.setMain`
 * é necessário porque o pipeline roda em `viewModelScope`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // === Helpers ================================================================

    private fun project(id: String, completed: Boolean): Project = Project(
        id = id,
        name = "Projeto $id",
        description = null,
        ownerId = "u1",
        createdAt = Instant.parse("2026-09-01T00:00:00Z"),
        isCompleted = completed,
    )

    private class Fakes(
        val projectsFlow: MutableStateFlow<List<Project>> =
            MutableStateFlow(emptyList()),
        val priorityFlow: MutableStateFlow<List<TaskPriorityCount>> =
            MutableStateFlow(emptyList()),
        val statsFlow: MutableStateFlow<TaskCompletionStats> = MutableStateFlow(
            TaskCompletionStats(totalCount = 0, doneCount = 0, doneThisWeekCount = 0),
        ),
    ) {
        fun viewModel(activeUserProvider: ActiveUserProvider): DashboardViewModel {
            val projectRepository: ProjectRepository = mockk()
            every { projectRepository.observeAllForOwner("u1") } returns projectsFlow
            val taskRepository: TaskRepository = mockk()
            every { taskRepository.observeCountByPriority("u1") } returns priorityFlow
            every {
                taskRepository.observeCompletionStats("u1", any<Long>())
            } returns statsFlow
            return DashboardViewModel(
                projectRepository = projectRepository,
                taskRepository = taskRepository,
                activeUserProvider = activeUserProvider,
            )
        }
    }

    private fun activeUserProvider(userId: String?): ActiveUserProvider = mockk {
        every { observeActiveUserId() } returns flowOf(userId)
    }

    // === E2.7 — valores iniciais e contagens ====================================

    @Test
    fun `estado vazio e loading falso quando nao ha owner ativo`() = runTest(testDispatcher) {
        val viewModel = Fakes().viewModel(activeUserProvider(null))

        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.activeProjects).isEqualTo(0)
            assertThat(state.completedProjects).isEqualTo(0)
            assertThat(state.priorityCounts).containsExactly(0, 0, 0, 0, 0).inOrder()
            assertThat(state.totalTasks).isEqualTo(0)
            assertThat(state.weeklyCompletionPercent).isEqualTo(0)
            assertThat(state.errorMessage).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `E2 7 uiState inicial fica em loading ate primeira emissao combinada`() =
        runTest(testDispatcher) {
            val viewModel = Fakes().viewModel(activeUserProvider("u1"))

            viewModel.uiState.test {
                val initial = awaitItem()
                assertThat(initial.isLoading).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `E2 7 contagem de projetos por estado ativos e concluidos`() = runTest(testDispatcher) {
        val fakes = Fakes()
        val viewModel = fakes.viewModel(activeUserProvider("u1"))

        viewModel.uiState.test {
            advanceUntilIdle()
            fakes.projectsFlow.value = listOf(
                project("p1", completed = false),
                project("p2", completed = false),
                project("p3", completed = true),
            )
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertThat(state.activeProjects).isEqualTo(2)
            assertThat(state.completedProjects).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `E2 7 grafico de prioridade normaliza 5 niveis e preenche zeros`() =
        runTest(testDispatcher) {
            val fakes = Fakes()
            val viewModel = fakes.viewModel(activeUserProvider("u1"))

            viewModel.uiState.test {
                advanceUntilIdle()
                // DAO só reporta níveis presentes: 2 em MEDIUM(1), 1 em URGENT(3).
                fakes.priorityFlow.value = listOf(
                    TaskPriorityCount(priorityCode = 1, count = 2),
                    TaskPriorityCount(priorityCode = 3, count = 1),
                )
                advanceUntilIdle()
                val state = expectMostRecentItem()
                assertThat(state.priorityCounts)
                    .containsExactly(0, 2, 0, 1, 0)
                    .inOrder()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `E2 7 taxa semanal e global refletindo stats do repositorio`() = runTest(testDispatcher) {
        val fakes = Fakes()
        val viewModel = fakes.viewModel(activeUserProvider("u1"))

        viewModel.uiState.test {
            advanceUntilIdle()
            fakes.statsFlow.value = TaskCompletionStats(
                totalCount = 10,
                doneCount = 4,
                doneThisWeekCount = 2,
            )
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertThat(state.totalTasks).isEqualTo(10)
            assertThat(state.doneTasks).isEqualTo(4)
            assertThat(state.overallCompletionPercent).isEqualTo(40)
            assertThat(state.weeklyCompletionPercent).isEqualTo(20)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `E2 7 reatividade mudanca no banco re-emite estado sem retry`() = runTest(testDispatcher) {
        val fakes = Fakes()
        val viewModel = fakes.viewModel(activeUserProvider("u1"))

        viewModel.uiState.test {
            advanceUntilIdle()
            // Simula invalidação do Room: nova escrita no banco emite
            // novo valor no Flow dos DAOs e o estado acompanha.
            fakes.statsFlow.value = TaskCompletionStats(
                totalCount = 5,
                doneCount = 5,
                doneThisWeekCount = 5,
            )
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertThat(state.overallCompletionPercent).isEqualTo(100)
            assertThat(state.weeklyCompletionPercent).isEqualTo(100)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // === E2.8 — estados de erro e retry =========================================

    @Test
    fun `E2 8 erro do Room popula errorMessage de carga`() = runTest(testDispatcher) {
        val activeUserProvider: ActiveUserProvider = mockk {
            every { observeActiveUserId() } returns flowOf("u1")
        }
        val projectRepository: ProjectRepository = mockk()
        every { projectRepository.observeAllForOwner("u1") } returns flow {
            throw IllegalStateException("disk full")
        }

        val viewModel = DashboardViewModel(
            projectRepository = projectRepository,
            taskRepository = mockk(relaxed = true),
            activeUserProvider = activeUserProvider,
        )

        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertThat(state.errorMessage).isEqualTo(DashboardViewModel.ERROR_LOAD_FAILED)
            assertThat(state.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(viewModel.errorMessage.value).isEqualTo(DashboardViewModel.ERROR_LOAD_FAILED)
    }

    @Test
    fun `E2 8 retry re-assina Flow apos erro e limpa errorMessage`() = runTest(testDispatcher) {
        val activeUserProvider: ActiveUserProvider = mockk {
            every { observeActiveUserId() } returns flowOf("u1")
        }
        val projectRepository: ProjectRepository = mockk()
        every { projectRepository.observeAllForOwner("u1") } returns flow {
            throw IllegalStateException("boom")
        }
        // Fluxos de sucesso para a nova assinatura do `retry()` —
        // stub completo: um mock `relaxed` lançaria em métodos que
        // retornam Flow (mockk não tem default para Flow), re-entrando
        // no caminho de erro.
        val taskRepository: TaskRepository = mockk()
        every { taskRepository.observeCountByPriority("u1") } returns flowOf(emptyList())
        every {
            taskRepository.observeCompletionStats("u1", any<Long>())
        } returns flowOf(
            TaskCompletionStats(totalCount = 0, doneCount = 0, doneThisWeekCount = 0),
        )

        val viewModel = DashboardViewModel(
            projectRepository = projectRepository,
            taskRepository = taskRepository,
            activeUserProvider = activeUserProvider,
        )

        viewModel.uiState.test {
            advanceUntilIdle()
            assertThat(viewModel.errorMessage.value).isEqualTo(DashboardViewModel.ERROR_LOAD_FAILED)

            // Reconfigura o mock para sucesso na nova assinatura.
            every { projectRepository.observeAllForOwner("u1") } returns flowOf(emptyList())
            viewModel.retry()
            advanceUntilIdle()

            assertThat(viewModel.errorMessage.value).isNull()
            val state = expectMostRecentItem()
            assertThat(state.errorMessage).isNull()
            assertThat(state.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `E2 8 clearError zera errorMessage`() = runTest(testDispatcher) {
        val activeUserProvider: ActiveUserProvider = mockk {
            every { observeActiveUserId() } returns flowOf("u1")
        }
        val projectRepository: ProjectRepository = mockk()
        every { projectRepository.observeAllForOwner("u1") } returns flow {
            throw IllegalStateException("boom")
        }

        val viewModel = DashboardViewModel(
            projectRepository = projectRepository,
            taskRepository = mockk(relaxed = true),
            activeUserProvider = activeUserProvider,
        )

        viewModel.uiState.test {
            advanceUntilIdle()
            assertThat(viewModel.errorMessage.value).isEqualTo(DashboardViewModel.ERROR_LOAD_FAILED)

            viewModel.clearError()
            assertThat(viewModel.errorMessage.value).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
