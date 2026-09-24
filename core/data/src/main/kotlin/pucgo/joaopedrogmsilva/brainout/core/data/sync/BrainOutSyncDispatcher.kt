// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.sync

import android.util.Log
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.PendingOpEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectSyncPayload
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.SYNC_JSON
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.SyncEntityType
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.SyncOpType
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TagSyncPayload
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TaskSyncPayload
import pucgo.joaopedrogmsilva.brainout.core.data.remote.RemoteDataSource
import retrofit2.HttpException

/**
 * Implementação HTTP de [SyncDispatcher] sobre o cliente Retrofit do
 * E3.2 ([RemoteDataSource]).
 *
 * Interpreta o payload JSON de cada op (serialização vive neste
 * módulo) e roteia para o endpoint correspondente:
 * - PROJECT/TASK com CREATE/UPDATE → PUT upsert idempotente
 *   (contrato do stub — `backend-stub/server.py`), `id` do cliente
 *   no path e no body.
 * - TAG com CREATE → POST /v1/tags (servidor gera o id); DELETE →
 *   DELETE /v1/tags/{id} (204 mesmo ausente).
 * - DELETE de PROJECT/TASK → DELETE, 204 mesmo ausente.
 *
 * Tradução de erros:
 * - `IOException` (sem rede, timeout, DNS) → [SyncOutcome.Retriable].
 * - `HttpException` 5xx → [SyncOutcome.Retriable] (indisponibilidade
 *   temporária do serviço).
 * - `HttpException` 4xx → [SyncOutcome.Permanent] com o código HTTP
 *   logado — contrato violado, reenviar não muda o resultado.
 * - Payload não decodificável / op desconhecida → [SyncOutcome.Permanent].
 *
 * O logger é injetável para os testes unitários não dependerem de
 * `android.util.Log`.
 */
@Singleton
class BrainOutSyncDispatcher @Inject constructor(
    private val remote: RemoteDataSource,
    private val logError: (String) -> Unit = { message -> Log.e(TAG, message) },
) : SyncDispatcher {

    override suspend fun send(op: PendingOpEntity): SyncOutcome = try {
        route(op)
    } catch (e: IOException) {
        SyncOutcome.Retriable("rede indisponível: ${e.message}")
    } catch (e: HttpException) {
        val code = e.code()
        if (code in RETRIABLE_HTTP_RANGE) {
            SyncOutcome.Retriable("HTTP $code: ${e.message}")
        } else {
            logError("sync: HTTP $code em ${op.entityType}/${op.entityId} (${op.opType})")
            SyncOutcome.Permanent(httpCode = code, reason = e.message ?: "HTTP $code")
        }
    }

    /** Roteia a op para o endpoint do contrato (ou erro permanente). */
    @Suppress("ReturnCount")
    private suspend fun route(op: PendingOpEntity): SyncOutcome {
        val entityType = enumValueOfOrNull<SyncEntityType>(op.entityType)
            ?: return permanent(op, "entityType desconhecido: ${op.entityType}")
        val opType = enumValueOfOrNull<SyncOpType>(op.opType)
            ?: return permanent(op, "opType desconhecido: ${op.opType}")

        return when (entityType) {
            SyncEntityType.PROJECT -> when (opType) {
                SyncOpType.DELETE -> guard { remote.deleteProject(op.entityId) }
                else -> {
                    val payload = decodeOrReject<ProjectSyncPayload>(op)
                        ?: return permanent(op, "payload JSON inválido para PROJECT")
                    guard {
                        remote.updateProject(
                            projectId = payload.id,
                            name = payload.name,
                            description = payload.description,
                        )
                    }
                }
            }
            SyncEntityType.TASK -> when (opType) {
                SyncOpType.DELETE -> guard { remote.deleteTask(op.entityId) }
                else -> {
                    val payload = decodeOrReject<TaskSyncPayload>(op)
                        ?: return permanent(op, "payload JSON inválido para TASK")
                    guard {
                        remote.updateTask(
                            taskId = payload.id,
                            projectId = payload.projectId,
                            title = payload.title,
                            priority = payload.priority,
                            done = payload.done,
                        )
                    }
                }
            }
            SyncEntityType.TAG -> when (opType) {
                SyncOpType.CREATE -> {
                    val payload = decodeOrReject<TagSyncPayload>(op)
                        ?: return permanent(op, "payload JSON inválido para TAG")
                    guard { remote.createTag(name = payload.name, color = payload.color, id = payload.id) }
                }
                SyncOpType.DELETE -> guard { remote.deleteTag(op.entityId) }
                // UPDATE de tag não é suportado pelo contrato atual
                // (o POST gera novo id); op é descartada com log.
                SyncOpType.UPDATE -> permanent(op, "UPDATE de tag não suportado pelo contrato")
            }
        }
    }

    /**
     * Executa o envio e devolve [SyncOutcome.Success] em 2xx;
     * IOException/HttpException propagam para a tradução em [send].
     */
    private suspend fun guard(block: suspend () -> Unit): SyncOutcome {
        block()
        return SyncOutcome.Success
    }

    private inline fun <reified T> decodeOrReject(op: PendingOpEntity): T? = try {
        SYNC_JSON.decodeFromString<T>(op.payload)
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        null
    }

    private fun permanent(op: PendingOpEntity, reason: String): SyncOutcome {
        logError("sync: op ${op.id} (${op.entityType}/${op.entityId} ${op.opType}) inválida — $reason")
        return SyncOutcome.Permanent(httpCode = 0, reason = reason)
    }

    private inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String): T? =
        runCatching { enumValueOf<T>(name) }.getOrNull()

    companion object {
        private const val TAG = "BrainOutSync"

        /** 500..599 são retriable; o resto (4xx) é permanente. */
        private val RETRIABLE_HTTP_RANGE = 500..599
    }
}
