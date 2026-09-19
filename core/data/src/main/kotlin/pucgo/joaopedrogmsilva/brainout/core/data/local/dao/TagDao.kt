// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TagEntity

/**
 * DAO da tabela `tags`.
 *
 * Oferece tanto operações `suspend` para uso em repositórios quanto
 * [Flow]s para observação reativa na camada de apresentação.
 *
 * O índice único `(owner_id, name)` é a chave da invariante de
 * unicidade por usuário: o `insert` usa `OnConflictStrategy.ABORT`
 * para que colisões falhem explicitamente como
 * `SQLiteConstraintException` em vez de silenciosamente substituir.
 * O repositório (`TagRepositoryImpl`) trata essa exceção e a
 * converte em erro de domínio.
 */
@Dao
interface TagDao {

    // --- Reads ---

    /**
     * Observa todas as tags de um proprietário, ordenadas pelo nome.
     * Usado pelo `HomeViewModel` para popular o diálogo de seleção
     * de tags ao criar/editar projeto.
     */
    @Query("SELECT * FROM tags WHERE owner_id = :ownerId ORDER BY name")
    fun observeForOwner(ownerId: String): Flow<List<TagEntity>>

    /** Busca pontual por `id`. Retorna `null` se não existir. */
    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): TagEntity?

    /**
     * Busca várias tags por `id` em uma única consulta. Usado pelo
     * `TagRepository.findByIds` para carregar tags a partir dos
     * `tagIds` armazenados na UI (chips).
     *
     * Recebe `List<String>` para casar com o bind nativo do Room; o
     * domínio aceita `Collection<String>` e converte aqui.
     */
    @Query("SELECT * FROM tags WHERE id IN (:ids)")
    suspend fun findByIds(ids: List<String>): List<TagEntity>

    // --- Writes ---

    /**
     * Insere uma nova tag. Retorna o `rowId` da linha inserida (Long).
     *
     * `ABORT` faz com que uma colisão do índice único
     * `(owner_id, name)` resulte em `SQLiteConstraintException`,
     * permitindo ao repositório distinguir o caso "nome duplicado"
     * de outros erros.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(tag: TagEntity): Long

    /** Remove a tag com o `id` informado (cascateia em `project_tags`). */
    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: String)
}
