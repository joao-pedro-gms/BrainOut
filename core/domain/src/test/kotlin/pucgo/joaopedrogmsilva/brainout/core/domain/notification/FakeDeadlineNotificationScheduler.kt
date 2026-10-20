// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.notification

import java.time.Instant

/**
 * [DeadlineNotificationScheduler] que registra as chamadas, para
 * verificar a reconciliação de lembretes da restauração de backup.
 */
internal class FakeDeadlineNotificationScheduler : DeadlineNotificationScheduler {
    /** Agendamentos registrados, na ordem da chamada. */
    val scheduled: MutableList<Pair<String, Instant>> = mutableListOf()

    /** Cancelamentos registrados, na ordem da chamada. */
    val cancelled: MutableList<String> = mutableListOf()

    override fun schedule(
        taskId: String,
        triggerAt: Instant,
    ) {
        scheduled.add(taskId to triggerAt)
    }

    override fun cancel(taskId: String) {
        cancelled.add(taskId)
    }
}
