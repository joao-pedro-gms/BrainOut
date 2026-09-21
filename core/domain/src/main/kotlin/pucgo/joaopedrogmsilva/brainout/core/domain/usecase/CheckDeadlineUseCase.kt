// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Holiday
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.HolidayRepository

/** Resultado conhecido; falhas de consulta são propagadas, não representam dia útil. */
data class DeadlineInfo(val isBusinessDay: Boolean, val nextHoliday: Holiday?)

/**
 * Consulta a janela [prazo - 7 dias, prazo], inclusive, no fuso informado.
 * nextHoliday é o feriado nacional mais próximo do prazo nessa janela.
 * Busca ambos os anos quando a janela cruza dezembro/janeiro.
 * Cancelamento e falhas são propagados; a UI trata indisponibilidade sem impedir salvar.
 */
class CheckDeadlineUseCase @Inject constructor(
    private val holidayRepository: HolidayRepository,
) {
    suspend operator fun invoke(
        deadline: Instant?,
        zone: ZoneId = ZoneId.systemDefault(),
    ): DeadlineInfo {
        if (deadline == null) return DeadlineInfo(isBusinessDay = true, nextHoliday = null)
        val date = deadline.atZone(zone).toLocalDate()
        val start = date.minusDays(HOLIDAY_LOOKAHEAD_DAYS)
        val holidays = (start.year..date.year)
            .flatMap { holidayRepository.getHolidays(it) }
            .filter { it.type == "national" }
        val businessDay = date.dayOfWeek != DayOfWeek.SATURDAY &&
            date.dayOfWeek != DayOfWeek.SUNDAY && holidays.none { it.date == date }
        val holiday = holidays.filter { it.date in start..date }.maxByOrNull { it.date }
        return DeadlineInfo(businessDay, holiday)
    }

    companion object {
        const val HOLIDAY_LOOKAHEAD_DAYS: Long = 7L
    }
}
