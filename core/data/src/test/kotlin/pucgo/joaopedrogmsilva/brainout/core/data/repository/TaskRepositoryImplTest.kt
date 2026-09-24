// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.CompletionStatsRow
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.PendingOpDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.PriorityCountRow
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.ProjectDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.TaskDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.PendingOpEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectTagCrossRef
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TagEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TaskEntity
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidStateTransitionException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TaskNotFoundException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus

/**
 * Testes diretos do [TaskRepositoryImpl] (gap Kover 60% do módulo).
 *
 * Cobre escrita dual (Room + `pending_ops`), a matriz de transições e
 * as cascatas RN03 (E2.5) — incluindo o comportamento P0-2 de
 * `completeAndCascade`/`changeStatus` lançando [TaskNotFoundException]
 * em vez de `IllegalArgumentException` cru.
 */
class TaskRepositoryImplTest {

    @Test
    fun `findById mapeia entidade para dominio`() = runTest {
        val taskDao = FakeTaskDao()
        val task = sampleTask()
        taskDao.storage[task.id] = TaskEntity.fromDomain(task)
        val repository = newRepository(taskDao)

        assertThat(repository.findById(task.id)).isEqualTo(task)
    }

    @Test
    fun `findById retorna null quando ausente`() = runTest {
        assertThat(newRepository(FakeTaskDao()).findById("ausente")).isNull()
    }

    @Test
    fun `create persiste e enfileira op CREATE`() = runTest {
        val taskDao = FakeTaskDao()
        val pendingOpDao = FakePendingOpDao()
        val repository = TaskRepositoryImpl(taskDao, FakeProjectDao(taskDao), pendingOpDao)
        val task = sampleTask()

        repository.create(task)

        assertThat(taskDao.storage[task.id]?.toDomain()).isEqualTo(task)
        val ops = pendingOpDao.enqueued
        assertThat(ops).hasSize(1)
        assertThat(ops.first().opType).isEqualTo("CREATE")
        assertThat(ops.first().entityType).isEqualTo("TASK")
        assertThat(ops.first().entityId).isEqualTo(task.id)
    }

    @Test
    fun `update persiste e enfileira op UPDATE`() = runTest {
        val taskDao = FakeTaskDao()
        val pendingOpDao = FakePendingOpDao()
        val repository = TaskRepositoryImpl(taskDao, FakeProjectDao(taskDao), pendingOpDao)
        val task = sampleTask()
        taskDao.storage[task.id] = TaskEntity.fromDomain(task)
        val updated = task.copy(title = "Título novo")

        repository.update(updated)

        assertThat(taskDao.storage[task.id]?.title).isEqualTo("Título novo")
        assertThat(pendingOpDao.enqueued.map { it.opType }).containsExactly("UPDATE")
    }

    @Test
    fun `changeStatus lanca TaskNotFoundException quando ausente`() = runTest {
        val repository = newRepository(FakeTaskDao())

        val thrown = runCatching { repository.changeStatus("ausente", TaskStatus.DONE) }
            .exceptionOrNull()

        assertThat(thrown).isInstanceOf(TaskNotFoundException::class.java)
    }

    @Test
    fun `changeStatus aplica matriz e enfileira op UPDATE`() = runTest {
        val taskDao = FakeTaskDao()
        val pendingOpDao = FakePendingOpDao()
        val repository = TaskRepositoryImpl(taskDao, FakeProjectDao(taskDao), pendingOpDao)
        val task = sampleTask(status = TaskStatus.DOING)
        taskDao.storage[task.id] = TaskEntity.fromDomain(task)

        val updated = repository.changeStatus(task.id, TaskStatus.DONE)

        assertThat(updated.status).isEqualTo(TaskStatus.DONE)
        assertThat(updated.completedAt).isNotNull()
        assertThat(taskDao.storage[task.id]?.status).isEqualTo(TaskStatus.DONE.name)
        assertThat(pendingOpDao.enqueued.map { it.opType }).containsExactly("UPDATE")
    }

    @Test
    fun `changeStatus rejeita transicao invalida sem enfileirar`() = runTest {
        val taskDao = FakeTaskDao()
        val pendingOpDao = FakePendingOpDao()
        val repository = TaskRepositoryImpl(taskDao, FakeProjectDao(taskDao), pendingOpDao)
        val done = sampleTask(status = TaskStatus.DONE)
        taskDao.storage[done.id] = TaskEntity.fromDomain(done)

        val thrown = runCatching { repository.changeStatus(done.id, TaskStatus.TODO) }
            .exceptionOrNull()

        assertThat(thrown).isInstanceOf(InvalidStateTransitionException::class.java)
        assertThat(pendingOpDao.enqueued).isEmpty()
    }

