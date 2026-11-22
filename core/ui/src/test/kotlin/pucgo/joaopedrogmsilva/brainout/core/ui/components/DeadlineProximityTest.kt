// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Semáforo de proximidade do `NeoDeadlineChip`.
//
// `deadlineProximity` compara dias civis locais (start-of-day), não
// horas: a tarefa vence "hoje" se o dia dela for hoje, e a janela de 7
// dias usa `Calendar.add` para ser DST-corrente. Estes testes fixam os
// limites — atrasado, hoje, amanhã, o sétimo dia, o oitavo — e a
// travessia de ano.

package pucgo.joaopedrogmsilva.brainout.core.ui.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar

class DeadlineProximityTest {
    private fun millis(
        year: Int,
        month: Int,
        day: Int,
    ): Long =
        Calendar
            .getInstance()
            .apply {
                set(year, month, day, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

    @Test
    fun `dia anterior ao de hoje e atrasado`() {
        val now = millis(2026, Calendar.NOVEMBER, 22)
        val due = millis(2026, Calendar.NOVEMBER, 21)

        assertThat(deadlineProximity(due, now)).isEqualTo(NeoDeadlineProximity.OVERDUE)
    }

    @Test
    fun `mesmo dia civil e hoje independente da hora`() {
        val now =
            Calendar
                .getInstance()
                .apply { set(2026, Calendar.NOVEMBER, 22, 23, 59, 59) }
                .timeInMillis
        val due =
            Calendar
                .getInstance()
                .apply { set(2026, Calendar.NOVEMBER, 22, 0, 0, 0) }
                .timeInMillis

        assertThat(deadlineProximity(due, now)).isEqualTo(NeoDeadlineProximity.TODAY)
    }

    @Test
    fun `amanha cai na semana`() {
        val now = millis(2026, Calendar.NOVEMBER, 22)
        val due = millis(2026, Calendar.NOVEMBER, 23)

        assertThat(deadlineProximity(due, now)).isEqualTo(NeoDeadlineProximity.THIS_WEEK)
    }

    @Test
    fun `setimo dia ainda e esta semana`() {
        val now = millis(2026, Calendar.NOVEMBER, 22)
        val due = millis(2026, Calendar.NOVEMBER, 29)

        assertThat(deadlineProximity(due, now)).isEqualTo(NeoDeadlineProximity.THIS_WEEK)
    }

    @Test
    fun `oitavo dia ja e depois`() {
        val now = millis(2026, Calendar.NOVEMBER, 22)
        val due = millis(2026, Calendar.NOVEMBER, 30)

        assertThat(deadlineProximity(due, now)).isEqualTo(NeoDeadlineProximity.LATER)
    }

    @Test
    fun `travessia de ano nao confunde a comparacao`() {
        val now = millis(2026, Calendar.DECEMBER, 30)
        val dueThisWeek = millis(2027, Calendar.JANUARY, 2)
        val dueLater = millis(2027, Calendar.JANUARY, 15)

        assertThat(deadlineProximity(dueThisWeek, now)).isEqualTo(NeoDeadlineProximity.THIS_WEEK)
        assertThat(deadlineProximity(dueLater, now)).isEqualTo(NeoDeadlineProximity.LATER)
    }

    @Test
    fun `dia anterior em janeiro ao comparar com ano novo e atrasado`() {
        val now = millis(2027, Calendar.JANUARY, 1)
        val due = millis(2026, Calendar.DECEMBER, 31)

        assertThat(deadlineProximity(due, now)).isEqualTo(NeoDeadlineProximity.OVERDUE)
    }
}
