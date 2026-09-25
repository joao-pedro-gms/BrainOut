// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.PendingOpDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.ProjectDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.TaskDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.PendingOpEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.SyncEntityType
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.SyncOpType
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TaskEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TaskSyncPayload
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidStateTransitionException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TaskNotFoundException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskCompletionStats
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskPriorityCount
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository

/**
 * Implementação Room de [TaskRepository] com **escrita dual**
 * offline-first (E3.3).
 *
 * Toda mutação grava no Room **e** enfileira a op em `pending_ops`
 * na mesma transação Room ([PendingOpDao.enqueueInTx]). O corpo do
 * payload segue o contrato do stub (`backend-stub/server.py`): PUT
 * upsert idempotente com o `id` do cliente no path e no body; o campo
 * `done` espelha `status == DONE`.
 *
 * Em [changeStatus], aplica a matriz de transições do domínio via
 * [Task.transitionTo] antes de persistir — validações de estado
 * ficam no domínio, a persistência é agnóstica.
 *
 * **RN03 — E2.5 (conclusão cascata):** [completeAndCascade] e
 * [reopenAndCascade] coordenam a transição de status da tarefa
 * com a atualização do flag `is_completed` do projeto dentro de
 * **uma única transação Room** (via `ProjectDao.cascadeCompleteTask`
 * / `cascadeReopenTask`). A UI nunca observa um estado em que a
 * tarefa está DONE mas o projeto permanece ativo — ou tudo é
 * aplicado, ou nada é (rollback automático do Room).
 *
 * A op pendente das cascatas é o UPDATE da tarefa (payload completo);
 * a marca de conclusão do projeto é derivada no backend pelo mesmo
 * caminho de negócio — não enfileiramos ops separadas para o projeto,
 * mantendo a fila 1:1 com as ações do usuário.
 *
 * `@property taskDao` DAO de tarefas injetado pelo Hilt via `DataModule`.
 * `@property projectDao` DAO de projetos — necessário para acessar
 *  os métodos `@Transaction` de cascata que coordenam `tasks` e
 *  `projects` atomicamente.
 * `@property pendingOpDao` DAO da fila de sincronização (E3.3).
 */
