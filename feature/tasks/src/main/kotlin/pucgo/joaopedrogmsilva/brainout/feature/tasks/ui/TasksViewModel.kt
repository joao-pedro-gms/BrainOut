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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    val errorMessage: String? = null,
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
class TasksViewModel @Inject constructor(
    @Suppress("unused") private val taskRepository: TaskRepository,
    @Suppress("unused") private val projectRepository: ProjectRepository,
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

    private val _errorMessage: MutableStateFlow<String?> = MutableStateFlow(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val uiState: StateFlow<TasksUiState> = combine(
        _retryToken
            .flatMapLatest { _ ->
                activeUserProvider.observeActiveUserId()
                    .flatMapLatest { ownerId ->
                        if (ownerId == null) {
                            flowOf(TasksUiState(isLoading = false))
                        } else {
                            taskRepository.observeAllForOwner(ownerId)
                                .flatMapLatest { tasks ->
                                    projectRepository.observeAllForOwner(ownerId).map { projects ->
                                        TasksUiState(
                                            rows = tasks.map { it.toRow(projects) },
                                            isLoading = false,
                                        )
                                    }
                                }
                        }
                    }
                    .catch { throwable ->
                        if (throwable is CancellationException) throw throwable
                        val message = throwable.toTasksErrorMessage()
                        _errorMessage.value = message
                        emit(TasksUiState(isLoading = false, errorMessage = message))
                    }
                    // Limpa erro pendente APENAS em emissões vindas do
                    // `flatMapLatest` (sucesso), não nas emitidas pelo
                    // `catch` acima — identificadas pelo
                    // `errorMessage == null`. Sem esse filtro, o `catch`
                    // emitiria um estado com erro e o `onEach` seguinte
                    // o resetaria para `null` imediatamente, "engolindo"
                    // a falha. (E2.8)
                    .onEach { state ->
                        if (state.errorMessage == null) {
                            _errorMessage.value = null
                        }
                    }
            },
        _errorMessage,
    ) { ui, err ->
        if (err != null) ui.copy(errorMessage = err) else ui.copy(errorMessage = null)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = TasksUiState(),
        )

    /**
     * Re-assina o pipeline de observação dos repositórios após uma
     * falha (E2.8). Incrementa [retryToken], o que dispara o
     * `flatMapLatest` em [uiState] e descarta a coleta atual.
     */
    fun retry() {
        _errorMessage.value = null
        _retryToken.value = _retryToken.value + 1
    }

    /** Limpa a mensagem de erro atual (E2.8). Chamado pela UI quando o usuário dispensa o banner. */
    fun clearError() {
        _errorMessage.value = null
    }

    private fun Task.toRow(projects: List<Project>): TaskRow {
        val byId = projects.associateBy { it.id }
        return TaskRow(
            task = this,
            projectName = byId[this.projectId]?.name ?: NO_PROJECT_LABEL,
        )
    }

    companion object {
        /** Texto exibido quando a tarefa referencia um projeto inexistente. */
        const val NO_PROJECT_LABEL: String = "(sem projeto)"

        /**
         * Mensagem canônica para falhas de carga (E2.8). A UI
         * resolve esta chave para o recurso localizado via
         * `R.string.tasks_error_load_failed`.
         */
        const val ERROR_LOAD_FAILED: String = "Não foi possível carregar suas tarefas"

        /** Indica se [message] é a de falha de carga. */
        fun isLoadErrorMessage(message: String): Boolean = message == ERROR_LOAD_FAILED
    }
}

/**
 * Converte uma [Throwable] vinda do Room em uma mensagem canônica
 * para a UI (E2.8). Mantém a regra de não expor stack traces: a UI
 * só recebe a chave de recurso; o detalhe técnico fica em logcat.
 */
internal fun Throwable.toTasksErrorMessage(): String = TasksViewModel.ERROR_LOAD_FAILED
