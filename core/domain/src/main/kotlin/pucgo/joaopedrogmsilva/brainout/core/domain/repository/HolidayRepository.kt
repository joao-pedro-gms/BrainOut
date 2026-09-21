// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.repository

import pucgo.joaopedrogmsilva.brainout.core.domain.model.Holiday

/**
 * Porta do repositório de feriados nacionais (E3.5).
 *
 * Implementado por [pucgo.joaopedrogmsilva.brainout.core.data.repository.HolidayRepositoryImpl]
 * no `:core:data` com chamadas ao serviço público BrasilAPI
 * (`/api/feriados/v1/{year}`). O domínio permanece independente do
 * detalhe HTTP — para a regra de negócio basta o contrato abaixo.
 *
 * Política de cache (E3.5): nenhum cache persistente; o
 * [pucgo.joaopedrogmsilva.brainout.core.data.repository.HolidayRepositoryImpl]
 * aplica um cache em memória (`Map<Int, List<Holiday>>`) durante a
 * vida do processo para evitar chamadas repetidas no mesmo ano.
 */
interface HolidayRepository {

    /**
     * Lista os feriados nacionais de um ano.
     *
     * @param year Ano civil (ex.: 2026). Anos sem dados (muito antigos
     *   ou futuros) retornam lista vazia por convenção — o caso de uso
     *   trata "sem feriado" como estado neutro.
     * @throws java.io.IOException em falha de rede/timeout.
     * @throws retrofit2.HttpException em erro HTTP não-2xx (ex.: 404
     *   para ano fora da cobertura da BrasilAPI).
     */
    suspend fun getHolidays(year: Int): List<Holiday>
}