    @Test
    fun `delete de tarefa ausente e idempotente sem enfileirar`() = runTest {
        val pendingOpDao = FakePendingOpDao()
        val repository = TaskRepositoryImpl(FakeTaskDao(), FakeProjectDao(FakeTaskDao()), pendingOpDao)

        repository.delete("ausente")

        assertThat(pendingOpDao.enqueued).isEmpty()
    }

    @Test
    fun `delete de tarefa ativa enfileira DELETE e chama cascadeDeleteTask`() = runTest {
        val taskDao = FakeTaskDao()
        val projectDao = FakeProjectDao(taskDao)
        val pendingOpDao = FakePendingOpDao()
        val repository = TaskRepositoryImpl(taskDao, projectDao, pendingOpDao)
        val task = sampleTask(status = TaskStatus.DOING)
        taskDao.storage[task.id] = TaskEntity.fromDomain(task)

        repository.delete(task.id)

        assertThat(taskDao.storage).doesNotContainKey(task.id)
        assertThat(pendingOpDao.enqueued.map { it.opType }).containsExactly("DELETE")
        assertThat(projectDao.deleteCalls).containsExactly(
            DeleteCall(task.id, task.projectId, true),
        )
    }

    @Test
    fun `completeAndCascade lanca TaskNotFoundException quando ausente`() = runTest {
        val repository = newRepository(FakeTaskDao())

        val thrown = runCatching { repository.completeAndCascade("ausente") }
            .exceptionOrNull()

        assertThat(thrown).isInstanceOf(TaskNotFoundException::class.java)
    }

    @Test
    fun `completeAndCascade retorna no-op quando ja DONE`() = runTest {
        val taskDao = FakeTaskDao()
        val pendingOpDao = FakePendingOpDao()
        val repository = TaskRepositoryImpl(taskDao, FakeProjectDao(taskDao), pendingOpDao)
        val done = sampleTask(status = TaskStatus.DONE)
        taskDao.storage[done.id] = TaskEntity.fromDomain(done)

        assertThat(repository.completeAndCascade(done.id)).isEqualTo(done)
        assertThat(pendingOpDao.enqueued).isEmpty()
    }

    @Test
    fun `completeAndCascade encadeia TODO ate DONE e chama cascadeCompleteTask`() = runTest {
        val taskDao = FakeTaskDao()
        val projectDao = FakeProjectDao(taskDao)
        val pendingOpDao = FakePendingOpDao()
        val repository = TaskRepositoryImpl(taskDao, projectDao, pendingOpDao)
        val todo = sampleTask(status = TaskStatus.TODO)
        taskDao.storage[todo.id] = TaskEntity.fromDomain(todo)

        repository.completeAndCascade(todo.id)

        assertThat(projectDao.completeCalls.map { it.taskId }).containsExactly(todo.id)
        assertThat(pendingOpDao.enqueued.map { it.opType }).containsExactly("UPDATE")
    }

    @Test
    fun `reopenAndCascade lanca TaskNotFoundException quando ausente`() = runTest {
        val repository = newRepository(FakeTaskDao())

        val thrown = runCatching { repository.reopenAndCascade("ausente", TaskStatus.DOING) }
            .exceptionOrNull()

        assertThat(thrown).isInstanceOf(TaskNotFoundException::class.java)
    }

    @Test
    fun `reopenAndCascade recusa reabrir tarefa que nao esta DONE`() = runTest {
        val taskDao = FakeTaskDao()
        val repository = newRepository(taskDao)
        val todo = sampleTask(status = TaskStatus.TODO)
        taskDao.storage[todo.id] = TaskEntity.fromDomain(todo)

        val thrown = runCatching { repository.reopenAndCascade(todo.id, TaskStatus.DOING) }
            .exceptionOrNull()

        assertThat(thrown).isInstanceOf(InvalidStateTransitionException::class.java)
    }

