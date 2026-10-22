// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// ViewModel da [TasksScreen] — observador reativo de TODAS as tarefas
// do owner ativo, com join do nome do projeto correspondente para
// exibição direta na linha.
//
// Marco E2.6 do ROADMAP.md — corresponde à aba "Tarefas" da HomeScreen,
// que antes mostrava um placeholder estático.
// Marco E2.8 — captura falhas do Room, expõe `errorMessage` no estado
// e oferece `retry()` para re-assinar o pipeline.

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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import pucgo.joaopedrogmsilva.brainout.core.data.session.ActiveUserProvider
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Estado de UI da [TasksScreen].
 *
 * Combina a lista de tarefas do owner (vinda de
 * [TaskRepository.observeAllForOwner]) com o mapa de projetos (vindo
 * de [ProjectRepository.observeAllForOwner]) para produzir linhas já
 * com o nome do projeto pronto para renderização. Sem owner ativo
 * (sessão nula) retornamos lista vazia e `isLoading = false` — a UI
 * cai no estado vazio sem precisar de uma segunda fonte de verdade.
 *
 * E2.8 — `errorMessage` é alimentado quando o Flow do Room emite
 * uma exceção (e.g. `SQLiteException`); a UI renderiza banner de
 * erro com botão "Tentar novamente".
 *
 * @property rows Linhas a renderizar (uma por tarefa do owner).
 * @property isLoading `true` enquanto a cadeia `flatMapLatest` ainda
 *  não emitiu a primeira lista — útil para a UI decidir entre o
 *  spinner e a lista vazia sem ter que conhecer a estrutura interna
 *  do `StateFlow`.
 * @property errorMessage mensagem da última falha ao carregar a
 *  lista, ou `null` quando não há erro pendente.
 */
data class TasksUiState(
    val rows: List<TaskRow> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: TasksErrorType? = null,
)

/**
 * Linha exibida pela [TasksScreen] — agrupa a [Task] com o nome do
 * projeto para evitar lookups repetidos na camada de UI.
 *
 * @property task Tarefa persistida no Room.
 * @property projectName Nome do projeto correspondente, ou
 *  `"(sem projeto)"` quando a FK ficou orfã (projeto excluído sem
 *  cascade das tarefas — improvável dado o schema atual, mas o
 *  fallback protege a UI).
 */
data class TaskRow(
    val task: Task,
    val projectName: String,
)

