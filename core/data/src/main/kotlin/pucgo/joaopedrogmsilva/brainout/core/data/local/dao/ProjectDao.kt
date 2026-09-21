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
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TaskEntity

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

    /**
     * Atualiza a coluna `is_completed` do projeto.
     *
     * Usado pela cascata de RN03 (E2.5) — escrita atômica junto com
     * `tasks.status`/`tasks.completed_at` via [cascadeCompleteTask]
     * e [cascadeReopenTask]. Não é exposto diretamente para a UI;
     * apenas as duas operações de cascata abaixo o consomem.
     */
    @Query("UPDATE projects SET is_completed = :isCompleted WHERE id = :id")
    suspend fun updateIsCompleted(id: String, isCompleted: Boolean)

    /**
     * Executa a cascata de "concluir tarefa" (RN03 — E2.5) em uma
     * única transação Room.
     *
     * Recebe do domínio a [TaskEntity] já com `status = DONE` e
     * `completedAt` preenchidos, e o [TaskDao] injetado pelo caller
     * (a interface Room não permite referenciar outro DAO aqui
     * diretamente, então o repositório passa o DAO como
     * parâmetro). Persistimos em ordem:
     *
     * 1. Atualizamos o status + completed_at da tarefa.
     * 2. Recontamos tarefas ativas restantes — se for zero,
     *    marcamos o projeto como concluído (`is_completed = 1`).
     *
     * Se qualquer escrita falhar, Room reverte as anteriores — a UI
     * nunca observa um estado intermediário.
     */
    @Transaction
    suspend fun cascadeCompleteTask(
        taskDao: TaskDao,
        taskId: String,
        newStatus: String,
        completedAt: java.time.Instant,
        projectId: String,
    ) {
        taskDao.updateStatusAndCompletedAt(
            id = taskId,
            status = newStatus,
            completedAt = completedAt,
        )
        val activeAfter = taskDao.countActiveByProject(projectId)
        if (activeAfter == 0) {
            updateIsCompleted(projectId, true)
        }
    }

    /**
     * Executa a cascata de "reabrir tarefa" (RN03 — E2.5) em uma
     * única transação Room.
     *
     * Reabrir uma tarefa incrementa a contagem de tarefas ativas
     * do projeto; se o projeto já estava marcado como concluído,
     * desmarcamos (o projeto volta a aparecer como ativo). Se
     * ainda há outras tarefas ativas, a contagem simplesmente
     * cresceu e o projeto já estava ativo — sem mudança no flag.
     */
    @Transaction
    suspend fun cascadeReopenTask(
        taskDao: TaskDao,
        taskId: String,
        newStatus: String,
        projectId: String,
    ) {
        taskDao.updateStatusAndCompletedAt(
            id = taskId,
            status = newStatus,
            completedAt = null,
        )
        val project = findById(projectId)
        if (project != null && project.isCompleted) {
            // Se a reabertura deixou o projeto com pelo menos uma
            // tarefa ativa (a que acabamos de reabrir conta!), o
            // projeto volta a ficar ativo.
            updateIsCompleted(projectId, false)
        }
    }

    /**
     * Executa a cascata de "excluir tarefa ativa" (RN03 — E2.5) em
     * uma única transação Room.
     *
     * A exclusão é o caminho menos óbvio da regra: se a tarefa
     * excluída era a última ativa do projeto, o resultado líquido
     * é o mesmo de concluí-la — `countActive == 0`, e o projeto
     * deve ser marcado como concluído. Tarefas já concluídas
     * (DONE) não entram na contagem de "ativas", então excluí-las
     * não dispara a cascata.
     *
     * @param taskId id da tarefa a remover (a referência de status
     *  deve ser lida ANTES pelo repositório para sabermos se era
     *  ativa).
     */
    @Transaction
    suspend fun cascadeDeleteTask(
        taskDao: TaskDao,
        taskId: String,
        projectId: String,
        wasActive: Boolean,
    ) {
        taskDao.deleteById(taskId)
        if (wasActive) {
            val activeAfter = taskDao.countActiveByProject(projectId)
            if (activeAfter == 0) {
                updateIsCompleted(projectId, true)
            }
        }
    }
}
