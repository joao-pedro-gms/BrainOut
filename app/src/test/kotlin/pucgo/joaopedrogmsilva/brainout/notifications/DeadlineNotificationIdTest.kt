// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.notifications

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Testes da identidade da notificação de lembrete (DEF-54).
 *
 * O id numérico da notificação vem de `hashCode()`, que tem 32 bits e
 * **colide**. O par de UUIDs abaixo é real — os dois têm o mesmo
 * `hashCode()` — e é o que fez a notificação de uma tarefa substituir
 * a de outra na bandeja.
 */
class DeadlineNotificationIdTest {
    /**
     * Par real de UUIDs com o mesmo `hashCode()` (encontrado por força
     * bruta sobre UUIDs v4).
     */
    private val first = "c3f9f00e-1c18-433f-8115-b38ff7434044"

    /** Ver [first] — mesmo `hashCode()`, tarefa diferente. */
    private val second = "1ed5817b-4697-49c7-b1d8-8a62c94fce50"

    @Test
    fun `a colisao do hashCode e real`() {
        assertThat(first.hashCode()).isEqualTo(second.hashCode())
    }

    @Test
    fun `duas tarefas com o mesmo hashCode nao compartilham a identidade da notificacao`() {
        // O id numérico sozinho não distingue as duas — é por isso que
        // ele não pode ser a identidade.
        assertThat(DeadlineNotificationId.of(first)).isEqualTo(DeadlineNotificationId.of(second))

        // Quem distingue é a tag: o id da tarefa, que é único.
        assertThat(DeadlineNotificationId.tagOf(first)).isNotEqualTo(DeadlineNotificationId.tagOf(second))
    }

    @Test
    fun `id numerico nunca e negativo`() {
        // "polygenelubricants" é a string clássica de hash negativo
        // (Integer.MIN_VALUE) — o caso real levantado pela auditoria
        // de 24/09/2026.
        assertThat("polygenelubricants".hashCode()).isLessThan(0)

        assertThat(DeadlineNotificationId.of("polygenelubricants")).isAtLeast(0)
    }

    @Test
    fun `a mesma tarefa produz sempre a mesma identidade`() {
        assertThat(DeadlineNotificationId.of(first)).isEqualTo(DeadlineNotificationId.of(first))
        assertThat(DeadlineNotificationId.tagOf(first)).isEqualTo(DeadlineNotificationId.tagOf(first))
    }
}
