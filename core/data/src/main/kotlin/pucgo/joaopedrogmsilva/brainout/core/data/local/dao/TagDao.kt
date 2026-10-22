// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectTagRow
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
     * Observa as tags de vários projetos em **uma** consulta.
     *
     * Emite uma linha por par (projeto, tag). Um conjunto vazio de
     * projetos produz uma lista vazia — o Room emite normalmente, ao
     * contrário de combinar consultas separadas (que nunca emitiriam).
     *
     * @param projectIds ids dos projetos observados.
     */
    @Query(
        """
        SELECT t.*, pt.project_id AS project_id FROM tags t
        INNER JOIN project_tags pt ON pt.tag_id = t.id
        WHERE pt.project_id IN (:projectIds)
        ORDER BY t.name
        """,
    )
    fun observeForProjects(projectIds: List<String>): Flow<List<ProjectTagRow>>

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
