// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import java.time.Instant
import javax.inject.Inject
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository

/**
 * Caso de uso responsável por atualizar uma [Task] existente.
 *
 * O caller deve aplicar as invariantes do domínio via métodos
 * específicos ([Task.rename], [Task.changePriority], [Task.reassign],
 * [Task.changeDueDate]) antes de invocar este caso de uso — a
 * validação de invariantes já ocorreu nessa etapa.
 *
 * Sempre que o prazo ([Task.dueDate]) ou o status mudam, o lembrete
 * de prazo (marco E3.6) é reconciliado:
 * - Tarefa **DONE** → lembrete cancelado.
 * - Tarefa ativa com prazo futuro → (re)agendado para
 *   `dueDate - [DeadlineNotificationScheduler.REMINDER_LEAD]`.
 * - Tarefa ativa sem prazo ou com prazo no passado → sem lembrete
 *   (cancela qualquer agendamento remanescente).
 */
class UpdateTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val deadlineScheduler: DeadlineNotificationScheduler,
) {
    /** @param task Tarefa com os campos atualizados — `id` deve existir. */
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
        } else {
            deadlineScheduler.cancel(task.id)
        }
    }
}
