// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// ViewModel da [DashboardScreen] — visão consolidada (E2.7 do ROADMAP).
//
// Combina três fontes reativas do Room para o owner ativo:
// - contagem de projetos por estado (ativos/concluídos) via
//   [ProjectRepository.observeAllForOwner];
// - gráfico de tarefas por prioridade (0..4) via
//   [TaskRepository.observeCountByPriority];
// - taxa de conclusão semanal via [TaskRepository.observeCompletionStats].
//
// Todos os Flows são invalidados automaticamente pelo Room quando o
// banco muda — os gráficos atualizam sem recarregar a tela.
//
// Marco E2.8 — captura falhas do Room via [withLoadFailurePolicy]
// (re-lançando `CancellationException` para preservar o cancelamento
// estruturado), expõe `errorMessage` e oferece `retry()` com token de
// re-assinatura, seguindo o mesmo padrão de HomeViewModel/TasksViewModel.

package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import pucgo.joaopedrogmsilva.brainout.core.data.session.ActiveUserProvider
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskPriorityCount
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository
import java.time.Clock
import java.time.DayOfWeek
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/**
 * Estado de UI da [DashboardScreen] (E2.7).
 *
 * @property activeProjects projetos com `isCompleted = false`.
 * @property completedProjects projetos com `isCompleted = true`.
 * @property priorityCounts contagens por prioridade, índice 0 = nível
 *  LOW … índice 4 = nível CRITICAL — sempre 5 entradas.
 * @property totalTasks total de tarefas do owner.
 * @property doneTasks tarefas concluídas (total histórico).
 * @property weeklyCompletionPercent taxa de conclusão semanal (0..100):
 *  concluídas na semana corrente / total de tarefas.
 * @property overallCompletionPercent taxa de conclusão global (0..100).
 * @property isLoading `true` até a primeira emissão combinada.
 * @property errorMessage mensagem da última falha de carga, ou `null`.
 */
data class DashboardUiState(
    val activeProjects: Int = 0,
    val completedProjects: Int = 0,
    val priorityCounts: List<Int> = List(PRIORITY_LEVELS) { 0 },
    val totalTasks: Int = 0,
    val doneTasks: Int = 0,
    val weeklyCompletionPercent: Int = 0,
    val overallCompletionPercent: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: TasksErrorType? = null,
) {
    companion object {
        /** Níveis de prioridade exibidos no gráfico (0..4 — E1.4). */
        const val PRIORITY_LEVELS: Int = 5
    }
}

