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
 * @property type Categoria do calendário local ([TYPE_NATIONAL]).
 */
data class Holiday(
    val date: LocalDate,
    val name: String,
    val type: String,
) {
    companion object {
        /**
         * Categoria do feriado nacional — a única que o calendário local
         * produz e a que o [pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CheckDeadlineUseCase]
         * filtra.
         *
         * A categoria é um discriminante **entre camadas**: `:core:data`
         * produz o feriado e o domínio o filtra. Se os dois lados
         * grafassem diferente, o filtro não casaria com nada e a dica
         * de próximo feriado sumiria **em silêncio** — e nenhum teste
         * pegaria, porque o teste do caso de uso monta o próprio feriado.
         */
        const val TYPE_NATIONAL: String = "national"
    }
}