@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val projectDao: ProjectDao,
    private val pendingOpDao: PendingOpDao,
) : TaskRepository {

    override fun observeForProject(projectId: String): Flow<List<Task>> =
        taskDao.observeForProject(projectId).map { rows -> rows.map { it.toDomain() } }

    override fun observeAllForOwner(ownerId: String): Flow<List<Task>> =
        taskDao.observeAllForOwner(ownerId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun findById(id: String): Task? =
        taskDao.findById(id)?.toDomain()

    /** Cria a tarefa e enfileira a op CREATE na mesma transação. */
    override suspend fun create(task: Task): Task {
        pendingOpDao.enqueueInTx(
            op = opFor(task.id, SyncOpType.CREATE, task),
        ) {
            taskDao.insert(TaskEntity.fromDomain(task))
        }
        return task
    }

    /** Atualiza a tarefa e enfileira a op UPDATE na mesma transação. */
    override suspend fun update(task: Task): Task {
        pendingOpDao.enqueueInTx(
            op = opFor(task.id, SyncOpType.UPDATE, task),
        ) {
            taskDao.update(TaskEntity.fromDomain(task))
        }
        return task
    }

    /**
     * Aplica a transição de status (matriz do domínio) e enfileira a
     * op UPDATE — tudo dentro da mesma transação Room: se a matriz
     * rejeitar a transição, nem o Room nem a fila mudam.
     */
    override suspend fun changeStatus(id: String, target: TaskStatus): Task {
        val current = taskDao.findById(id)?.toDomain()
            ?: throw TaskNotFoundException(id)
        val updated = current.transitionTo(target)
        pendingOpDao.enqueueInTx(
            op = opFor(id, SyncOpType.UPDATE, updated),
        ) {
            taskDao.update(TaskEntity.fromDomain(updated))
        }
        return updated
    }

    /**
     * Remove a tarefa e enfileira a op DELETE na mesma transação.
     *
     * RN03 — exclusão da última tarefa ativa também conclui o
     * projeto. Precisamos saber se a tarefa era ativa antes de
     * removê-la (para reaplicar a regra "countActive == 0 ⇒
     * projeto concluído").
     */
    override suspend fun delete(id: String) {
        val current = taskDao.findById(id)
            ?: return // idempotente: tarefa inexistente = nada a fazer
        val wasActive = current.status != TaskStatus.DONE.name
        pendingOpDao.enqueueInTx(
            op = PendingOpEntity.enqueue(
                entityType = SyncEntityType.TASK,
                entityId = id,
                opType = SyncOpType.DELETE,
            ),
        ) {
            projectDao.cascadeDeleteTask(
                taskDao = taskDao,
                taskId = id,
                projectId = current.projectId,
                wasActive = wasActive,
            )
        }
    }

    override suspend fun countActiveByProject(projectId: String): Int =
        taskDao.countActiveByProject(projectId)

    /**
     * E2.7 — contagem reativa por prioridade. A query agrupa por
     * `priority_code`; aqui preenchemos os níveis 0..4 ausentes com
     * zero para que o Dashboard sempre desenhe 5 barras.
     */
    override fun observeCountByPriority(ownerId: String): Flow<List<TaskPriorityCount>> =
        taskDao.observeCountByPriority(ownerId).map { rows ->
            val byCode = rows.associate { it.priorityCode to it.taskCount }
            TaskPriority.VALID_CODES.map { code ->
                TaskPriorityCount(priorityCode = code, count = byCode[code] ?: 0)
            }
        }

    /**
     * E2.7 — estatísticas de conclusão reativas. Delegamos direto ao
     * DAO: a conversão de `weekStartMillis` é responsabilidade do
     * caller (o ViewModel decide a semântica de "semana").
     */
    override fun observeCompletionStats(
        ownerId: String,
        weekStartMillis: Long,
    ): Flow<TaskCompletionStats> =
        taskDao.observeCompletionStats(ownerId, weekStartMillis).map { row ->
            TaskCompletionStats(
                totalCount = row.totalCount,
                doneCount = row.doneCount,
                doneThisWeekCount = row.doneThisWeekCount,
            )
        }

    /**
     * RN03 (E2.5): conclui a tarefa (target = DONE) e, em cascata,
     * marca o projeto como concluído caso a tarefa seja a última
     * ativa.
     *
     * Aplica a matriz de transições do domínio via
     * [Task.transitionTo]. Se a tarefa está em [TaskStatus.TODO],
     * a matriz proíbe TODO → DONE direto (regra E1.4 do domínio);
     * este caminho respeita essa regra encadeando as transições:
     * TODO → DOING → DONE dentro do mesmo método, cada uma
     * passando pelos invariantes do domínio. O resultado é uma
     * única alteração observável no Room, aplicada atomicamente
     * via [ProjectDao.cascadeCompleteTask] — e a op pendente
     * (UPDATE da tarefa em DONE) entra na mesma transação.
     *
     * A exceção [InvalidStateTransitionException] é propagada sem
     * efeito no banco caso nenhuma transição intermediária seja
     * permitida (estado inicial desconhecido, p.ex.).
     */
    override suspend fun completeAndCascade(taskId: String): Task {
        val current = taskDao.findById(taskId)?.toDomain()
            ?: throw TaskNotFoundException(taskId)
        if (current.status == TaskStatus.DONE) {
            // Já está concluída — no-op. O projeto já deve estar
            // concluído pelo caminho original; nada a fazer.
            return current
        }
        // Encadeia a transição para respeitar a matriz do domínio
        // (E1.4). Cada `transitionTo` valida a próxima etapa.
        val doing = current.transitionTo(TaskStatus.DOING)
        val done = doing.transitionTo(TaskStatus.DONE)
        val completedAt = done.completedAt
            ?: Instant.now() // fallback defensivo: transitionTo popula
        pendingOpDao.enqueueInTx(
            op = opFor(taskId, SyncOpType.UPDATE, done),
        ) {
            projectDao.cascadeCompleteTask(
                taskDao = taskDao,
                taskId = taskId,
                newStatus = TaskStatus.DONE.name,
                completedAt = completedAt,
                projectId = current.projectId,
            )
        }
        return done
    }

    /**
     * RN03 (E2.5): reabre a tarefa para [target] (status ativo) e,
     * em cascata, desmarca o projeto como concluído caso ele
     * estivesse marcado. Aplica a matriz de transições do domínio
     * via [Task.transitionTo]. A op pendente entra na mesma
     * transação da cascata.
     */
    override suspend fun reopenAndCascade(taskId: String, target: TaskStatus): Task {
        require(target != TaskStatus.DONE) {
            "reopenAndCascade aceita apenas estados ativos; use completeAndCascade para DONE"
        }
        val current = taskDao.findById(taskId)?.toDomain()
            ?: throw TaskNotFoundException(taskId)
        if (current.status != TaskStatus.DONE) {
            // Não está em DONE — não há reabertura a fazer; o caller
            // deveria usar o caminho simples. Lançamos para evitar
            // estado silencioso.
            throw InvalidStateTransitionException(
                "Reabertura só é válida a partir de DONE; tarefa $taskId está em ${current.status}",
            )
        }
        val updated = current.transitionTo(target)
        pendingOpDao.enqueueInTx(
            op = opFor(taskId, SyncOpType.UPDATE, updated),
        ) {
            // cascadeReopenTask é @Transaction — a reabertura da
            // tarefa e a desmarcação do projeto são aplicadas
            // atomicamente junto com o enfileiramento.
            projectDao.cascadeReopenTask(
                taskDao = taskDao,
                taskId = taskId,
                newStatus = target.name,
                projectId = current.projectId,
            )
        }
        return updated
    }

    /** Op pendente de tarefa com payload completo (PUT upsert). */
    private fun opFor(taskId: String, opType: SyncOpType, task: Task) =
        PendingOpEntity.enqueue(
            entityType = SyncEntityType.TASK,
            entityId = taskId,
            opType = opType,
            payloadObj = TaskSyncPayload(
                id = task.id,
                projectId = task.projectId,
                title = task.title,
                priority = task.priority.priorityCode,
                done = task.status == TaskStatus.DONE,
            ),
        )
}
