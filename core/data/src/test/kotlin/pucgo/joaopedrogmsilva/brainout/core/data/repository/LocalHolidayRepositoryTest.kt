// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

class LocalHolidayRepositoryTest {
    private val repository = HolidayRepositoryImpl()

    @Test
    fun `calendario de 2026 contem dez feriados nacionais sem pontos facultativos`() =
        runTest {
            val dates = repository.getHolidays(2026).map { it.date.toString() }
            assertThat(dates)
                .containsExactly(
                    "2026-01-01",
                    "2026-04-03",
                    "2026-04-21",
                    "2026-05-01",
                    "2026-09-07",
                    "2026-10-12",
                    "2026-11-02",
                    "2026-11-15",
                    "2026-11-20",
                    "2026-12-25",
                ).inOrder()
        }

    @Test
    fun `paixao de cristo muda com a pascoa do ano consultado`() =
        runTest {
            assertThat(repository.getHolidays(2025).first { it.name == "Paixão de Cristo" }.date)
                .isEqualTo(LocalDate.parse("2025-04-18"))
            assertThat(repository.getHolidays(2027).first { it.name == "Paixão de Cristo" }.date)
                .isEqualTo(LocalDate.parse("2027-03-26"))
        }

    @Test
    fun `consultas de anos distintos nao compartilham datas de cache`() =
        runTest {
            repository.getHolidays(2026)
            assertThat(repository.getHolidays(2027).all { it.date.year == 2027 }).isTrue()
        }
}
