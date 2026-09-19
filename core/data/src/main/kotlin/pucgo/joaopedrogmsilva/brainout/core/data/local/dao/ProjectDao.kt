// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
@file:Suppress("ConstructorParameterNaming")
package pucgo.joaopedrogmsilva.brainout.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectTagCrossRef
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TagEntity

/**
 * DAO da tabela `projects` (e operações relacionadas a `project_tags`).
 *
 * Oferece tanto operações `suspend` para uso em repositórios quanto
 * [Flow]s para observação reativa na camada de apresentação.
 *
 * As pesquisas retornam [ProjectEntity] (e não o modelo de domínio
 * `Project`) por design: o mapeamento Entity ↔ Domain é responsabilidade
 * do repositório, mantendo o DAO agnóstico ao contrato de domínio.
 */
@Dao
interface ProjectDao {

    // --- Reads ---

    /**
     * Observa todos os projetos de um proprietário, ordenados pelo mais
     * recente primeiro. Usado pelo `HomeViewModel`.
     */
    @Query("SELECT * FROM projects WHERE owner_id = :ownerId ORDER BY created_at DESC")
    fun observeAllForOwner(ownerId: String): Flow<List<ProjectEntity>>

    /** Busca pontual por `id`. Retorna `null` se não existir. */
    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ProjectEntity?

    /**
     * Observa as [TagEntity]s associadas ao projeto via JOIN com a
     * tabela de junção `project_tags`. Ordena pelo nome da tag.
     */
    @Query(
        """
        SELECT t.* FROM tags t
        INNER JOIN project_tags pt ON pt.tag_id = t.id
        WHERE pt.project_id = :projectId
        ORDER BY t.name
        """,
    )
    fun observeTagsFor(projectId: String): Flow<List<TagEntity>>

    // --- Writes ---

    /**
     * Insere um novo projeto. Lança `SQLiteConstraintException` em caso
     * de PK duplicada.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(project: ProjectEntity)

    @Update
    suspend fun update(project: ProjectEntity)

    /** Remove o projeto com o `id` informado (e cascateia em `tasks` e `project_tags`). */
    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String)

    // --- Tag associations ---

    /**
     * Insere associações N:N projeto ↔ tag. `IGNORE` torna o insert
     * idempotente: uma associação já existente é silenciosamente
     * descartada, evitando duplicação de chave primária composta.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProjectTags(refs: List<ProjectTagCrossRef>)

    /** Remove todas as associações de tags do projeto. */
    @Query("DELETE FROM project_tags WHERE project_id = :projectId")
    suspend fun clearProjectTags(projectId: String)

    /**
     * Substitui o conjunto de tags associadas ao projeto.
     *
     * Executa [clearProjectTags] e [insertProjectTags] na mesma
     * transação Room — ou tudo é aplicado, ou nada é. Garante que
     * o `Flow` observado em [observeTagsFor] nunca emite estado
     * intermediário com mistura de tags antigas e novas.
     */
    @Transaction
    suspend fun replaceProjectTags(projectId: String, tagIds: List<String>) {
        clearProjectTags(projectId)
        if (tagIds.isNotEmpty()) {
            insertProjectTags(
                tagIds.map { tagId -> ProjectTagCrossRef(project_id = projectId, tag_id = tagId) },
            )
        }
    }
}
