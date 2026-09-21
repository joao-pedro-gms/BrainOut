// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// ViewModel da [ProjectDetailScreen] — observador reativo das tarefas
// do projeto e fachada dos casos de uso de CRUD (RN01, mudança de
// status, renomeação, exclusão do projeto).

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pucgo.joaopedrogmsilva.brainout.core.domain.error.ProjectTaskLimitReachedException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TaskPriorityChangeForbiddenException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.ChangeTaskStatusUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CreateTaskUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.DeleteProjectUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.DeleteTaskUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.MAX_ACTIVE_TASKS_PER_PROJECT
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.UpdateTaskUseCase

/**
 * Estado de UI da [ProjectDetailScreen].
 *
 * Combina a lista de tarefas observada do [TaskRepository] com mensagens
 * de erro efêmeras disparadas pelas ações do usuário (RN01, título
 * inválido, etc.). O `errorMessage` é tratado pela camada de UI com
 * `LaunchedEffect` que dispara `clearError()` após alguns segundos.
 *
 * E2.8 — `isLoading` permanece `true` até o primeiro emit do Flow de
 * tarefas ou até o `catch` final converter uma falha em
 * `errorMessage` + `isLoading = false`. Antes deste marco a UI nunca
 * via um spinner porque o `_errorMessage.value` do map nunca
 * propagava corretamente.
 */
