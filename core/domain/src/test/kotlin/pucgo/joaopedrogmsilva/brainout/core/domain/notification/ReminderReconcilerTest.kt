// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.notification

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import java.time.Instant

class ReminderReconcilerTest {
    private val now: Instant = Instant.parse("2026-11-18T12:00:00Z")

    @Test
    fun `agenda tarefa ativa quando o disparo esta depois do instante atual`() {
        val scheduler = FakeDeadlineNotificationScheduler()
        val triggerAt = now.plusSeconds(1)
        val dueDate = triggerAt.plus(DeadlineNotificationScheduler.REMINDER_LEAD)

        scheduler.reconcileTaskReminder("task-1", TaskStatus.TODO, dueDate, now)

        assertThat(scheduler.scheduled).containsExactly("task-1" to triggerAt)
        assertThat(scheduler.cancelled).isEmpty()
    }

    @Test
    fun `cancela quando o instante de disparo coincide com agora`() {
        val scheduler = FakeDeadlineNotificationScheduler()
        val dueDate = now.plus(DeadlineNotificationScheduler.REMINDER_LEAD)

        scheduler.reconcileTaskReminder("task-1", TaskStatus.DOING, dueDate, now)

        assertThat(scheduler.scheduled).isEmpty()
        assertThat(scheduler.cancelled).containsExactly("task-1")
    }

    @Test
    fun `cancela quando o instante de disparo ja passou`() {
        val scheduler = FakeDeadlineNotificationScheduler()
        val dueDate = now.minusSeconds(1).plus(DeadlineNotificationScheduler.REMINDER_LEAD)

        scheduler.reconcileTaskReminder("task-1", TaskStatus.TODO, dueDate, now)

        assertThat(scheduler.scheduled).isEmpty()
        assertThat(scheduler.cancelled).containsExactly("task-1")
    }

    @Test
    fun `cancela tarefa concluida mesmo com disparo futuro`() {
        val scheduler = FakeDeadlineNotificationScheduler()
        val dueDate = now.plus(DeadlineNotificationScheduler.REMINDER_LEAD).plusSeconds(60)

        scheduler.reconcileTaskReminder("task-1", TaskStatus.DONE, dueDate, now)

        assertThat(scheduler.scheduled).isEmpty()
        assertThat(scheduler.cancelled).containsExactly("task-1")
    }

    @Test
    fun `cancela tarefa ativa sem prazo`() {
        val scheduler = FakeDeadlineNotificationScheduler()

        scheduler.reconcileTaskReminder("task-1", TaskStatus.DOING, null, now)

        assertThat(scheduler.scheduled).isEmpty()
        assertThat(scheduler.cancelled).containsExactly("task-1")
    }
}