    @Test
    fun `observeCountByPriority preenche niveis 0 a 4 ausentes com zero`() = runTest {
        val taskDao = FakeTaskDao()
        taskDao.priorityCounts["p1"] = PriorityCountRow(priorityCode = 2, taskCount = 3)
        val repository = newRepository(taskDao)

        val counts = repository.observeCountByPriority("p1").first()

        assertThat(counts).hasSize(5)
        assertThat(counts.map { it.priorityCode }).containsExactly(0, 1, 2, 3, 4).inOrder()
        assertThat(counts[2].count).isEqualTo(3)
        assertThat(counts[0].count).isEqualTo(0)
        assertThat(counts[4].count).isEqualTo(0)
    }

    @Test
    fun `countActiveByProject delega para o DAO`() = runTest {
        val taskDao = FakeTaskDao()
        taskDao.activeCounts["p1"] = 7
        val repository = newRepository(taskDao)

        assertThat(repository.countActiveByProject("p1")).isEqualTo(7)
    }

    @Test
    fun `observeCompletionStats mapeia row para dominio`() = runTest {
        val taskDao = FakeTaskDao()
        taskDao.completionStats["p1"] = CompletionStatsRow(
            totalCount = 10,
            doneCount = 4,
            doneThisWeekCount = 2,
        )
        val repository = newRepository(taskDao)

        val stats = repository.observeCompletionStats("p1", 0L).first()

        assertThat(stats.totalCount).isEqualTo(10)
        assertThat(stats.doneCount).isEqualTo(4)
        assertThat(stats.doneThisWeekCount).isEqualTo(2)
    }

    private fun newRepository(taskDao: TaskDao): TaskRepositoryImpl =
        TaskRepositoryImpl(taskDao, FakeProjectDao(taskDao), FakePendingOpDao())

    private fun sampleTask(
        status: TaskStatus = TaskStatus.TODO,
        priority: TaskPriority = TaskPriority.MEDIUM,
        createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    ): Task = Task.create(
        projectId = "p1",
        title = "Tarefa",
        priority = priority,
        status = status,
        now = createdAt,
    )

    /** Registra uma chamada a `cascadeDeleteTask`. */
    private data class DeleteCall(val taskId: String, val projectId: String, val wasActive: Boolean)

    /** Registra uma chamada a `cascadeCompleteTask`. */
    private data class CompleteCall(
        val taskId: String,
        val newStatus: String,
        val projectId: String,
    )

    /**
     * Fake de [TaskDao]. Armazena tarefas por id e mantém mapas de
     * contagem/estatísticas que os testes podem preencher diretamente.
     */
    private class FakeTaskDao : TaskDao {
        val storage: MutableMap<String, TaskEntity> = mutableMapOf()
        val activeCounts: MutableMap<String, Int> = mutableMapOf()
        val priorityCounts: MutableMap<String, PriorityCountRow> = mutableMapOf()
        val completionStats: MutableMap<String, CompletionStatsRow> = mutableMapOf()

        override fun observeForProject(projectId: String): Flow<List<TaskEntity>> =
            MutableStateFlow(storage.values.filter { it.projectId == projectId })

        override fun observeAllForOwner(ownerId: String): Flow<List<TaskEntity>> =
            MutableStateFlow(storage.values.toList())

        override fun observeCountByPriority(ownerId: String): Flow<List<PriorityCountRow>> =
            MutableStateFlow(priorityCounts.values.toList())

        override fun observeCompletionStats(
            ownerId: String,
            weekStartMillis: Long,
        ): Flow<CompletionStatsRow> =
            MutableStateFlow(
                completionStats[ownerId] ?: CompletionStatsRow(),
            )

        override suspend fun findById(id: String): TaskEntity? = storage[id]

        override suspend fun countActiveByProject(projectId: String): Int =
            activeCounts[projectId] ?: 0

        override suspend fun insert(task: TaskEntity) {
            check(!storage.containsKey(task.id)) { "id duplicado" }
            storage[task.id] = task
        }

        override suspend fun update(task: TaskEntity) {
            storage[task.id] = task
        }

        override suspend fun updateStatus(id: String, status: String) {
            val current = storage[id] ?: return
            storage[id] = current.copy(status = status)
        }

        override suspend fun updateStatusAndCompletedAt(
            id: String,
            status: String,
            completedAt: Instant?,
        ) {
            val current = storage[id] ?: return
            storage[id] = current.copy(status = status, completedAt = completedAt)
        }

        override suspend fun deleteById(id: String) {
            storage.remove(id)
        }
    }

