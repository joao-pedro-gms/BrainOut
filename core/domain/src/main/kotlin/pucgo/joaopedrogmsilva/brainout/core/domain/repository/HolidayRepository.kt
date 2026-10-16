// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.repository

import pucgo.joaopedrogmsilva.brainout.core.domain.model.Holiday

/** Porta de consulta anual ao calendário local de feriados nacionais. */
interface HolidayRepository {
    /** Consulta local determinística, sem I/O de rede. */
    suspend fun getHolidays(year: Int): List<Holiday>
}
