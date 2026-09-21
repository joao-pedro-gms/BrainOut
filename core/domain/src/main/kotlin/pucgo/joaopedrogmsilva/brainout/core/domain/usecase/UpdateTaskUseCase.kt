// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import java.time.Instant
import javax.inject.Inject
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TaskPriorityChangeForbiddenException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository

/**
 * Caso de uso responsável por atualizar uma [Task] existente.
 *
 * Validação contra o registro **persistido** (RN02 — E2.4):
 * antes de aplicar a atualização, o caso de uso recarrega o estado
 * atual do banco. Se a tarefa persistida estiver em
 * [TaskStatus.DONE], a operação é rejeitada com
 * [TaskPriorityChangeForbiddenException], impedindo bypass via
 * `copy`/objeto velho (race com outra escrita). Esse controle fica
 * aqui — e não no domínio — porque a regra é "regra de banco",
 * não regra de modelo.
 *
 * Caso o chamador já tenha aplicado as invariantes do domínio via
 * [Task.rename], [Task.changePriority], [Task.reassign],
 * [Task.changeDueDate] e deseje confiar nelas, ainda assim o
 * carregamento aqui é necessário para garantir atomicidade com o
 * estado em disco.
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
        // RN02 — defesa contra bypass via `copy`/objeto velho: se o
        // estado persistido difere do input, recarregamos do banco e
        // aplicamos RN02 contra o registro vigente. Se o registro
        // vigente está em DONE, rejeitamos mesmo que o input tenha
        // status ativo (ou tenha sido `copy` para uma prioridade
        // diferente).
        val persisted = repository.findById(task.id)
        if (persisted != null && persisted.status == TaskStatus.DONE) {
            throw TaskPriorityChangeForbiddenException(task.id)
        }
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
