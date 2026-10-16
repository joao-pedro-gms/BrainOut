// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import pucgo.joaopedrogmsilva.brainout.core.domain.model.Holiday
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.HolidayRepository
import java.time.LocalDate
import javax.inject.Inject

/** Calendário nacional local pelas regras atuais; não inclui pontos facultativos ou feriados regionais. */
class HolidayRepositoryImpl
    @Inject
    constructor() : HolidayRepository {
        @Suppress("MagicNumber")
        override suspend fun getHolidays(year: Int): List<Holiday> {
            require(year in 1583..9999) { "Ano fora do calendário gregoriano suportado" }
            val fixed =
                listOf(
                    holiday(year, 1, 1, "Confraternização Universal"),
                    holiday(year, 4, 21, "Tiradentes"),
                    holiday(year, 5, 1, "Dia do Trabalho"),
                    holiday(year, 9, 7, "Independência do Brasil"),
                    holiday(year, 10, 12, "Nossa Senhora Aparecida"),
                    holiday(year, 11, 2, "Finados"),
                    holiday(year, 11, 15, "Proclamação da República"),
                    holiday(year, 12, 25, "Natal"),
                )
            val goodFriday = Holiday(easter(year).minusDays(2), "Paixão de Cristo", "national")
            val consciousnessDay =
                if (year >=
                    2024
                ) {
                    listOf(holiday(year, 11, 20, "Dia Nacional de Zumbi e da Consciência Negra"))
                } else {
                    emptyList()
                }
            return (fixed + goodFriday + consciousnessDay).sortedBy { it.date }
        }

        private fun holiday(
            year: Int,
            month: Int,
            day: Int,
            name: String,
        ): Holiday = Holiday(LocalDate.of(year, month, day), name, "national")

        /** Computus gregoriano (Meeus/Jones/Butcher), independente de rede ou fuso horário. */
        @Suppress("MagicNumber")
        private fun easter(year: Int): LocalDate {
            val a = year % 19
            val b = year / 100
            val c = year % 100
            val d = b / 4
            val e = b % 4
            val f = (b + 8) / 25
            val g = (b - f + 1) / 3
            val h = (19 * a + b - d - g + 15) % 30
            val i = c / 4
            val k = c % 4
            val l = (32 + 2 * e + 2 * i - h - k) % 7
            val m = (a + 11 * h + 22 * l) / 451
            val n = h + l - 7 * m + 114
            return LocalDate.of(year, n / 31, n % 31 + 1)
        }
    }
