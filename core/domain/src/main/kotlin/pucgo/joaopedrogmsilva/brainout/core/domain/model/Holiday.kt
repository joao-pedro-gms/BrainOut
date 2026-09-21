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
 * O repositório remoto (BrasilAPI, sem autenticação) já entrega datas
 * ISO-8601 (`YYYY-MM-DD`); aqui representamos como [LocalDate] para
 * evitar aritmética confusa de fusos quando o caso de uso compara com
 * o prazo de uma [Task] (também um instante "absoluto" UTC).
 *
 * @property date Data do feriado (sem fuso).
 * @property name Nome descritivo (ex.: "Natal").
 * @property type Categoria reportada pelo serviço externo ("national" hoje;
 *   mantido aberto para futuras categorias como "estadual" / "municipal").
 */
data class Holiday(
    val date: LocalDate,
    val name: String,
    val type: String,
)
