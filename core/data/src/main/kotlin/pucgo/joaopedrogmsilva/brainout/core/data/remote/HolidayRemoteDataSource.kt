// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.remote

import android.util.Log
import java.time.LocalDate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Holiday
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Fonte de dados remota do serviço de feriados nacionais (E3.5).
 *
 * Espelha o padrão do [RemoteDataSource] (E3.2): centraliza a
 * construção do Retrofit/OkHttp para a URL base injetada via
 * `BuildConfig.HOLIDAYS_BASE_URL` (flavor `dev`/`prod`), mantém
 * logger injetável para os testes unitários não dependerem de
 * `android.util.Log`, e expõe um método por endpoint.
 *
 * Política de cache (E3.5): mapa `Int -> List<Holiday>` em memória,
 * preenchido on-demand e reutilizado entre chamadas do mesmo ano.
 * Não há persistência em disco — feriados mudam raramente, mas o
 * usuário pode aguardar alguns segundos na primeira chamada do ano.
 *
 * Erros:
 * - 404 (ano sem cobertura) → traduzido para lista vazia.
 * - Timeout/IOException → exceção re-sinalizada para o chamador.
 */
class HolidayRemoteDataSource(
    baseUrl: String,
    private val logError: (String) -> Unit = { message -> Log.e(TAG, message) },
) {

    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            },
        )
        .build()

    private val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val api: HolidayApi = retrofit.create(HolidayApi::class.java)

    private val cache: MutableMap<Int, List<Holiday>> = mutableMapOf()
    private val cacheMutex = Mutex()

    /**
     * Lista os feriados nacionais do [year] (cache em memória incluído).
     *
     * @throws java.io.IOException em falha de rede/timeout.
     */
    suspend fun listHolidays(year: Int): List<Holiday> {
        cacheMutex.withLock { cache[year] }?.let { return it }
        val fresh = try {
            api.listHolidays(year).map { it.toDomain() }
        } catch (e: retrofit2.HttpException) {
            // 404 = ano sem cobertura; convenção do domínio é lista vazia.
            if (e.code() == 404) emptyList() else {
                logError("listHolidays($year) falhou: ${e.message}")
                throw e
            }
        } catch (e: Exception) {
            logError("listHolidays($year) falhou: ${e.message}")
            throw e
        }
        cacheMutex.withLock { cache[year] = fresh }
        return fresh
    }

    private fun HolidayDto.toDomain(): Holiday = Holiday(
        date = LocalDate.parse(date),
        name = name,
        type = type,
    )

    companion object {
        private const val TAG = "HolidayRemote"
    }
}
