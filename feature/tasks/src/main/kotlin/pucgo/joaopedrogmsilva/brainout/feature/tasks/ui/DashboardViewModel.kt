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
// Marco E2.8 — captura falhas do Room via `.catch` (re-lançando
// `CancellationException` para preservar o cancelamento estruturado),
// expõe `errorMessage` e oferece `retry()` com token de re-assinatura,
// seguindo o mesmo padrão de HomeViewModel/TasksViewModel.

package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import pucgo.joaopedrogmsilva.brainout.core.data.session.ActiveUserProvider
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskPriorityCount
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

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
    val errorMessage: String? = null,
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
    ) : ViewModel() {
        /**
         * Token de retry (E2.8). Cada chamada a [retry] incrementa este
         * valor; o pipeline [uiState] depende dele como chave de
         * `flatMapLatest`, descartando a coleta atual e re-assinando os
         * repositórios.
         */
        private val _retryToken: MutableStateFlow<Int> = MutableStateFlow(0)
        val retryToken: StateFlow<Int> = _retryToken.asStateFlow()

        private val _errorMessage: MutableStateFlow<String?> = MutableStateFlow(null)
        val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

        val uiState: StateFlow<DashboardUiState> =
            combine(
                _retryToken
                    .flatMapLatest { _ -> dashboardLoadResults() },
                _errorMessage,
            ) { result, err ->
                val ui =
                    when (result) {
                        is DashboardLoadResult.Loaded -> result.state
                        is DashboardLoadResult.Failed -> DashboardUiState(isLoading = false)
                    }
                // A mensagem vive em `_errorMessage`; o estado interno da
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
         * [DashboardLoadResult], "leu" e "falhou" são estados diferentes
         * por construção.
         */
        private fun dashboardLoadResults(): Flow<DashboardLoadResult> {
            val loaded: Flow<DashboardLoadResult> =
                activeUserProvider
                    .observeActiveUserId()
                    .flatMapLatest { ownerId ->
                        val inner: Flow<DashboardLoadResult> =
                            if (ownerId == null) {
                                flowOf(DashboardLoadResult.Loaded(DashboardUiState(isLoading = false)))
                            } else {
                                combine(
                                    projectRepository.observeAllForOwner(ownerId),
                                    taskRepository.observeCountByPriority(ownerId),
                                    taskRepository.observeCompletionStats(
                                        ownerId = ownerId,
                                        weekStartMillis = currentWeekStartMillis(),
                                    ),
                                ) { projects, priorityCounts, stats ->
                                    DashboardLoadResult.Loaded(
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
            return loaded
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    Log.e(
                        TAG,
                        "Erro ao carregar estatísticas do dashboard: ${throwable.message}",
                        throwable,
                    )
                    _errorMessage.value = DashboardViewModel.ERROR_LOAD_FAILED
                    emit(DashboardLoadResult.Failed)
                }
                // Limpa o erro pendente APENAS em leituras bem-sucedidas;
                // sem esse filtro o `catch` seria "engolido" na sequência.
                .onEach { result ->
                    if (result is DashboardLoadResult.Loaded) {
                        _errorMessage.value = null
                    }
                }
        }

        /** Resultado de uma leitura das estatísticas do painel. */
        private sealed interface DashboardLoadResult {
            /** Leitura bem-sucedida. */
            data class Loaded(
                val state: DashboardUiState,
            ) : DashboardLoadResult

            /**
             * A leitura falhou. A mensagem mora em `_errorMessage` — este
             * tipo só distingue o desfecho, sem duplicar o texto.
             */
            data object Failed : DashboardLoadResult
        }

        /**
         * Re-assina o pipeline após uma falha (E2.8). Incrementa
         * [retryToken], o que dispara o `flatMapLatest` em [uiState].
         */
        fun retry() {
            Log.d(TAG, "retry acionado no Dashboard")
            _errorMessage.value = null
            _retryToken.value = _retryToken.value + 1
        }

        /** Limpa a mensagem de erro atual (E2.8). Chamado pela UI ao dispensar o banner. */
        fun clearError() {
            _errorMessage.value = null
        }

        /**
         * Início da semana corrente em epoch millis (UTC), segunda-feira
         * 00:00 via [TemporalAdjusters.previousOrSame]. Semana UTC evita
         * depender do fuso do dispositivo para a agregação SQL — a contagem
         * é estável entre sessões e dispositivos.
         */
        private fun currentWeekStartMillis(): Long =
            Instant
                .now()
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

            /**
             * Mensagem canônica para falhas de carga (E2.8). A UI resolve
             * esta chave para o recurso localizado via
             * `R.string.dashboard_error_load_failed`.
             */
            const val ERROR_LOAD_FAILED: String = "Não foi possível carregar o dashboard"

            /** Indica se [message] é a de falha de carga. */
            fun isLoadErrorMessage(message: String): Boolean = message == ERROR_LOAD_FAILED
        }
    }
