// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Tipo de entidade referenciada por uma [PendingOpEntity].
 *
 * Os nomes são persistidos textualmente (`name`) para sobreviver a
 * reordenações da enum entre versões do app.
 */
enum class SyncEntityType { PROJECT, TASK, TAG }

/**
 * Ação sincronizada por uma [PendingOpEntity].
 *
 * CREATE e UPDATE são enviados ao backend como PUT idempotente
 * (upsert — contrato do stub em `backend-stub/server.py`), então a
 * distinção existe apenas para auditoria da fila; DELETE usa o
 * verbo DELETE, que o stub responde 204 mesmo quando a entidade
 * já não existe (retry-safe).
 */
enum class SyncOpType { CREATE, UPDATE, DELETE }

/**
 * Operação pendente de sincronização com a retaguarda (E3.3).
 *
 * Cada escrita local do app (projetos, tarefas, tags) grava uma linha
 * aqui **na mesma transação Room** da escrita principal — ou a altera-
 * ção e a fila são aplicadas juntas, ou nada é aplicado. O
 * `SyncWorker` (módulo `:app`) drena a fila em ordem de inserção
 * (`id` auto-incremental) e remove a linha após o sucesso.
 *
 * A tabela **não** tem FK para `projects`/`tasks`/`tags`: a fila deve
 * sobreviver à remoção da entidade referenciada (ex.: projeto apagado
 * enquanto a op CREATE ainda não sincronizou — a op DELETE do projeto
 * cobre o resultado final no backend).
 *
 * @property id Sequência auto-incremental — define a ordem de drenagem.
 * @property entityType [SyncEntityType.name] da entidade alvo.
 * @property entityId UUID local da entidade (política cliente-supplied).
 * @property opType [SyncOpType.name] da operação.
 * @property payload Corpo JSON a enviar ao backend (vazio `{}` para
 *   DELETE — o path já identifica a entidade).
 * @property createdAt Instante em que a op foi enfileirada.
 * @property attempts Tentativas de envio já realizadas (auditoria de
 *   backoff; o limite de rendição é responsabilidade do WorkManager).
 */
@Entity(
    tableName = "pending_ops",
    indices = [
        Index(value = ["created_at"], name = "idx_pending_ops_created_at"),
    ],
)
data class PendingOpEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "entity_type")
    val entityType: String,
    @ColumnInfo(name = "entity_id")
    val entityId: String,
    @ColumnInfo(name = "op_type")
    val opType: String,
    @ColumnInfo(name = "payload")
    val payload: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "attempts")
    val attempts: Int = 0,
) {
    companion object {

        /**
         * Fábrica de operação pendente. Serializa [payloadObj] com o
         * [SYNC_JSON] quando informado; caso contrário grava `{}` (op
         * DELETE — o corpo é irrelevante para o contrato).
         */
        fun enqueue(
            entityType: SyncEntityType,
            entityId: String,
            opType: SyncOpType,
            payloadObj: Any? = null,
            now: Instant = Instant.now(),
        ): PendingOpEntity = PendingOpEntity(
            entityType = entityType.name,
            entityId = entityId,
            opType = opType.name,
            payload = payloadObj?.let { obj -> SYNC_JSON.encodeToString(SyncPayloadSerializer(obj), obj) }
                ?: "{}",
            createdAt = now,
        )
    }
}

/**
 * Configuração compartilhada de serialização da fila de sync.
 *
 * `encodeDefaults = true` garante que campos com default (`done`,
 * `priority`) sempre apareçam no corpo — o backend Pydantic aceita
 * tanto, mas enviar tudo simplifica o debug da fila.
 */
val SYNC_JSON: Json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

/**
 * Resolve o serializer polimórfico dos payloads de sync. Os payloads
 * são `@Serializable data class` sem genéricos, então o `Any` da
 * fábrica acima é resolvido por inspeção de tipo em runtime — as
 * três formas de payload são conhecidas e fechadas.
 */
@Suppress("FunctionNaming")
private fun SyncPayloadSerializer(obj: Any): kotlinx.serialization.KSerializer<Any> =
    @Suppress("UNCHECKED_CAST")
    when (obj) {
        is ProjectSyncPayload -> ProjectSyncPayload.serializer()
        is TaskSyncPayload -> TaskSyncPayload.serializer()
        is TagSyncPayload -> TagSyncPayload.serializer()
        else -> throw IllegalArgumentException(
            "Payload de sync não suportado: ${obj::class.simpleName}",
        )
    } as kotlinx.serialization.KSerializer<Any>

/** Payload de projeto enviado em CREATE/UPDATE (PUT upsert). */
@Serializable
data class ProjectSyncPayload(
    @kotlinx.serialization.SerialName("id")
    val id: String,
    val name: String,
    val description: String?,
)

/** Payload de tarefa enviado em CREATE/UPDATE (PUT upsert). */
@Serializable
data class TaskSyncPayload(
    @kotlinx.serialization.SerialName("id")
    val id: String,
    @kotlinx.serialization.SerialName("project_id")
    val projectId: String,
    val title: String,
    val priority: Int,
    val done: Boolean,
)

/** Payload de tag enviado em CREATE (POST — o servidor gera o id). */
@Serializable
data class TagSyncPayload(
    val name: String,
    val color: String,
)
