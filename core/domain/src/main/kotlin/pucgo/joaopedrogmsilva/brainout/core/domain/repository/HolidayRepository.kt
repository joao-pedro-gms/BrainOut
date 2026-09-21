// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.repository

import pucgo.joaopedrogmsilva.brainout.core.domain.model.Holiday

/** Porta de consulta anual; implementação usa cache em memória, sem persistência. */
interface HolidayRepository {
    /** Lista vazia em 200 sem itens ou 404; demais falhas e cancelamento são propagados. */
    suspend fun getHolidays(year: Int): List<Holiday>
}
