// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.sync

import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.PendingOpEntity

/**
 * Resultado do envio de uma operação da fila ao backend (E3.3).
 *
 * - [Success]: op concluída — pode ser removida da fila.
 * - [Retriable]: falha transitória (rede, 5xx, timeout) — op volta
 *   para a fila e o worker pede retry ao WorkManager.
 * - [Permanent]: falha definitiva (HTTP 4xx do contrato, payload
 *   inválido) — op é descartada (a alteração local permanece; o
 *   backend rejeitou ou o payload não decodifica).
 */
sealed interface SyncOutcome {
    data object Success : SyncOutcome
    data class Retriable(val reason: String) : SyncOutcome
    data class Permanent(val httpCode: Int, val reason: String) : SyncOutcome
}

/**
 * Porta de sincronização usada pelo `SyncWorker` (`:app`).
 *
 * Isola o acesso HTTP (`RemoteDataSource`) e a interpretação dos
 * payloads JSON da fila `pending_ops` na camada de dados — o worker
 * não conhece o contrato de rede nem os DTOs, apenas entrega a op e
 * recebe o resultado classificado. Isso mantém o worker testável com
 * um fake e a serialização dentro do módulo que declara os payloads.
 *
 * A implementação real [BrainOutSyncDispatcher] envolve o cliente
 * Retrofit do E3.2:
 * - Projetos/tarefas: **PUT** upsert idempotente (política
 *   cliente-supplied UUID do stub — ver `backend-stub/server.py`).
 *   O `id` vai no path e no body; divergência = 400 (erro permanente).
 * - Tags: **POST** create (o servidor gera o id — tags não têm
 *   identidade local forte) e **DELETE** idempotente.
 * - Deletes de projeto/tarefa: **DELETE**, sempre 204 (retry-safe).
 *
 * Classificação de erros: `IOException` e HTTP 5xx são retriables;
 * HTTP 4xx é permanente (contrato violado).
 */
interface SyncDispatcher {

    /**
     * Envia a op [PendingOpEntity] ao backend e devolve o resultado
     * classificado. Payloads inválidos (JSON malformado, tipo não
     * suportado, opType desconhecido) retornam [SyncOutcome.Permanent]
     * — não há como reenviar uma op cujo payload não decodifica.
     */
    suspend fun send(op: PendingOpEntity): SyncOutcome
}
