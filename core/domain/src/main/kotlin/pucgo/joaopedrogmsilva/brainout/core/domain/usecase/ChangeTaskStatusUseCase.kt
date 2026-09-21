// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import java.time.Instant
import javax.inject.Inject
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository

/**
 * Caso de uso responsável por alterar o [Task.status] de uma [Task]
 * **e** reconciliar o lembrete de prazo associado (marco E3.6).
 *
 * A matriz de transições válidas é aplicada pelo domínio via
 * [Task.transitionTo]; o repositório apenas materializa a mudança.
 * Lança [pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidStateTransitionException]
 * quando a transição pedida não é permitida (ex.: DONE → TODO).
 *
 * Regra de agendamento do lembrete:
 * - Tarefa passa a **DONE** (ou permanece DONE) → lembrete cancelado.
 * - Tarefa volta a ficar ativa e possui [Task.dueDate] → lembrete
 *   (re)agendado para `dueDate - [DeadlineNotificationScheduler.REMINDER_LEAD]`.
 * - Prazo no passado ou sem prazo → sem agendamento (o scheduler
 *   descarta gatilhos no passado; aqui já não chamamos).
 */
class ChangeTaskStatusUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val deadlineScheduler: DeadlineNotificationScheduler,
) {
    /**
     * @param taskId Identificador da tarefa.
     * @param target Estado desejado.
     * @return A [Task] atualizada.
     */
    suspend operator fun invoke(taskId: String, target: TaskStatus): Task {
        val updated = repository.changeStatus(taskId, target)
        reconcileReminder(updated)
        return updated
    }

    /**
     * Atualiza a tarefa inteira (usado pelos fluxos de edição) e
     * reconcilia o lembrete de acordo com o novo status/prazo.
     */
    suspend operator fun invoke(task: Task): Task {
        val updated = repository.update(task)
        reconcileReminder(updated)
        return updated
    }

    private fun reconcileReminder(task: Task) {
        if (task.status == TaskStatus.DONE) {
            deadlineScheduler.cancel(task.id)
            return
        }
        val triggerAt = task.dueDate?.minus(DeadlineNotificationScheduler.REMINDER_LEAD)
        if (triggerAt != null && triggerAt.isAfter(Instant.now())) {
            deadlineScheduler.schedule(task.id, triggerAt)
        }
    }
}