    /**
     * Fake de [ProjectDao] que apenas registra as chamadas de cascata.
     * As cascatas delegam para o [TaskDao] compartilhado para simular
     * a escrita real do Room.
     */
    private class FakeProjectDao(private val taskDao: TaskDao) : ProjectDao {
        val deleteCalls: MutableList<DeleteCall> = mutableListOf()
        val completeCalls: MutableList<CompleteCall> = mutableListOf()
        val reopenCalls: MutableList<Triple<String, String, String>> = mutableListOf()
        val projects: MutableMap<String, ProjectEntity> = mutableMapOf()

        override fun observeAllForOwner(ownerId: String): Flow<List<ProjectEntity>> =
            MutableStateFlow(projects.values.filter { it.ownerId == ownerId })

        override fun searchProjects(
            ownerId: String,
            query: String,
            tagId: String?,
            sort: String,
        ): Flow<List<ProjectEntity>> = MutableStateFlow(emptyList())

        override suspend fun findById(id: String): ProjectEntity? = projects[id]

        override fun observeTagsFor(projectId: String): Flow<List<TagEntity>> =
            MutableStateFlow(emptyList())

        override suspend fun insert(project: ProjectEntity) {
            projects[project.id] = project
        }

        override suspend fun update(project: ProjectEntity) {
            projects[project.id] = project
        }

        override suspend fun deleteById(id: String) {
            projects.remove(id)
        }

        override suspend fun insertProjectTags(refs: List<ProjectTagCrossRef>) = Unit

        override suspend fun clearProjectTags(projectId: String) = Unit

        override suspend fun replaceProjectTags(projectId: String, tagIds: List<String>) = Unit

        override suspend fun updateIsCompleted(id: String, isCompleted: Boolean) {
            val current = projects[id] ?: return
            projects[id] = current.copy(isCompleted = isCompleted)
        }

        override suspend fun cascadeCompleteTask(
            taskDao: TaskDao,
            taskId: String,
            newStatus: String,
            completedAt: Instant,
            projectId: String,
        ) {
            completeCalls += CompleteCall(taskId, newStatus, projectId)
            taskDao.updateStatusAndCompletedAt(taskId, newStatus, completedAt)
        }

        override suspend fun cascadeReopenTask(
            taskDao: TaskDao,
            taskId: String,
            newStatus: String,
            projectId: String,
        ) {
            reopenCalls += Triple(taskId, newStatus, projectId)
            taskDao.updateStatusAndCompletedAt(taskId, newStatus, null)
        }

        override suspend fun cascadeDeleteTask(
            taskDao: TaskDao,
            taskId: String,
            projectId: String,
            wasActive: Boolean,
        ) {
            deleteCalls += DeleteCall(taskId, projectId, wasActive)
            taskDao.deleteById(taskId)
        }
    }

    /**
     * Fake de [PendingOpDao] que captura a ordem de enfileiramento
     * para validar a escrita dual.
     */
    private class FakePendingOpDao : PendingOpDao {
        val enqueued: MutableList<PendingOpEntity> = mutableListOf()
        private var nextId = 1L

        override suspend fun nextBatch(limit: Int): List<PendingOpEntity> =
            enqueued.take(limit)

        override suspend fun count(): Int = enqueued.size

        override fun observeCount(): Flow<Int> = MutableStateFlow(enqueued.size)

        override suspend fun insert(op: PendingOpEntity): Long {
            val withId = op.copy(id = nextId++)
            enqueued += withId
            return withId.id
        }

        override suspend fun update(op: PendingOpEntity) {
            val index = enqueued.indexOfFirst { it.id == op.id }
            if (index >= 0) enqueued[index] = op
        }

        override suspend fun deleteById(id: Long) {
            enqueued.removeAll { it.id == id }
        }

        override suspend fun deleteForEntity(entityType: String, entityId: String) {
            enqueued.removeAll { it.entityType == entityType && it.entityId == entityId }
        }

        override suspend fun markAttempt(opId: Long) {
            val index = enqueued.indexOfFirst { it.id == opId }
            if (index >= 0) {
                enqueued[index] = enqueued[index].copy(attempts = enqueued[index].attempts + 1)
            }
        }

        override suspend fun findById(id: Long): PendingOpEntity? =
            enqueued.firstOrNull { it.id == id }

        override suspend fun enqueueInTx(op: PendingOpEntity, write: suspend () -> Unit) {
            write()
            insert(op)
        }
    }
}
