// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.model

import java.time.LocalDate

/**
 * Feriado nacional (E3.5).
 *
 * Modelo de domínio consumido por [pucgo.joaopedrogmsilva.brainout.core.domain.repository.HolidayRepository]
 * e pelo caso de uso
 * [pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CheckDeadlineUseCase].
 *
 * O calendário local usa datas sem fuso para comparar com os prazos.
 *
 * @property date Data do feriado (sem fuso).
 * @property name Nome descritivo (ex.: "Natal").
 * @property type Categoria do calendário local ("national").
 */
data class Holiday(
    val date: LocalDate,
    val name: String,
    val type: String,
)