/**
 * ViewModel do Dashboard (E2.7).
 *
 * Assina os Flows reativos dos repositórios enquanto a tela estiver
 * visível (`WhileSubscribed(5_000)`), reagindo ao owner ativo via
 * [ActiveUserProvider.observeActiveUserId]. Falhas do Room caem no
 * `.catch` do pipeline (E2.8) e podem ser recuperadas com [retry].
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel
    @Inject
    constructor(
        private val projectRepository: ProjectRepository,
        private val taskRepository: TaskRepository,
        private val activeUserProvider: ActiveUserProvider,
        private val clock: Clock,
    ) : ViewModel() {
        /** Estado de leitura compartilhado com a lista de Tarefas (retry + erro). */
        private val loadState = TasksLoadState()

        /** Mensagem de erro pendente para a UI (E2.8). */
        val errorMessage: StateFlow<TasksErrorType?> = loadState.errorMessage

        val uiState: StateFlow<DashboardUiState> =
            combine(
                loadState.retryToken
                    .flatMapLatest { _ -> dashboardLoadResults() },
                loadState.errorMessage,
            ) { result, err ->
                val ui =
                    when (result) {
                        is TasksLoadResult.Loaded -> result.value
                        is TasksLoadResult.Failed -> DashboardUiState(isLoading = false)
                    }
                // A mensagem vive em `loadState`; o estado interno da
                // leitura só carrega os dados.
                ui.copy(errorMessage = err)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = DashboardUiState(),
            )

        /**
         * Leitura das estatísticas do painel, com a falha tipada.
         *
         * Mesmo desenho de [TasksViewModel.taskLoadResults]: o `catch`
         * emitia um estado com `errorMessage` preenchido e o passo
         * seguinte usava esse campo como flag de sucesso, enquanto o
         * `combine` final sobrescrevia o mesmo campo. Com
         * [TasksLoadResult], "leu" e "falhou" são estados diferentes
         * por construção — e a política de falha é a mesma das duas
         * telas, em [withLoadFailurePolicy].
         */
        private fun dashboardLoadResults(): Flow<TasksLoadResult<DashboardUiState>> {
            val loaded: Flow<TasksLoadResult<DashboardUiState>> =
                activeUserProvider
                    .observeActiveUserId()
                    .flatMapLatest { ownerId ->
                        val inner: Flow<TasksLoadResult<DashboardUiState>> =
                            if (ownerId == null) {
                                flowOf(TasksLoadResult.Loaded(DashboardUiState(isLoading = false)))
                            } else {
                                combine(
                                    projectRepository.observeAllForOwner(ownerId),
                                    taskRepository.observeCountByPriority(ownerId),
                                    taskRepository.observeCompletionStats(
                                        ownerId = ownerId,
                                        weekStartMillis = currentWeekStartMillis(),
                                    ),
                                ) { projects, priorityCounts, stats ->
                                    TasksLoadResult.Loaded(
                                        DashboardUiState(
                                            activeProjects = projects.count { !it.isCompleted },
                                            completedProjects = projects.count { it.isCompleted },
                                            priorityCounts = normalizePriorityCounts(priorityCounts),
                                            totalTasks = stats.totalCount,
                                            doneTasks = stats.doneCount,
                                            weeklyCompletionPercent = stats.weeklyCompletionPercent,
                                            overallCompletionPercent = stats.overallCompletionPercent,
                                            isLoading = false,
                                        ),
                                    )
                                }
                            }
                        inner
                    }
            return loaded.withLoadFailurePolicy(loadState) { throwable ->
                Log.e(
                    TAG,
                    "Erro ao carregar estatísticas do dashboard: ${throwable.message}",
                    throwable,
                )
            }
        }

        /**
         * Re-assina o pipeline após uma falha (E2.8). Incrementa o token
         * de retry, o que dispara o `flatMapLatest` em [uiState].
         */
        fun retry() {
            Log.d(TAG, "retry acionado no Dashboard")
            loadState.retry()
        }

        /** Limpa a mensagem de erro atual (E2.8). Chamado pela UI ao dispensar o banner. */
        fun clearError() {
            loadState.clearError()
        }

        /**
         * Início da semana corrente em epoch millis (UTC), segunda-feira
         * 00:00 via [TemporalAdjusters.previousOrSame]. Semana UTC evita
         * depender do fuso do dispositivo para a agregação SQL — a contagem
         * é estável entre sessões e dispositivos.
         *
         * O instante vem do [clock] injetado, e não de `Instant.now()`:
         * com o relógio do sistema dentro do ViewModel, o cálculo não era
         * determinístico e o teste dependia do dia em que rodava.
         *
         * O valor é calculado na assinatura do fluxo. Como o `stateIn` usa
         * `WhileSubscribed(5_000)`, reabrir a tela depois da virada
         * recalcula a semana — o caso que ficava desatualizado é o de uma
         * tela aberta atravessando a virada, que é raro e está
         * documentado aqui.
         */
        private fun currentWeekStartMillis(): Long =
            clock
                .instant()
                .atZone(ZoneOffset.UTC)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .toLocalDate()
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()

        /**
         * Garante sempre 5 entradas (níveis 0..4). Prioridades ausentes na
         * resposta do DAO ficam com contagem zero.
         */
        private fun normalizePriorityCounts(counts: List<TaskPriorityCount>): List<Int> {
            val byCode = counts.associate { it.priorityCode to it.count }
            return TaskPriority.VALID_CODES.map { code -> byCode[code] ?: 0 }
        }

        companion object {
            private const val TAG = "BrainOut:DashboardVM"
        }
    }
