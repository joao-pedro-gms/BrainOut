// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.TaskDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TaskEntity
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository

/**
 * Implementação Room de [TaskRepository].
 *
 * Mantém o mapeamento entre [Task] (domínio) e [TaskEntity] (Room).
 * Em [changeStatus], aplica a matriz de transições do domínio via
 * [Task.transitionTo] antes de persistir — validações de estado
 * ficam no domínio, a persistência é agnóstica.
 *
 * Expõe [countActiveByProject] para que o `CreateTaskUseCase`
 * consiga aplicar a regra RN01 sem importar `:core:data`.
 *
 * @property taskDao DAO de tarefas injetado pelo Hilt via `DataModule`.
 */
@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
) : TaskRepository {

    override fun observeForProject(projectId: String): Flow<List<Task>> =
        taskDao.observeForProject(projectId).map { rows -> rows.map { it.toDomain() } }

    override fun observeAllForOwner(ownerId: String): Flow<List<Task>> =
        taskDao.observeAllForOwner(ownerId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun findById(id: String): Task? =
        taskDao.findById(id)?.toDomain()

    override suspend fun create(task: Task): Task {
        taskDao.insert(TaskEntity.fromDomain(task))
        return task
    }

    override suspend fun update(task: Task): Task {
        taskDao.update(TaskEntity.fromDomain(task))
        return task
    }

    override suspend fun changeStatus(id: String, target: TaskStatus): Task {
        val current = taskDao.findById(id)?.toDomain()
            ?: throw IllegalArgumentException("Tarefa não encontrada: $id")
        val updated = current.transitionTo(target)
        taskDao.update(TaskEntity.fromDomain(updated))
        return updated
    }

    override suspend fun delete(id: String) {
        taskDao.deleteById(id)
    }

    override suspend fun countActiveByProject(projectId: String): Int =
        taskDao.countActiveByProject(projectId)
}
