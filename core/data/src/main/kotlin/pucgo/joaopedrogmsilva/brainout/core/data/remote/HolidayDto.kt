// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO de feriado nacional exposto pelo serviço público BrasilAPI
 * (`GET https://brasilapi.com.br/api/feriados/v1/{year}`).
 *
 * Mantido no pacote `remote/` porque o contrato é externo e fora do
 * nosso controle — qualquer mudança na BrasilAPI vira diff local
 * rastreável (sem precisar mexer no modelo de domínio
 * [pucgo.joaopedrogmsilva.brainout.core.domain.model.Holiday]).
 *
 * Campos:
 * - [date] ISO-8601 curto (`YYYY-MM-DD`).
 * - [name] Nome do feriado (ex.: "Natal").
 * - [type] Categoria — hoje sempre `"national"`, mantido aberto.
 * - [weekday] Dia da semana em pt-BR (ex.: "sexta-feira"); ignorado
 *   pelo domínio mas tolerado via `ignoreUnknownKeys`/defaults.
 */
@Serializable
data class HolidayDto(
    @SerialName("date")
    val date: String,
    @SerialName("name")
    val name: String,
    @SerialName("type")
    val type: String = "national",
    @SerialName("weekday")
    val weekday: String? = null,
)
