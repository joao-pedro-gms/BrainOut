// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Interface Retrofit para o serviço público de feriados nacionais
 * (E3.5): BrasilAPI (`https://brasilapi.com.br/api/feriados/v1/{year}`).
 *
 * O serviço é gratuito e não exige autenticação; a URL base é
 * injetada via `BuildConfig.HOLIDAYS_BASE_URL` no construtor do
 * [HolidayRemoteDataSource] — nenhum host aparece hard-coded aqui.
 *
 * O endpoint retorna uma **lista** (e não `{"items": [...]}` como
 * o BrainOutApi), refletindo o contrato real do serviço externo.
 */
interface HolidayApi {

    /**
     * Lista os feriados nacionais do [year] informado.
     *
     * @throws retrofit2.HttpException 404 quando o ano está fora da
     *   cobertura do serviço; o [HolidayRemoteDataSource] traduz para
     *   lista vazia por convenção do domínio.
     */
    @GET("api/feriados/v1/{year}")
    suspend fun listHolidays(@Path("year") year: Int): List<HolidayDto>
}
