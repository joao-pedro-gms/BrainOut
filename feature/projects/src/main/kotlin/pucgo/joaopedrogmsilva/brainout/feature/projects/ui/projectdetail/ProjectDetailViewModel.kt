// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// ViewModel da [ProjectDetailScreen] — observador reativo das tarefas
// do projeto e fachada dos casos de uso de CRUD (RN01, mudança de
// status, renomeação, exclusão do projeto).

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CheckDeadlineUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CreateTaskUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.DeadlineInfo
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.DeleteProjectUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.DeleteTaskUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.MAX_ACTIVE_TASKS_PER_PROJECT
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.UpdateTaskUseCase
import java.time.Instant
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

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
    val errorMessage: ProjectDetailError? = null,
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
@Suppress("LongParameterList") // E3.5 injeta CheckDeadlineUseCase (9 deps).
class ProjectDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val taskRepository: TaskRepository,
        private val createTask: CreateTaskUseCase,
        private val updateTask: UpdateTaskUseCase,
        private val changeStatus: ChangeTaskStatusUseCase,
        private val deleteProject: DeleteProjectUseCase,
        private val deleteTask: DeleteTaskUseCase,
        private val checkDeadline: CheckDeadlineUseCase,
    ) : ViewModel() {
        private val projectId: String =
            checkNotNull(savedStateHandle.get<String>(PROJECT_ID_ARG)) {
                "projectId é obrigatório em ProjectDetail"
            }

        private val _errorMessage: MutableStateFlow<ProjectDetailError?> = MutableStateFlow(null)
        val errorMessage: StateFlow<ProjectDetailError?> = _errorMessage.asStateFlow()

        /**
         * Token de retry (E2.8). Cada chamada a [retry] incrementa este
         * valor; o pipeline [uiState] depende dele via `flatMapLatest`,
         * então a mudança descarta a coleta atual e re-assina o Flow do
         * [TaskRepository].
         */
        private val retryToken: MutableStateFlow<Int> = MutableStateFlow(0)

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
        val uiState: StateFlow<ProjectDetailUiState> =
            retryToken
                .flatMapLatest { _ -> taskLoadResults() }
                // Limpa o erro pendente APENAS em leituras bem-sucedidas.
                // Sem esse filtro, o `catch` emitiria um resultado vazio e o
                // passo seguinte resetaria o erro para `null`
                // imediatamente, "engolindo" a falha. (E2.8)
                .onEach { result ->
                    if (result is TaskLoadResult.Loaded) {
                        _errorMessage.value = null
                    }
                }.map { result ->
                    ProjectDetailUiState(
                        projectId = projectId,
                        tasks = (result as? TaskLoadResult.Loaded)?.tasks.orEmpty(),
                        isLoading = false,
                        errorMessage = _errorMessage.value,
                    )
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                    initialValue = ProjectDetailUiState(projectId = projectId),
                )

        /**
         * Leitura das tarefas do projeto, com a falha tipada.
         *
         * O `catch` converte a exceção do Room em [TaskLoadResult.Failed] e
         * registra a mensagem em [errorMessage]; o `onEach` do pipeline
         * acima só limpa o erro quando o resultado é [TaskLoadResult.Loaded].
         */
        private fun taskLoadResults(): Flow<TaskLoadResult> {
            // O tipo do Flow e' declarado explicitamente porque o `map`
            // inferiria apenas o subtipo concreto (`Loaded`); o `catch`
            // precisa de `TaskLoadResult` para poder emitir tambem o
            // caso de falha.
            val loaded: Flow<TaskLoadResult> =
                taskRepository
                    .observeForProject(projectId)
                    .map { tasks -> TaskLoadResult.Loaded(tasks) }
            return loaded.catch { throwable ->
                if (throwable is CancellationException) throw throwable
                _errorMessage.value = ProjectDetailError.LoadFailed
                emit(TaskLoadResult.Failed)
            }
        }

        /**
         * Resultado de uma leitura da lista de tarefas.
         *
         * Antes era uma "lista de sentinela" — uma `List<Task>` vazia
         * comparada **por referência** (`!==`) para distinguir a falha da
         * lista vazia de verdade do Room. Funcionava por acidente: a
         * lista do Room é sempre uma nova instância e a sentinela um
         * objeto fixo. Bastava o Room devolver a mesma instância para
         * os dois estados virarem ambíguos — e o KDoc do arquivo chegou
         * a descrever uma sentinela `ListOf(0)` que o código nunca teve.
         *
         * Com o tipo explícito, "falhou" e "não tem tarefas" são
         * estados diferentes por construção.
         */
        private sealed interface TaskLoadResult {
            /** Leitura bem-sucedida (a lista pode estar vazia). */
            data class Loaded(
                val tasks: List<Task>,
            ) : TaskLoadResult

            /**
             * A leitura falhou. A mensagem mora em [errorMessage] — este
             * tipo só distingue o desfecho, sem duplicar o texto.
             */
            data object Failed : TaskLoadResult
        }

        /**
         * Cria uma nova tarefa no projeto corrente. Aplica a RN01
         * (limite de [MAX_ACTIVE_TASKS_PER_PROJECT] tarefas ativas)
         * através do [CreateTaskUseCase] — se a regra for violada,
         * uma mensagem amigável é exposta via [errorMessage] para que
         * a UI mostre o chip de erro e chame [clearError] após 5s.
         *
         * @param dueDate prazo opcional (E3.5: integra com a checagem
         *   de feriados nacionais).
         * E2.8 — também captura falhas genéricas do Room (e.g.
         * `SQLiteException`) que não são mapeadas por exceções de
         * domínio, evitando crash silencioso.
         */
        fun addTask(
            title: String,
            priority: TaskPriority = TaskPriority.MEDIUM,
            dueDate: Instant? = null,
            onSuccess: () -> Unit = {},
        ) {
            val trimmed = title.trim()
            if (trimmed.isEmpty()) {
                _errorMessage.update { ProjectDetailError.EmptyTitle }
                return
            }
            Log.d(TAG, "addTask: title=$trimmed, priority=$priority, projectId=$projectId")
            viewModelScope.launch {
                try {
                    createTask.invoke(
                        projectId = projectId,
                        title = trimmed,
                        priority = priority,
                        dueDate = dueDate,
                    )
                    onSuccess()
                } catch (e: ProjectTaskLimitReachedException) {
                    Log.w(TAG, "Limite de tarefas ativas atingido (RN01) no projeto $projectId")
                    _errorMessage.update { ProjectDetailError.TaskLimitReached(e.limit) }
                } catch (e: pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException) {
                    Log.w(TAG, "Título de tarefa inválido: ${e.message}")
                    _errorMessage.update { ProjectDetailError.InvalidTitle }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Log.e(TAG, "Erro ao adicionar tarefa: ${e.message}", e)
                    _errorMessage.update { ProjectDetailError.ActionFailed }
                }
            }
        }

        /**
         * Avalia um prazo usando o [CheckDeadlineUseCase] (E3.5). É uma
         * fachada para que a UI não precise importar o caso de uso
         * diretamente; delega 1-para-1 e devolve [DeadlineInfo] com a
         * janela de feriado dos próximos 7 dias antes do prazo.
         */
        suspend fun evaluateDeadline(deadline: Instant?): DeadlineInfo = checkDeadline(deadline)

        /**
         * Move a tarefa [taskId] para [target], respeitando a matriz
         * de transições.
         *
         * RN01 (DEF-20) — reabrir (`DONE ->` ativo) num projeto no
         * teto de [MAX_ACTIVE_TASKS_PER_PROJECT] é rejeitado pelo
         * [ChangeTaskStatusUseCase] com
         * [ProjectTaskLimitReachedException]. Aqui ela vira a mesma
         * mensagem do caminho de criação (`addTask`), e não o
         * fallback genérico de [toProjectDetailErrorMessage], que
         * descreve falha de **leitura** e seria mentira numa
         * rejeição de regra de negócio.
         */
        fun changeStatus(
            taskId: String,
            target: TaskStatus,
        ) {
            Log.d(TAG, "changeStatus: taskId=$taskId, target=$target")
            viewModelScope.launch {
                try {
                    changeStatus.invoke(taskId, target)
                } catch (e: pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidStateTransitionException) {
                    Log.w(TAG, "Transição de status inválida para $taskId: ${e.message}")
                    _errorMessage.update { ProjectDetailError.InvalidTransition }
                } catch (e: ProjectTaskLimitReachedException) {
                    Log.w(TAG, "RN01: reabertura no teto de tarefas ativas para taskId=$taskId")
                    _errorMessage.update { ProjectDetailError.TaskLimitReached(e.limit) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Log.e(TAG, "Erro ao alterar status da tarefa $taskId: ${e.message}", e)
                    _errorMessage.update { ProjectDetailError.ActionFailed }
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
        fun changeTaskPriority(
            task: Task,
            newPriority: TaskPriority,
            onSuccess: () -> Unit = {},
        ) {
            Log.d(TAG, "changeTaskPriority: taskId=${task.id}, newPriority=$newPriority")
            val updated =
                try {
                    task.changePriority(newPriority)
                } catch (e: TaskPriorityChangeForbiddenException) {
                    Log.w(TAG, "RN02: alteração de prioridade bloqueada para taskId=${task.id} (tarefa DONE)")
                    _errorMessage.update { ProjectDetailError.PriorityLocked }
                    return
                } catch (e: pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException) {
                    _errorMessage.update { ProjectDetailError.InvalidTitle }
                    return
                }
            viewModelScope.launch {
                try {
                    updateTask.invoke(updated)
                    onSuccess()
                } catch (e: TaskPriorityChangeForbiddenException) {
                    // Persistência indica DONE (bypass via copy).
                    Log.w(TAG, "RN02: alteração de prioridade bloqueada pela persistência para taskId=${task.id}")
                    _errorMessage.update { ProjectDetailError.PriorityLocked }
                } catch (e: pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException) {
                    _errorMessage.update { ProjectDetailError.InvalidTitle }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Log.e(TAG, "Erro ao alterar prioridade da tarefa ${task.id}: ${e.message}", e)
                    _errorMessage.update { ProjectDetailError.ActionFailed }
                }
            }
        }

        /** Renomeia a [task] via [Task.rename], que valida o título. */
        fun renameTask(
            task: Task,
            newTitle: String,
            onSuccess: () -> Unit = {},
        ) {
            val trimmed = newTitle.trim()
            if (trimmed.isEmpty()) {
                _errorMessage.update { ProjectDetailError.EmptyTitle }
                return
            }
            if (trimmed == task.title) {
                onSuccess()
                return
            }
            Log.d(TAG, "renameTask: taskId=${task.id}, newTitle=$trimmed")
            viewModelScope.launch {
                try {
                    updateTask.invoke(task.rename(trimmed))
                    onSuccess()
                } catch (e: pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException) {
                    _errorMessage.update { ProjectDetailError.InvalidTitle }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Log.e(TAG, "Erro ao renomear tarefa ${task.id}: ${e.message}", e)
                    _errorMessage.update { ProjectDetailError.ActionFailed }
                }
            }
        }

        /** Remove a [task] do projeto (ação do menu "Excluir" do item). */
        fun deleteTask(task: Task) {
            Log.d(TAG, "deleteTask: taskId=${task.id}")
            viewModelScope.launch {
                try {
                    deleteTask.invoke(task.id)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    _errorMessage.update { ProjectDetailError.ActionFailed }
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
                    _errorMessage.update { ProjectDetailError.ActionFailed }
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
            retryToken.value = retryToken.value + 1
        }

        companion object {
            private const val TAG = "BrainOut:ProjectDetailVM"

            /** Nome do argumento da rota do Navigation Compose. */
            const val PROJECT_ID_ARG: String = "projectId"
        }
    }
