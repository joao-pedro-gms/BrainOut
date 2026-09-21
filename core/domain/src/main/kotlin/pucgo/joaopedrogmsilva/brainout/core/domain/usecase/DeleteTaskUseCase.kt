// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Use case responsável por remover uma [Task] pelo id.

package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import javax.inject.Inject
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository

/**
 * Remove a [pucgo.joaopedrogmsilva.brainout.core.domain.model.Task]
 * identificada por [taskId] e cancela o lembrete de prazo associado
 * (marco E3.6). Cancelar após a remoção é sempre seguro — para o
 * scheduler é no-op quando não há agendamento pendente.
 */
class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val deadlineScheduler: DeadlineNotificationScheduler,
) {
    /** @param taskId Identificador da tarefa a remover. */
    suspend operator fun invoke(taskId: String) {
        repository.delete(taskId)
        deadlineScheduler.cancel(taskId)
    }
}
