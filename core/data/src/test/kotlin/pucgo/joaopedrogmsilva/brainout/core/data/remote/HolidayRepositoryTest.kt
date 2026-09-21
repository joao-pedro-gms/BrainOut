// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.remote

import com.google.common.truth.Truth.assertThat
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Holiday

/**
 * Testes unitários do [HolidayRemoteDataSource] (E3.5).
 *
 * Cobertura exigida pelo ROADMAP (E3.5):
 * - 200 com 2 feriados parseados.
 * - 200 lista vazia.
 * - 404 traduzido para lista vazia (ano sem cobertura).
 * - Timeout / IOException re-sinalizado para o chamador.
 *
 * Todos os cenários batem em um [MockWebServer] — sem rede real.
 */
class HolidayRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var remote: HolidayRemoteDataSource

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        remote = HolidayRemoteDataSource(baseUrl = server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `200 com 2 feriados parseia para o dominio`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    [
                      {
                        "date": "2026-12-25",
                        "name": "Natal",
                        "type": "national",
                        "weekday": "sexta-feira"
                      },
                      {
                        "date": "2026-01-01",
                        "name": "Confraternização mundial",
                        "type": "national",
                        "weekday": "quinta-feira"
                      }
                    ]
                    """.trimIndent(),
                ),
        )

        val holidays = remote.listHolidays(2026)

        assertThat(holidays).hasSize(2)
        assertThat(holidays[0]).isEqualTo(
            Holiday(date = LocalDate.parse("2026-12-25"), name = "Natal", type = "national"),
        )
        assertThat(holidays[1].name).isEqualTo("Confraternização mundial")
        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/api/feriados/v1/2026")
    }

    @Test
    fun `200 com lista vazia retorna lista vazia`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("[]"),
        )

        val holidays = remote.listHolidays(1900)

        assertThat(holidays).isEmpty()
        val request = server.takeRequest()
        assertThat(request.path).isEqualTo("/api/feriados/v1/1900")
    }

    @Test
    fun `404 traduz para lista vazia por convencao do dominio`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("""{"message": "not found"}"""),
        )

        val holidays = remote.listHolidays(1850)

        assertThat(holidays).isEmpty()
        val request = server.takeRequest()
        assertThat(request.path).isEqualTo("/api/feriados/v1/1850")
    }

    @Test
    fun `500 re-sinaliza como HttpException`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"message": "boom"}"""),
        )

        val result = runCatching { remote.listHolidays(2026) }
        val exception = result.exceptionOrNull()

        assertTrue("esperava HttpException, veio: $exception", exception is HttpException)
        assertThat((exception as HttpException).code()).isEqualTo(500)
    }

    @Test
    fun `IOException em rede offline re-sinaliza`() = runTest {
        // Sem enqueue: o próximo request falha com IOException (broken socket).
        val result = runCatching { remote.listHolidays(2026) }
        val exception = result.exceptionOrNull()

        assertTrue("esperava IOException, veio: $exception", exception is IOException)
    }

    @Test
    fun `segunda chamada no mesmo ano usa cache em memoria`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    [{"date":"2026-12-25","name":"Natal","type":"national"}]
                    """.trimIndent(),
                ),
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(500),
        )

        val first = remote.listHolidays(2026)
        val second = remote.listHolidays(2026)

        assertThat(first).hasSize(1)
        assertThat(second).isEqualTo(first) // sem nova request → 500 não é tocado
        assertThat(server.requestCount).isEqualTo(1)
    }
}
