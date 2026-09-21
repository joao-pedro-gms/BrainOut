// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.notification

import java.time.Duration
import java.time.Instant

/**
 * Porta de agendamento de lembretes de prazo (marco E3.6 do ROADMAP).
 *
 * O domínio apenas declara **o que** precisa acontecer — agendar ou
 * cancelar um lembrete para uma [pucgo.joaopedrogmsilva.brainout.core.domain.model.Task]
 * — sem conhecer a tecnologia de agendamento (WorkManager,
 * AlarmManager, etc.). A implementação concreta vive em `:app`
 * (`WorkManagerDeadlineScheduler`) e é ligada pelo Hilt, mantendo
 * `:core:domain` livre de Android (regra R12).
 *
 * Contrato:
 * - [schedule] registra (ou re-registra, substituindo o agendamento
 *   anterior) o lembrete da tarefa para disparar em [triggerAt].
 *   Implementações devem ignorar (ou cancelar) agendamentos no
 *   passado — prazo já vencido não dispara lembrete retroativo.
 * - [cancel] remove qualquer agendamento pendente da tarefa; chamar
 *   para uma tarefa sem agendamento é no-op.
 *
 * O tempo de antecedência do lembrete é [REMINDER_LEAD]: as camadas
 * acima calculam `dueDate - REMINDER_LEAD` e passam o resultado em
 * [triggerAt].
 */
interface DeadlineNotificationScheduler {

    /**
     * Agenda (ou reagenda, com política de substituição) o lembrete
     * da tarefa [taskId] para disparar em [triggerAt]. Chamadas
     * repetidas para o mesmo `taskId` substituem o agendamento
     * anterior — a última escrita vence.
     */
    fun schedule(taskId: String, triggerAt: Instant)

    /** Cancela o lembrete pendente de [taskId]. No-op se não houver. */
    fun cancel(taskId: String)

    companion object {

        /**
         * Antecedência do lembrete: notificar 1 hora antes do prazo
         * (requisito E3.6 — texto da notificação "prazo em 1 hora").
         */
        val REMINDER_LEAD: Duration = Duration.ofHours(1)
    }
}
