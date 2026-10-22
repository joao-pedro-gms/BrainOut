// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.notification

import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import java.time.Instant

/**
 * Reconcilia o lembrete de prazo de uma tarefa (marco E3.6).
 *
 * Existe **uma** regra para decidir se uma tarefa deve ter lembrete
 * pendente:
 *
 * > O lembrete existe se, e somente se, a tarefa está ativa
 * > (`status != DONE`), tem prazo e o instante de disparo
 * > (`dueDate - [DeadlineNotificationScheduler.REMINDER_LEAD]`) ainda
 * > está no futuro.
 *
 * Todos os caminhos que mudam status ou prazo (criar, editar,
 * concluir, reabrir, restaurar backup) passam por esta função, de modo
 * que o estado do agendamento nunca diverge do estado da tarefa.
 *
 * @param taskId Identificador da tarefa.
 * @param status Status atual da tarefa.
 * @param dueDate Prazo atual da tarefa, ou `null` se não houver.
 */
fun DeadlineNotificationScheduler.reconcileTaskReminder(
    taskId: String,
    status: TaskStatus,
    dueDate: Instant?,
) {
    if (status == TaskStatus.DONE) {
        cancel(taskId)
        return
    }
    val triggerAt = dueDate?.minus(DeadlineNotificationScheduler.REMINDER_LEAD)
    if (triggerAt != null && triggerAt.isAfter(Instant.now())) {
        schedule(taskId, triggerAt)
    } else {
        // Sem prazo, ou prazo já vencido: não há lembrete a disparar —
        // e qualquer agendamento remanescente precisa sair do caminho.
        cancel(taskId)
    }
}
