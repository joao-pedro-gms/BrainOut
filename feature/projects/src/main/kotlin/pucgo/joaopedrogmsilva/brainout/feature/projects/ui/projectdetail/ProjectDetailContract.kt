// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus

/**
 * Estado consumido pelo corpo do detalhe de projeto.
 *
 * @property tasks tarefas do projeto.
 * @property isLoading a primeira coleta do Room ainda não aconteceu.
 * @property errorMessage erro de carga ou de ação, quando houver.
 */
internal data class ProjectDetailState(
    val tasks: List<Task>,
    val isLoading: Boolean,
    val errorMessage: String?,
)

/**
 * Ações disparadas pelo corpo do detalhe de projeto.
 *
 * As mesmas ações valem para todas as linhas da lista, então travels
 * uma única instância — a assinatura de cada linha cai de sete
 * parâmetros para dois.
 */
internal data class ProjectDetailActions(
    val onRetry: () -> Unit,
    val onDismissError: () -> Unit,
    val onChangeStatus: (taskId: String, target: TaskStatus) -> Unit,
    val onDeleteTask: (Task) -> Unit,
    /** Abre o diálogo de renomear; o `ViewModel` recebe o título novo. */
    val onRenameTask: (Task) -> Unit,
    val onChangePriority: (Task, TaskPriority) -> Unit,
)
