// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.PendingOpEntity

/**
 * DAO da fila de operações pendentes de sincronização (E3.3).
 *
 * O padrão de uso é transacional: o repositório chama [enqueueInTx]
 * (ou o bloco `@Transaction` do próprio DAO de destino) para gravar a
 * escrita local **e** a op na mesma transação Room. O `SyncWorker`
 * drena com [nextBatch], executa a chamada HTTP e conclui com
 * [markAttempt]/[deleteById].
 *
 * Sem FK para as entidades de negócio: ops podem referenciar entidades
 * já removidas localmente (remoção em cascata) sem quebrar a fila.
 */
@Dao
interface PendingOpDao {

    // --- Reads ---

    /**
     * Próximo lote de operações, em ordem de enfileiramento (`id`
     * crescente). O worker processa o lote sequencialmente e para no
     * primeiro erro de rede para preservar a ordem global.
     */
    @Query("SELECT * FROM pending_ops ORDER BY id ASC LIMIT :limit")
    suspend fun nextBatch(limit: Int): List<PendingOpEntity>

    /** Contagem atual de operações pendentes (exposta na UI em E3.4). */
    @Query("SELECT COUNT(*) FROM pending_ops")
    suspend fun count(): Int

    /**
     * Observa a contagem de operações pendentes — emitida a cada
     * mudança da tabela (invalidation tracker do Room). Usado pela
     * camada de apresentação para o indicador "X alterações aguardando".
     */
    @Query("SELECT COUNT(*) FROM pending_ops")
    fun observeCount(): kotlinx.coroutines.flow.Flow<Int>

    // --- Writes ---

    @Insert
    suspend fun insert(op: PendingOpEntity): Long

    @Update
    suspend fun update(op: PendingOpEntity)

    @Query("DELETE FROM pending_ops WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Remove todas as ops de uma entidade (usado após sync completo). */
    @Query("DELETE FROM pending_ops WHERE entity_type = :entityType AND entity_id = :entityId")
    suspend fun deleteForEntity(entityType: String, entityId: String)

    /**
     * Registra uma tentativa: incrementa [PendingOpEntity.attempts] e
     * regrava o payload (a op pode ter sido revalidada). Executa a
     * re-leitura e a escrita na mesma transação para evitar perder um
     * incremento em corrida com outra escrita da fila.
     */
    @Transaction
    suspend fun markAttempt(opId: Long) {
        val op = findById(opId) ?: return
        update(op.copy(attempts = op.attempts + 1))
    }

    /** Busca pontual por `id` (uso interno do [markAttempt] e testes). */
    @Query("SELECT * FROM pending_ops WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): PendingOpEntity?

    /**
     * Enfileira [op] **e** executa [write] na mesma transação Room —
     * helper para a escrita dual dos repositórios (E3.3, item 2).
     *
     * Recebe a escrita como lambda para que cada DAO componha a sua
     * operação sem o `PendingOpDao` conhecer os outros DAOs. Se
     * qualquer uma das duas escritas falhar, Room reverte ambas —
     * nunca há alteração local sem op pendente correspondente.
     */
    @Transaction
    suspend fun enqueueInTx(op: PendingOpEntity, write: suspend () -> Unit) {
        write()
        insert(op)
    }
}
