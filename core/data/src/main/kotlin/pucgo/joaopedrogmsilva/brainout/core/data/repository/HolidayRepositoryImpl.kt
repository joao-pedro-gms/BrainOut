// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import javax.inject.Inject
import pucgo.joaopedrogmsilva.brainout.core.data.remote.HolidayRemoteDataSource
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Holiday
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.HolidayRepository

/**
 * Implementação padrão de
 * [pucgo.joaopedrogmsilva.brainout.core.domain.repository.HolidayRepository]
 * (E3.5) que delega para o
 * [pucgo.joaopedrogmsilva.brainout.core.data.remote.HolidayRemoteDataSource].
 *
 * Política de cache fica encapsulada no data source (em memória,
 * `Map<Int, List<Holiday>>`); aqui só propagamos o contrato.
 */
class HolidayRepositoryImpl @Inject constructor(
    private val remote: HolidayRemoteDataSource,
) : HolidayRepository {

    override suspend fun getHolidays(year: Int): List<Holiday> = remote.listHolidays(year)
}
