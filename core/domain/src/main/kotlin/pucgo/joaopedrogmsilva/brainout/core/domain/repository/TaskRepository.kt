// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus

interface TaskRepository {
    fun observeForProject(projectId: String): Flow<List<Task>>
    fun observeAllForOwner(ownerId: String): Flow<List<Task>>
    suspend fun findById(id: String): Task?
    suspend fun create(task: Task): Task
    suspend fun update(task: Task): Task
    suspend fun changeStatus(id: String, target: TaskStatus): Task
    suspend fun delete(id: String)
}
