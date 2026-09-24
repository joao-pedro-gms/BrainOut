// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import java.time.Instant
import javax.inject.Inject
import pucgo.joaopedrogmsilva.brainout.core.domain.error.BusinessRuleException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TaskNotFoundException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository

/**
 * Caso de uso responsável por alterar o [Task.status] de uma [Task]
 * **e** reconciliar o lembrete de prazo associado (marco E3.6).
 *
 * RN03 (E2.5) — conclusão cascata: ao receber `target = DONE` o caso
 * de uso delega para [TaskRepository.completeAndCascade], que aplica
 * a transição E marca o projeto como concluído na mesma transação
 * Room quando a tarefa concluída é a última ativa. Para reabrir
 * (DONE → DOING ou DOING → TODO) delega para [TaskRepository.reopenAndCascade]
 * que desfaz a marca de conclusão do projeto caso ele estivesse
 * concluído.
 *
 * Transições intermediárias (TODO → DOING, DOING → TODO) usam
 * [TaskRepository.changeStatus] sem cascata porque não alteram a
 * cardinalidade de tarefas ativas.
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
        val updated = when (target) {
            // RN03 (E2.5): entrada em DONE é o único caminho que pode
            // alterar a contagem de tarefas ativas (decrementa) — passa
            // pela cascata para também marcar o projeto se for a
            // última ativa.
            TaskStatus.DONE -> repository.completeAndCascade(taskId)
            // Reabertura (saída de DONE) também muda a contagem e pode
            // precisar desmarcar o projeto: mesma via.
            else -> {
                val current = repository.findById(taskId) ?: throw TaskNotFoundException(taskId)
                if (current.status == TaskStatus.DONE) {
                    repository.reopenAndCascade(taskId, target)
                } else {
                    repository.changeStatus(taskId, target)
                }
            }
        }
        reconcileReminder(updated)
        return updated
    }

    /**
     * Atualiza a tarefa inteira (usado pelos fluxos de edição) e
     * reconcilia o lembrete de acordo com o novo status/prazo.
     *
     * RN02 (E2.4): se a chamada trouxer uma nova prioridade sobre
     * uma tarefa já concluída, [Task.changePriority] lança
     * [BusinessRuleException] (via
     * [pucgo.joaopedrogmsilva.brainout.core.domain.error.TaskPriorityChangeForbiddenException]).
     * O erro é propagado para a camada de UI sem qualquer suavização
     * aqui.
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
