// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.notification

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.Test

/**
 * Testes do contrato [DeadlineNotificationScheduler] usado pelos
 * use cases de Task (marco E3.6).
 *
 * Verifica a constante [DeadlineNotificationScheduler.REMINDER_LEAD]:
 * o lembrete deve disparar 1 hora antes do prazo, conforme o texto
 * exigido na notificação ("prazo em 1 hora").
 */
class DeadlineNotificationSchedulerContractTest {

    private val scheduler: DeadlineNotificationScheduler = mockk(relaxed = true)

    @Test
    fun `REMINDER_LEAD é de exatamente 1 hora`() {
        assertThat(DeadlineNotificationScheduler.REMINDER_LEAD.toMillis())
            .isEqualTo(60L * 60L * 1_000L)
    }

    @Test
    fun `cancel é no-op via mockk relaxed`() {
        scheduler.cancel("task-inexistente")
        // relaxed mock aceita a chamada sem agendamento pendente.
    }
}