data class ProjectDetailUiState(
    val projectId: String,
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

/**
 * ViewModel da [ProjectDetailScreen].
 *
 * Responsabilidades:
 * - Carregar a lista de tarefas do projeto via [TaskRepository.observeForProject].
 * - Expor operações de mutação que delegam aos use cases já existentes em
 *   `:core:domain` ([CreateTaskUseCase], [UpdateTaskUseCase],
 *   [ChangeTaskStatusUseCase], [DeleteProjectUseCase]).
 * - Traduzir violações de regra de negócio (RN01) em mensagens de UI.
 * - E2.8 — capturar falhas dos Flows do Room e dos use cases de CRUD,
 *   expondo [ProjectDetailUiState.errorMessage] e oferecendo
 *   [retry] / [clearError] para a UI mostrar banner com botão
 *   "Tentar novamente".
 *
 * O `projectId` vem do [SavedStateHandle] injetado pelo Navigation
 * Compose ao resolver a rota `project/{projectId}`.
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ProjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @Suppress("unused") private val taskRepository: TaskRepository,
    private val createTask: CreateTaskUseCase,
    private val updateTask: UpdateTaskUseCase,
    private val changeStatus: ChangeTaskStatusUseCase,
    private val deleteProject: DeleteProjectUseCase,
    private val deleteTask: DeleteTaskUseCase,
) : ViewModel() {

    private val projectId: String =
        checkNotNull(savedStateHandle.get<String>(PROJECT_ID_ARG)) {
            "projectId é obrigatório em ProjectDetail"
        }

    private val _errorMessage: MutableStateFlow<String?> = MutableStateFlow(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Token de retry (E2.8). Cada chamada a [retry] incrementa este
     * valor; o pipeline [uiState] depende dele via `flatMapLatest`,
     * então a mudança descarta a coleta atual e re-assina o Flow do
     * [TaskRepository].
     */
    private val _retryToken: MutableStateFlow<Int> = MutableStateFlow(0)
    val retryToken: StateFlow<Int> = _retryToken.asStateFlow()

    /**
     * Estado de UI da tela — E2.1/E2.2 + E2.8.
     *
     * Cada emissão do Flow do [TaskRepository] é mapeada para um
     * [ProjectDetailUiState] com `isLoading = false`. O `.catch`
     * final (E2.8) captura falhas do Room e as converte em
     * `errorMessage` + `isLoading = false`, para que a UI possa
     * mostrar o banner de erro com "Tentar novamente" em vez de
     * ficar presa em "Loading" para sempre.
     *
     * [CancellationException] é re-lançada para preservar o
     * cancelamento estruturado de corrotinas (não engolir sinal de
     * cancelamento do ViewModel).
     */
    val uiState: StateFlow<ProjectDetailUiState> = _retryToken
        .flatMapLatest { _ ->
            taskRepository
                .observeForProject(projectId)
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    val message = throwable.toProjectDetailErrorMessage()
                    _errorMessage.value = message
                    // Lista de sentinela — o `onEach` abaixo a
                    // distingue da lista vazia real do Room para
                    // evitar "engolir" o erro pendente.
                    emit(sentinelOnError)
                }
                // Limpa erro pendente APENAS em emissões vindas do
                // upstream (sucesso do Flow), não na sentinela
                // emitida pelo `catch` acima. Sem esse filtro, o
                // `catch` emitiria a lista vazia e o `onEach`
                // seguinte resetaria o erro para `null`
                // imediatamente, "engolindo" a falha. (E2.8)
                .onEach { tasks ->
                    if (tasks !== sentinelOnError) {
                        _errorMessage.value = null
                    }
                }
        }
        .let { tasksFlow ->
            kotlinx.coroutines.flow.flow {
                tasksFlow.collect { tasks ->
                    emit(
                        ProjectDetailUiState(
                            projectId = projectId,
                            tasks = tasks,
                            isLoading = false,
                            errorMessage = _errorMessage.value,
                        ),
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = ProjectDetailUiState(projectId = projectId),
        )

    /**
     * Sentinela interna para distinguir a lista vazia emitida pelo
     * `catch` (que sinaliza erro) de uma lista vazia real do Room.
     * Evita que o `onEach` "engola" o erro pendente logo após o
     * `catch` emitir a lista.
     *
     * Criada uma única vez por ViewModel — cada `catch` produz um
     * `emit(sentinelOnError)`, então o downstream recebe a mesma
     * referência. Como a lista do Room é uma `MutableList` real
     * (vinda do Cursor do `RoomDatabase`), nunca terá referência
     * idêntica a esta `ListOf(0)`.
     */
    private val sentinelOnError: List<Task> = listOf()

    /**
     * Cria uma nova tarefa no projeto corrente. Aplica a RN01
     * (limite de [MAX_ACTIVE_TASKS_PER_PROJECT] tarefas ativas)
     * através do [CreateTaskUseCase] — se a regra for violada,
     * uma mensagem amigável é exposta via [errorMessage] para que
     * a UI mostre o chip de erro e chame [clearError] após 5s.
     *
     * E2.8 — também captura falhas genéricas do Room (e.g.
     * `SQLiteException`) que não são mapeadas por exceções de
     * domínio, evitando crash silencioso.
     */
    fun addTask(title: String, priority: TaskPriority = TaskPriority.MEDIUM) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            _errorMessage.update { ERROR_EMPTY_TITLE }
            return
        }
        viewModelScope.launch {
            try {
                createTask.invoke(
                    projectId = projectId,
                    title = trimmed,
                    priority = priority,
                )
            } catch (e: ProjectTaskLimitReachedException) {
                @Suppress("SwallowedException")
                val message = e.message ?: taskLimitMessage()
                _errorMessage.update { message }
            } catch (e: pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException) {
                _errorMessage.update { e.message ?: ERROR_INVALID_TITLE }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _errorMessage.update { e.toProjectDetailErrorMessage() }
            }
        }
    }

    /**
     * Move a [task] para [target], respeitando a matriz de transições.
     * RN03 (E2.5) — a transição para DONE dispara a cascata de
     * conclusão no repositório (projeto é marcado como concluído
     * se a tarefa for a última ativa); reabertura também passa pela
     * cascata inversa.
     */
    fun changeStatus(taskId: String, target: TaskStatus) {
        viewModelScope.launch {
            try {
                changeStatus.invoke(taskId, target)
            } catch (e: pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidStateTransitionException) {
                _errorMessage.update { e.message ?: ERROR_INVALID_TRANSITION }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _errorMessage.update { e.toProjectDetailErrorMessage() }
            }
        }
    }

    /**
     * Altera a prioridade de uma tarefa (RN02 — E2.4).
     *
     * A UI já bloqueia tarefas concluídas, mas o ViewModel
     * também aplica a defesa em profundidade: se a tarefa
     * persistida estiver em [TaskStatus.DONE], a operação é
     * ignorada e o erro é exposto via [errorMessage] para que a
     * UI possa exibir a mensagem específica de RN02 (chip
     * bloqueado).
     *
     * Em tarefas ativas, usa [Task.changePriority] (que valida o
     * intervalo 0..4) e depois delega ao [UpdateTaskUseCase] — este
     * use case recarrega o estado persistido e rejeita tentativas
     * com a persistência em DONE (bypass via `copy`).
     */
    fun changeTaskPriority(task: Task, newPriority: TaskPriority) {
        val updated = try {
            task.changePriority(newPriority)
        } catch (e: TaskPriorityChangeForbiddenException) {
            _errorMessage.update { e.message ?: ERROR_PRIORITY_LOCKED }
            return
        } catch (e: pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException) {
            _errorMessage.update { e.message ?: ERROR_INVALID_TITLE }
            return
        }
        viewModelScope.launch {
            try {
                updateTask.invoke(updated)
            } catch (e: TaskPriorityChangeForbiddenException) {
                // Persistência indica DONE (bypass via copy).
                _errorMessage.update { e.message ?: ERROR_PRIORITY_LOCKED }
            } catch (e: pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException) {
                _errorMessage.update { e.message ?: ERROR_INVALID_TITLE }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _errorMessage.update { e.toProjectDetailErrorMessage() }
            }
        }
    }

    /** Renomeia a [task] via [Task.rename], que valida o título. */
    fun renameTask(task: Task, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isEmpty() || trimmed == task.title) return
        viewModelScope.launch {
            try {
                updateTask.invoke(task.rename(trimmed))
            } catch (e: pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException) {
                _errorMessage.update { e.message ?: ERROR_INVALID_TITLE }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _errorMessage.update { e.toProjectDetailErrorMessage() }
            }
        }
    }

    /** Remove a [task] do projeto (ação do menu "Excluir" do item). */
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                deleteTask.invoke(task.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _errorMessage.update { e.toProjectDetailErrorMessage() }
            }
        }
    }

    /**
     * Exclui o projeto corrente e, em seguida, chama [onDone] para
     * que a camada de navegação faça `popBackStack()`.
     */
    fun deleteProject(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                deleteProject.invoke(projectId)
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _errorMessage.update { e.toProjectDetailErrorMessage() }
            }
        }
    }

    /** Limpa a mensagem de erro atual — usado pela UI após o chip expirar. */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Re-assina o pipeline de observação de tarefas após uma falha
     * (E2.8). Incrementa [retryToken], o que dispara o
     * `flatMapLatest` em [uiState] e descarta a coleta atual.
     */
    fun retry() {
        _errorMessage.value = null
        _retryToken.value = _retryToken.value + 1
    }

    companion object {
        /** Nome do argumento da rota do Navigation Compose. */
        const val PROJECT_ID_ARG: String = "projectId"

        // Mensagens embutidas no ViewModel em vez de strings.xml —
        // mesma decisão do AuthViewModel (ver comentário no companion
        // object de lá): frases de validação técnica que não dependem
        // de localização para os testes de VM; a camada de UI traduz
        // para o recurso localizado via `resolveProjectDetailMessage`
        // (E4.6) quando a mensagem casa com uma chave conhecida.
        const val ERROR_EMPTY_TITLE: String = "Título da tarefa não pode ser vazio"
        const val ERROR_INVALID_TITLE: String = "Título inválido"
        const val ERROR_INVALID_TRANSITION: String = "Transição de status inválida"
        const val ERROR_PRIORITY_LOCKED: String =
            "RN02: alteração de prioridade bloqueada em tarefa concluída"

        /**
         * Mensagem canônica para falhas genéricas do Room/use case
         * (E2.8). A UI resolve esta chave para o recurso localizado
         * via `R.string.project_detail_error_load_failed`.
         */
        const val ERROR_LOAD_FAILED: String = "Não foi possível carregar as tarefas do projeto"

        private const val ERROR_TASK_LIMIT_PREFIX: String = "Erro: limite de "
        private const val ERROR_TASK_LIMIT_SUFFIX: String = " tarefas atingido"

        /** Mensagem canônica de RN01 para o limite informado (usada pela UI). */
        fun taskLimitMessage(limit: Int = MAX_ACTIVE_TASKS_PER_PROJECT): String =
            "$ERROR_TASK_LIMIT_PREFIX$limit$ERROR_TASK_LIMIT_SUFFIX"

        /** Indica se [message] é a mensagem de RN01 (limite de tarefas). */
        fun isTaskLimitMessage(message: String): Boolean =
            message.startsWith(ERROR_TASK_LIMIT_PREFIX) && message.endsWith(ERROR_TASK_LIMIT_SUFFIX)

        /** Indica se [message] é a mensagem canônica de RN02 (prioridade bloqueada). */
        fun isPriorityLockedMessage(message: String): Boolean =
            message.startsWith(ERROR_PRIORITY_LOCKED_PREFIX) && ERROR_PRIORITY_LOCKED_TAG in message

        /** Indica se [message] é a mensagem canônica de falha de carga (E2.8). */
        fun isLoadErrorMessage(message: String): Boolean = message == ERROR_LOAD_FAILED

        private const val ERROR_PRIORITY_LOCKED_PREFIX: String = "RN02: alteração de prioridade bloqueada"
        private const val ERROR_PRIORITY_LOCKED_TAG: String = "RN02"
    }
}

/**
 * Converte uma [Throwable] vinda do Room em uma mensagem canônica
 * para a UI (E2.8). Mantém a regra de não expor stack traces: a UI
 * só recebe a chave de recurso; o detalhe técnico fica em logcat.
 */
internal fun Throwable.toProjectDetailErrorMessage(): String =
    ProjectDetailViewModel.ERROR_LOAD_FAILED