/**
 * ViewModel da [TasksScreen].
 *
 * Responsabilidades:
 * - Reagir à sessão ativa via [ActiveUserProvider.observeActiveUserId].
 * - Observar todas as tarefas do owner via
 *   [TaskRepository.observeAllForOwner].
 * - Observar todos os projetos do owner (em paralelo) via
 *   [ProjectRepository.observeAllForOwner] e fazer o join por
 *   `projectId` para popular [TaskRow.projectName].
 * - Emitir [TasksUiState] com `isLoading = true` enquanto a primeira
 *   lista não chegou — `WhileSubscribed(5_000)` mantém o upstream vivo
 *   durante reconfigurações, espelhando o padrão da `ProjectDetailViewModel`.
 * - E2.8 — capturar falhas dos Flows do Room (via `.catch` + token
 *   de retry) e expor a mensagem resultante em [uiState] /
 *   [errorMessage] para que a UI mostre banner com botão
 *   "Tentar novamente".
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModel
    @Inject
    constructor(
        private val taskRepository: TaskRepository,
        private val projectRepository: ProjectRepository,
        private val activeUserProvider: ActiveUserProvider,
    ) : ViewModel() {
        /**
         * Token de retry (E2.8). Cada chamada a [retry] incrementa este
         * valor; o pipeline [uiState] depende dele como chave de
         * `flatMapLatest`, então a mudança descarta o Flow anterior
         * (liberando o `WhileSubscribed` interno) e inicia uma nova
         * inscrição nos repositórios.
         */
        private val _retryToken: MutableStateFlow<Int> = MutableStateFlow(0)
        val retryToken: StateFlow<Int> = _retryToken.asStateFlow()

        private val _errorMessage: MutableStateFlow<TasksErrorType?> = MutableStateFlow(null)
        val errorMessage: StateFlow<TasksErrorType?> = _errorMessage.asStateFlow()

        val uiState: StateFlow<TasksUiState> =
            combine(
                _retryToken
                    .flatMapLatest { _ -> taskLoadResults() },
                _errorMessage,
            ) { result, err ->
                val ui =
                    when (result) {
                        is TasksLoadResult.Loaded -> result.state
                        is TasksLoadResult.Failed -> TasksUiState(isLoading = false)
                    }
                // A mensagem vive em `_errorMessage`; o estado interno da
                // leitura só carrega os dados.
                ui.copy(errorMessage = err)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = TasksUiState(),
            )

        /**
         * Leitura das tarefas do usuário ativo, com a falha tipada.
         *
         * Antes, o `catch` emitia um [TasksUiState] com `errorMessage`
         * preenchido e o passo seguinte usava esse campo como
         * "isto foi sucesso ou falha?" — enquanto o `combine` final
         * sobrescrevia o mesmo campo com `_errorMessage`. O campo era
         * escrito duas vezes e lido uma vez, só para servir de flag.
         *
         * Com [TasksLoadResult], "leu" e "falhou" são estados
         * diferentes por construção, e a mensagem tem uma fonte só.
         */
        private fun taskLoadResults(): Flow<TasksLoadResult> {
            val loaded: Flow<TasksLoadResult> =
                activeUserProvider
                    .observeActiveUserId()
                    .flatMapLatest { ownerId ->
                        val inner: Flow<TasksLoadResult> =
                            if (ownerId == null) {
                                flowOf(TasksLoadResult.Loaded(TasksUiState(isLoading = false)))
                            } else {
                                taskRepository
                                    .observeAllForOwner(ownerId)
                                    .flatMapLatest { tasks ->
                                        projectRepository.observeAllForOwner(ownerId).map { projects ->
                                            // O índice é montado uma vez por
                                            // emissão: dentro do `map` de
                                            // tarefas, um `associateBy` por
                                            // linha custaria O(tarefas ×
                                            // projetos).
                                            val projectsById = projects.associateBy { it.id }
                                            TasksLoadResult.Loaded(
                                                TasksUiState(
                                                    rows = tasks.map { it.toRow(projectsById) },
                                                    isLoading = false,
                                                ),
                                            )
                                        }
                                    }
                            }
                        inner
                    }
            return loaded
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    Log.e(TAG, "Erro ao carregar tarefas do owner: ${throwable.message}", throwable)
                    _errorMessage.value = TasksErrorType.LoadFailed
                    emit(TasksLoadResult.Failed)
                }
                // Limpa o erro pendente APENAS em leituras bem-sucedidas.
                // Sem esse filtro, o `catch` emitiria um estado vazio e o
                // passo seguinte resetaria o erro para `null`
                // imediatamente, "engolindo" a falha. (E2.8)
                .onEach { result ->
                    if (result is TasksLoadResult.Loaded) {
                        _errorMessage.value = null
                    }
                }
        }

        /** Resultado de uma leitura da lista de tarefas. */
        private sealed interface TasksLoadResult {
            /** Leitura bem-sucedida (a lista pode estar vazia). */
            data class Loaded(
                val state: TasksUiState,
            ) : TasksLoadResult

            /**
             * A leitura falhou. A mensagem mora em `_errorMessage` — este
             * tipo só distingue o desfecho, sem duplicar o texto.
             */
            data object Failed : TasksLoadResult
        }

        /**
         * Re-assina o pipeline de observação dos repositórios após uma
         * falha (E2.8). Incrementa [retryToken], o que dispara o
         * `flatMapLatest` em [uiState] e descarta a coleta atual.
         */
        fun retry() {
            Log.d(TAG, "retry acionado em TasksViewModel")
            _errorMessage.value = null
            _retryToken.value = _retryToken.value + 1
        }

        /** Limpa a mensagem de erro atual (E2.8). Chamado pela UI quando o usuário dispensa o banner. */
        fun clearError() {
            _errorMessage.value = null
        }

        private fun Task.toRow(projectsById: Map<String, Project>): TaskRow =
            TaskRow(
                task = this,
                projectName = projectsById[this.projectId]?.name ?: NO_PROJECT_LABEL,
            )

        companion object {
            private const val TAG = "BrainOut:TasksVM"

            /** Texto exibido quando a tarefa referencia um projeto inexistente. */
            const val NO_PROJECT_LABEL: String = "(sem projeto)"
        }
    }
