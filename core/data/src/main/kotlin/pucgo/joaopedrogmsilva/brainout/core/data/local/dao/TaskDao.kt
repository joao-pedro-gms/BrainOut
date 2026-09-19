// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TaskEntity

/**
 * DAO da tabela `tasks`.
 *
 * Oferece tanto operações `suspend` para uso em repositórios quanto
 * [Flow]s para observação reativa na camada de apresentação.
 *
 * As pesquisas retornam [TaskEntity] (e não o modelo de domínio `Task`)
 * por design: o mapeamento Entity ↔ Domain é responsabilidade do
 * repositório, mantendo o DAO agnóstico ao contrato de domínio.
 */
@Dao
interface TaskDao {

    // --- Reads ---

    /**
     * Observa as tarefas de um projeto, ordenadas por prioridade
     * decrescente e, em empate, pela ordem de criação ascendente
     * (mais antiga primeiro dentro da mesma prioridade).
     */
    @Query(
        "SELECT * FROM tasks WHERE project_id = :projectId ORDER BY priority_code DESC, created_at",
    )
    fun observeForProject(projectId: String): Flow<List<TaskEntity>>

    /**
     * Observa todas as tarefas cujos projetos pertencem ao proprietário.
     * Útil para a sub-tela "Todas as tarefas" do `:feature:tasks`.
     */
    @Query(
        """
        SELECT t.* FROM tasks t
        INNER JOIN projects p ON p.id = t.project_id
        WHERE p.owner_id = :ownerId
        ORDER BY t.created_at DESC
        """,
    )
    fun observeAllForOwner(ownerId: String): Flow<List<TaskEntity>>

    /** Busca pontual por `id`. Retorna `null` se não existir. */
    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): TaskEntity?

    /**
     * Conta tarefas **ativas** (status != 'DONE') de um projeto.
     *
     * Usado pelo `CreateTaskUseCase` para validar a regra de negócio
     * RN01 (limite de 50 tarefas ativas por projeto).
     */
    @Query(
        """
        SELECT COUNT(*) FROM tasks
        WHERE project_id = :projectId AND status != 'DONE'
        """,
    )
    suspend fun countActiveByProject(projectId: String): Int

    // --- Writes ---

    /**
     * Insere uma nova tarefa. Lança `SQLiteConstraintException` em caso
     * de PK duplicada ou violação da FK `project_id` (projeto inexistente).
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    /**
     * Atualiza apenas a coluna `status`. Validações de transição
     * (matriz `TaskStatus.canTransitionTo`) ficam no domínio
     * (`Task.transitionTo`); aqui só persistimos o novo valor.
     */
    @Query("UPDATE tasks SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    /** Remove a tarefa com o `id` informado. */
    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)
}
