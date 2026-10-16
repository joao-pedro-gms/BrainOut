// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pucgo.joaopedrogmsilva.brainout.core.data.local.LocalTransaction
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.ProjectDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.TaskDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TaskEntity
import pucgo.joaopedrogmsilva.brainout.core.data.util.logDebug
import pucgo.joaopedrogmsilva.brainout.core.data.util.logWarn
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidStateTransitionException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TaskNotFoundException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskCompletionStats
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskPriorityCount
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BrainOut:TaskRepo"

/** Persistência local de task, com transações Room para escritas e cascatas. */
@Singleton
class TaskRepositoryImpl
    @Inject
    constructor(
        private val taskDao: TaskDao,
        private val projectDao: ProjectDao,
        private val localTransaction: LocalTransaction,
    ) : TaskRepository {
        override fun observeForProject(projectId: String): Flow<List<Task>> =
            taskDao.observeForProject(projectId).map { rows -> rows.map { it.toDomain() } }

        override fun observeAllForOwner(ownerId: String): Flow<List<Task>> =
            taskDao.observeAllForOwner(ownerId).map { rows -> rows.map { it.toDomain() } }

        override suspend fun findById(id: String): Task? = taskDao.findById(id)?.toDomain()

        override suspend fun create(task: Task): Task {
            logDebug(TAG, "Criando tarefa id=${task.id}, title=${task.title}, projectId=${task.projectId}")
            localTransaction.run {
                taskDao.insert(TaskEntity.fromDomain(task))
            }
            return task
        }

        override suspend fun update(task: Task): Task {
            logDebug(TAG, "Atualizando tarefa id=${task.id}, title=${task.title}, status=${task.status}")
            if (taskDao.findById(task.id) == null) {
                logWarn(TAG, "update ignorado: tarefa id=${task.id} não existe no Room")
                throw TaskNotFoundException(task.id)
            }
            localTransaction.run {
                taskDao.update(TaskEntity.fromDomain(task))
            }
            return task
        }

        override suspend fun changeStatus(
            id: String,
            target: TaskStatus,
        ): Task {
            logDebug(TAG, "Alterando status da tarefa id=$id para target=$target")
            val current =
                taskDao.findById(id)?.toDomain()
                    ?: throw TaskNotFoundException(id)
            val updated = current.transitionTo(target)
            localTransaction.run {
                taskDao.update(TaskEntity.fromDomain(updated))
            }
            return updated
        }

        override suspend fun delete(id: String) {
            logDebug(TAG, "Deletando tarefa id=$id")
            val current =
                taskDao.findById(id)
                    ?: run {
                        logDebug(TAG, "Deleção de tarefa id=$id ignorada: não encontrada no Room")
                        return
                    }
            val wasActive = current.status != TaskStatus.DONE.name
            localTransaction.run {
                projectDao.cascadeDeleteTask(
                    taskDao = taskDao,
                    taskId = id,
                    projectId = current.projectId,
                    wasActive = wasActive,
                )
            }
        }

        override suspend fun countActiveByProject(projectId: String): Int = taskDao.countActiveByProject(projectId)

        override fun observeCountByPriority(ownerId: String): Flow<List<TaskPriorityCount>> =
            taskDao.observeCountByPriority(ownerId).map { rows ->
                val byCode = rows.associate { it.priorityCode to it.taskCount }
                TaskPriority.VALID_CODES.map { code ->
                    TaskPriorityCount(priorityCode = code, count = byCode[code] ?: 0)
                }
            }

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

        override suspend fun completeAndCascade(taskId: String): Task {
            logDebug(TAG, "Concluindo tarefa id=$taskId em cascata com projeto")
            val current =
                taskDao.findById(taskId)?.toDomain()
                    ?: throw TaskNotFoundException(taskId)
            if (current.status == TaskStatus.DONE) {
                logDebug(TAG, "completeAndCascade: tarefa $taskId já em DONE — reidratando a cascata do projeto")
                return repairCompletedProject(current)
            }
            // Encadeia a transição para respeitar a matriz do domínio
            // (E1.4). Cada `transitionTo` valida a próxima etapa.
            val doing = current.transitionTo(TaskStatus.DOING)
            val done = doing.transitionTo(TaskStatus.DONE)
            val completedAt =
                done.completedAt
                    ?: Instant.now() // fallback defensivo: transitionTo popula
            localTransaction.run {
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

        private suspend fun repairCompletedProject(current: Task): Task {
            val activeAfter = taskDao.countActiveByProject(current.projectId)
            if (activeAfter == 0) {
                projectDao.updateIsCompleted(current.projectId, true)
                logDebug(TAG, "Cascata RN03 reidratada: projeto ${current.projectId} marcado como concluído")
            }
            return current
        }

        override suspend fun reopenAndCascade(
            taskId: String,
            target: TaskStatus,
        ): Task {
            logDebug(TAG, "Reabrindo tarefa id=$taskId para target=$target em cascata com projeto")
            require(target != TaskStatus.DONE) {
                "reopenAndCascade aceita apenas estados ativos; use completeAndCascade para DONE"
            }
            val current =
                taskDao.findById(taskId)?.toDomain()
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
            localTransaction.run {
                // cascadeReopenTask é @Transaction — a reabertura da
                // tarefa e a desmarcação do projeto são aplicadas
                // atomicamente na transação local.
                projectDao.cascadeReopenTask(
                    taskDao = taskDao,
                    taskId = taskId,
                    newStatus = target.name,
                    projectId = current.projectId,
                )
            }
            return updated
        }
    }
