// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Holiday
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.HolidayRepository

/**
 * Testes unitários do [CheckDeadlineUseCase] (E3.5).
 *
 * Cobertura exigida pelo ROADMAP:
 * - prazo em feriado → `isBusinessDay = false`, `nextHoliday != null`.
 * - prazo com feriado nos 7 dias anteriores → `nextHoliday != null`.
 * - prazo livre → `nextHoliday == null`, `isBusinessDay = true`.
 *
 * Cenários extras cobertos:
 * - prazo em fim de semana → `isBusinessDay = false`.
 * - `deadline == null` → resultado neutro.
 * - erro no repositório → degrada para `holidays = emptyList()`
 *   (a UI não quebra; a dica simplesmente não aparece).
 */
class CheckDeadlineUseCaseTest {

    private val holidays2026: List<Holiday> = listOf(
        Holiday(LocalDate.parse("2026-01-01"), "Confraternização mundial", "national"),
        Holiday(LocalDate.parse("2026-04-21"), "Tiradentes", "national"),
        Holiday(LocalDate.parse("2026-05-01"), "Dia do trabalho", "national"),
        Holiday(LocalDate.parse("2026-09-07"), "Independência", "national"),
        Holiday(LocalDate.parse("2026-12-25"), "Natal", "national"),
    )

    /**
     * 2026-05-01 cai numa sexta-feira — bom para os cenários abaixo.
     * 2026-12-25 também numa sexta-feira.
     */
    private val utc: ZoneId = ZoneOffset.UTC

    @Test
    fun `prazo em feriado retorna isBusinessDay false e o proprio feriado`() = runTest {
        val repository = mockk<HolidayRepository>()
        coEvery { repository.getHolidays(2026) } returns holidays2026
        val useCase = CheckDeadlineUseCase(repository)

        val deadline = LocalDate.parse("2026-12-25").atStartOfDay(utc).toInstant()
        val info = useCase(deadline, utc)

        assertThat(info.isBusinessDay).isFalse()
        assertThat(info.nextHoliday).isNotNull()
        assertThat(info.nextHoliday?.name).isEqualTo("Natal")
        assertThat(info.nextHoliday?.date).isEqualTo(LocalDate.parse("2026-12-25"))
    }

    @Test
    fun `prazo com feriado nos 7 dias anteriores retorna o feriado mais proximo`() = runTest {
        val repository = mockk<HolidayRepository>()
        coEvery { repository.getHolidays(2026) } returns holidays2026
        val useCase = CheckDeadlineUseCase(repository)

        // 2026-05-08 (sexta) — Tiradentes (21/04) está a 17 dias;
        // Dia do trabalho (01/05) está a 7 dias atrás → ainda dentro da janela.
        val deadline = LocalDate.parse("2026-05-08").atStartOfDay(utc).toInstant()
        val info = useCase(deadline, utc)

        assertThat(info.isBusinessDay).isTrue()
        assertThat(info.nextHoliday).isNotNull()
        assertThat(info.nextHoliday?.name).isEqualTo("Dia do trabalho")
        assertThat(info.nextHoliday?.date).isEqualTo(LocalDate.parse("2026-05-01"))
    }

    @Test
    fun `prazo sem feriado nos 7 dias anteriores retorna nextHoliday null`() = runTest {
        val repository = mockk<HolidayRepository>()
        coEvery { repository.getHolidays(2026) } returns holidays2026
        val useCase = CheckDeadlineUseCase(repository)

        // 2026-06-15 (segunda) — feriado mais próximo anterior é 01/05 (45d);
        // próximo posterior é 07/09 (84d). Janela de 7 dias não cobre nenhum.
        val deadline = LocalDate.parse("2026-06-15").atStartOfDay(utc).toInstant()
        val info = useCase(deadline, utc)

        assertThat(info.isBusinessDay).isTrue()
        assertThat(info.nextHoliday).isNull()
    }

    @Test
    fun `prazo em fim de semana retorna isBusinessDay false`() = runTest {
        val repository = mockk<HolidayRepository>()
        coEvery { repository.getHolidays(2026) } returns emptyList()
        val useCase = CheckDeadlineUseCase(repository)

        // 2026-06-13 é sábado (sem feriado próximo).
        val deadline = LocalDate.parse("2026-06-13").atStartOfDay(utc).toInstant()
        val info = useCase(deadline, utc)

        assertThat(info.isBusinessDay).isFalse()
        assertThat(info.nextHoliday).isNull()
    }

    @Test
    fun `deadline null retorna resultado neutro`() = runTest {
        val repository = mockk<HolidayRepository>()
        coEvery { repository.getHolidays(any()) } returns holidays2026
        val useCase = CheckDeadlineUseCase(repository)

        val info = useCase(deadline = null, zone = utc)

        assertThat(info.isBusinessDay).isTrue()
        assertThat(info.nextHoliday).isNull()
    }

    @Test
    fun `erro no repositorio propaga sem afirmar dia util`() = runTest {
        val repository = mockk<HolidayRepository>()
        val error = java.io.IOException("offline")
        coEvery { repository.getHolidays(2026) } throws error
        val deadline = LocalDate.parse("2026-06-15").atStartOfDay(utc).toInstant()
        assertThat(runCatching { CheckDeadlineUseCase(repository)(deadline, utc) }.exceptionOrNull())
            .isSameInstanceAs(error)
    }

    @Test
    fun `prazo 8 dias apos feriado mais recente fica fora da janela`() = runTest {
        val repository = mockk<HolidayRepository>()
        coEvery { repository.getHolidays(2026) } returns holidays2026
        val useCase = CheckDeadlineUseCase(repository)

        // 2026-05-11 (segunda) — Dia do trabalho foi em 01/05 (10 dias atrás)
        // → fora da janela de 7. Também não é fim de semana.
        val deadline = LocalDate.parse("2026-05-11").atStartOfDay(utc).toInstant()
        val info = useCase(deadline, utc)

        assertThat(info.isBusinessDay).isTrue()
        assertThat(info.nextHoliday).isNull()
    }

    @Test
    fun `prazo com feriado na mesma data prioriza o proprio feriado`() = runTest {
        val repository = mockk<HolidayRepository>()
        coEvery { repository.getHolidays(2026) } returns holidays2026
        val useCase = CheckDeadlineUseCase(repository)

        // 2026-05-01 — Dia do trabalho (mesma data) + 30/04 não é feriado.
        val deadline = LocalDate.parse("2026-05-01").atStartOfDay(utc).toInstant()
        val info = useCase(deadline, utc)

        assertThat(info.isBusinessDay).isFalse()
        assertThat(info.nextHoliday?.name).isEqualTo("Dia do trabalho")
    }

    @Test
    fun `instante deadline com hora eh normalizado para o dia civil da zona`() = runTest {
        val repository = mockk<HolidayRepository>()
        coEvery { repository.getHolidays(2026) } returns holidays2026
        val useCase = CheckDeadlineUseCase(repository)

        // 2026-12-25 23:30 UTC → continua sendo Natal na zona UTC.
        val deadline: Instant = LocalDate.parse("2026-12-25")
            .atTime(23, 30)
            .atZone(utc)
            .toInstant()
        val info = useCase(deadline, utc)

        assertThat(info.isBusinessDay).isFalse()
        assertThat(info.nextHoliday?.name).isEqualTo("Natal")
    }
}
