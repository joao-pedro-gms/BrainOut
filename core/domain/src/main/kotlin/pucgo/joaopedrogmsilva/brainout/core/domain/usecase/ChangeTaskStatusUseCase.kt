// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import javax.inject.Inject
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository

/**
 * Caso de uso responsável por alterar o [Task.status] de uma [Task].
 *
 * A matriz de transições válidas é aplicada pelo domínio via
 * [Task.transitionTo]; o repositório apenas materializa a mudança.
 * Lança [pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidStateTransitionException]
 * quando a transição pedida não é permitida (ex.: DONE → TODO).
 */
class ChangeTaskStatusUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    /**
     * @param taskId Identificador da tarefa.
     * @param target Estado desejado.
     * @return A [Task] atualizada.
     */
    suspend operator fun invoke(taskId: String, target: TaskStatus): Task =
        repository.changeStatus(taskId, target)
}
