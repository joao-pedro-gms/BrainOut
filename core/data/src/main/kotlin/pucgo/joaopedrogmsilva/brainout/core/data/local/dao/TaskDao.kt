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

    /**
     * Observa, para cada `priority_code` (0..4) das tarefas de projetos
     * do [ownerId], a contagem agregada por prioridade. Consulta
     * reativa (E2.7): emite novamente sempre que a tabela `tasks` ou
     * `projects` mudar (invalidação do Room).
     *
     * Retorna uma linha por prioridade presente no banco — prioridades
     * sem tarefas não aparecem; o consumidor preenche os níveis
     * faltantes com zero.
     */
    @Query(
        """
        SELECT t.priority_code AS priorityCode, COUNT(*) AS taskCount
        FROM tasks t
        INNER JOIN projects p ON p.id = t.project_id
        WHERE p.owner_id = :ownerId
        GROUP BY t.priority_code
        """,
    )
    fun observeCountByPriority(ownerId: String): Flow<List<PriorityCountRow>>

    /**
     * Contagem global de tarefas do [ownerId] por estado: totais e
     * concluídas (`status = 'DONE'`), usadas pela taxa de conclusão
     * semanal do Dashboard (E2.7). Reativa — re-emite a cada mudança
     * em `tasks`/`projects`.
     */
    @Query(
        """
        SELECT COUNT(*) AS totalCount,
               SUM(CASE WHEN t.status = 'DONE' THEN 1 ELSE 0 END) AS doneCount,
               SUM(CASE WHEN t.status = 'DONE'
                         AND t.completed_at >= :weekStartMillis THEN 1 ELSE 0 END) AS doneThisWeekCount
        FROM tasks t
        INNER JOIN projects p ON p.id = t.project_id
        WHERE p.owner_id = :ownerId
        """,
    )
    fun observeCompletionStats(ownerId: String, weekStartMillis: Long): Flow<CompletionStatsRow>

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

    /**
     * Atualiza `status` E `completed_at` numa única escrita. Usado
     * por [pucgo.joaopedrogmsilva.brainout.core.data.repository.TaskRepositoryImpl.completeAndCascade]
     * dentro de uma `@Transaction` junto com a marca de projeto
     * concluído, garantindo que `tasks.completed_at` e
     * `projects.is_completed` sejam ambos persistidos como uma
     * única operação atômica (RN03 — E2.5).
     *
     * `completedAt` em milissegundos epoch; `null` para reabrir a
     * tarefa (limpa o carimbo).
     */
    @Query(
        "UPDATE tasks SET status = :status, completed_at = :completedAt WHERE id = :id",
    )
    suspend fun updateStatusAndCompletedAt(
        id: String,
        status: String,
        completedAt: java.time.Instant?,
    )

    /** Remove a tarefa com o `id` informado. */
    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)
}
