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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pucgo.joaopedrogmsilva.brainout.core.domain.error.ProjectTaskLimitReachedException
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

    val uiState: StateFlow<ProjectDetailUiState> = taskRepository
        .observeForProject(projectId)
        .map { tasks ->
            ProjectDetailUiState(
                projectId = projectId,
                tasks = tasks,
                isLoading = false,
                errorMessage = _errorMessage.value,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = ProjectDetailUiState(projectId = projectId),
        )

    /**
     * Cria uma nova tarefa no projeto corrente. Aplica a RN01
     * (limite de [MAX_ACTIVE_TASKS_PER_PROJECT] tarefas ativas)
     * através do [CreateTaskUseCase] — se a regra for violada,
     * uma mensagem amigável é exposta via [errorMessage] para que
     * a UI mostre o chip de erro e chame [clearError] após 5s.
     */
    fun addTask(title: String, priority: TaskPriority = TaskPriority.MEDIUM) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            _errorMessage.update { "Título da tarefa não pode ser vazio" }
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
                val message = e.message ?: ERROR_TASK_LIMIT
                _errorMessage.update { message }
            } catch (e: pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException) {
                _errorMessage.update { e.message ?: ERROR_INVALID_TITLE }
            }
        }
    }

    /** Move a [task] para [target], respeitando a matriz de transições. */
    fun changeStatus(taskId: String, target: TaskStatus) {
        viewModelScope.launch {
            try {
                changeStatus.invoke(taskId, target)
            } catch (e: pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidStateTransitionException) {
                _errorMessage.update { e.message ?: ERROR_INVALID_TRANSITION }
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
            }
        }
    }

    /** Remove a [task] do projeto (ação do menu "Excluir" do item). */
    fun deleteTask(task: Task) {
        viewModelScope.launch { deleteTask.invoke(task.id) }
    }

    /**
     * Exclui o projeto corrente e, em seguida, chama [onDone] para
     * que a camada de navegação faça `popBackStack()`.
     */
    fun deleteProject(onDone: () -> Unit) {
        viewModelScope.launch {
            deleteProject.invoke(projectId)
            onDone()
        }
    }

    /** Limpa a mensagem de erro atual — usado pela UI após o chip expirar. */
    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        /** Nome do argumento da rota do Navigation Compose. */
        const val PROJECT_ID_ARG: String = "projectId"

        private const val ERROR_TASK_LIMIT: String =
            "Erro: limite de $MAX_ACTIVE_TASKS_PER_PROJECT tarefas atingido"
        private const val ERROR_INVALID_TITLE: String = "Título inválido"
        private const val ERROR_INVALID_TRANSITION: String = "Transição de status inválida"
    }
}
