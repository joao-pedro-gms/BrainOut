// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.remote

import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Holiday
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Cache somente em memória, por ano; chamadas concorrentes compartilham a consulta. */
class HolidayRemoteDataSource(
    baseUrl: String,
    client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build(),
) {
    private val api = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(HolidayApi::class.java)
    private val cache = mutableMapOf<Int, List<Holiday>>()
    private val mutex = Mutex()

    /** 404 significa ano sem dados; outros erros e cancelamento propagam sem preencher cache. */
    suspend fun listHolidays(year: Int): List<Holiday> = mutex.withLock {
        cache[year]?.let { return@withLock it }
        val fresh = try {
            api.listHolidays(year).map { Holiday(LocalDate.parse(it.date), it.name, it.type) }
        } catch (e: HttpException) {
            if (e.code() == HTTP_NOT_FOUND) emptyList() else throw e
        }
        cache[year] = fresh
        fresh
    }

    companion object {
        private const val CALL_TIMEOUT_SECONDS = 10L
        private const val HTTP_NOT_FOUND = 404
    }
}
