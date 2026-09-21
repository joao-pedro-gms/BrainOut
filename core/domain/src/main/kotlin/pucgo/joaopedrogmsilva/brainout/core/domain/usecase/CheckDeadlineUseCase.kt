// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Holiday
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.HolidayRepository

/**
 * Resultado da checagem de prazo (E3.5).
 *
 * @property isBusinessDay `true` se o prazo cai em dia útil
 *   (seg-sex) **e** não é feriado nacional.
 * @property nextHoliday Próximo feriado dentro de uma janela de
 *   [HOLIDAY_LOOKAHEAD_DAYS] dias *antes* do prazo, inclusive. `null`
 *   quando nenhum feriado se enquadra na janela — é o caso comum e
 *   indica "nada para avisar".
 */
data class DeadlineInfo(
    val isBusinessDay: Boolean,
    val nextHoliday: Holiday?,
)

/**
 * Caso de uso que avalia um prazo de [pucgo.joaopedrogmsilva.brainout.core.domain.model.Task]
 * sob a ótica de dias úteis e feriados nacionais (E3.5).
 *
 * Política (definida no ROADMAP E3.5):
 * - **isBusinessDay** = prazo não cai em sábado/domingo nem em feriado.
 * - **nextHoliday** = o feriado mais próximo dentro dos 7 dias
 *   anteriores ao prazo (inclusive o próprio dia, se for feriado).
 *   Quando não há feriado na janela, retorna `null` (sem aviso).
 *
 * A camada de UI consome este caso de uso no
 * [pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail.ProjectDetailScreen]
 * para mostrar uma dica inline "⚠ Próximo feriado: …" no diálogo
 * de criação/edição de tarefa com prazo.
 *
 * Sem persistência: cada chamada busca o ano corrente do prazo via
 * [HolidayRepository] (com cache em memória na implementação).
 */
class CheckDeadlineUseCase @Inject constructor(
    private val holidayRepository: HolidayRepository,
) {
    /**
     * Avalia o [deadline] informado.
     *
     * @param deadline Instante do prazo (UTC, conforme [Task]). Quando
     *   `null`, devolve [DeadlineInfo] "neutro" (`isBusinessDay = true`,
     *   `nextHoliday = null`) para que a UI simplesmente oculte a dica.
     * @param zone Fuso usado para mapear o instante para uma data civil.
     *   Padrão: [ZoneId.systemDefault] — o usuário cria a tarefa na
     *   sua zona local.
     */
    suspend operator fun invoke(
        deadline: Instant?,
        zone: ZoneId = ZoneId.systemDefault(),
    ): DeadlineInfo {
        if (deadline == null) {
            return DeadlineInfo(isBusinessDay = true, nextHoliday = null)
        }
        val deadlineDate: LocalDate = deadline.atZone(zone).toLocalDate()
        val year = deadlineDate.year
        val holidays: List<Holiday> = runCatching { holidayRepository.getHolidays(year) }
            .getOrElse { emptyList() }

        val isBusinessDay = deadlineDate.isBusinessDay(holidays)
        val nextHoliday = holidays
            .filter { it.date in deadlineDate.minusDays(HOLIDAY_LOOKAHEAD_DAYS)..deadlineDate }
            .maxByOrNull { it.date }

        return DeadlineInfo(isBusinessDay = isBusinessDay, nextHoliday = nextHoliday)
    }

    private fun LocalDate.isBusinessDay(holidays: List<Holiday>): Boolean {
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) return false
        return holidays.none { it.date == this }
    }

    companion object {
        /** Janela (em dias) para procurar o "próximo feriado antes do prazo". */
        const val HOLIDAY_LOOKAHEAD_DAYS: Long = 7L
    }
}
