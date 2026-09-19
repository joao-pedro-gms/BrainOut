// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// ViewModel da [TasksScreen] — observador reativo de TODAS as tarefas
// do owner ativo, com join do nome do projeto correspondente para
// exibição direta na linha.
//
// Marco E2.6 do ROADMAP.md — corresponde à aba "Tarefas" da HomeScreen,
// que antes mostrava um placeholder estático.

package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
 * @property rows Linhas a renderizar (uma por tarefa do owner).
 * @property isLoading `true` enquanto a cadeia `flatMapLatest` ainda
 *  não emitiu a primeira lista — útil para a UI decidir entre o
 *  spinner e a lista vazia sem ter que conhecer a estrutura interna
 *  do `StateFlow`.
 */
data class TasksUiState(
    val rows: List<TaskRow> = emptyList(),
    val isLoading: Boolean = true,
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
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModel @Inject constructor(
    @Suppress("unused") private val taskRepository: TaskRepository,
    @Suppress("unused") private val projectRepository: ProjectRepository,
    private val activeUserProvider: ActiveUserProvider,
) : ViewModel() {

    val uiState: StateFlow<TasksUiState> = activeUserProvider.observeActiveUserId()
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
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = TasksUiState(),
        )

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
    }
}
