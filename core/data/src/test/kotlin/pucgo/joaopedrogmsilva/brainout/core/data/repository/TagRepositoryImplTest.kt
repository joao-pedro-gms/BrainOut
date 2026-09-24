// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.PendingOpDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.ProjectDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.TagDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.PendingOpEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectTagCrossRef
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TagEntity
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TagRepository

/**
 * Testes diretos do [TagRepositoryImpl] (gap Kover 60% do módulo).
 *
 * Cobre a escrita dual (Room + `pending_ops`), o payload de sync com
 * `id` cliente-supplied (R6 — P0-4) e o `observeByProjectIds`
 * introduzido no P2 de tags-por-projeto.
 */
class TagRepositoryImplTest {

    @Test
    fun `findById mapeia entidade para dominio`() = runTest {
        val tagDao = FakeTagDao()
        val tag = sampleTag()
        tagDao.tags[tag.id] = TagEntity.fromDomain(tag)
        val repository = newRepository(tagDao)

        assertThat(repository.findById(tag.id)).isEqualTo(tag)
    }

    @Test
    fun `findById retorna null quando ausente`() = runTest {
        assertThat(newRepository().findById("ausente")).isNull()
    }

    @Test
    fun `findByIds mapeia entidades para dominio`() = runTest {
        val tagDao = FakeTagDao()
        val t1 = sampleTag(id = "t1", name = "A")
        val t2 = sampleTag(id = "t2", name = "B")
        tagDao.tags[t1.id] = TagEntity.fromDomain(t1)
        tagDao.tags[t2.id] = TagEntity.fromDomain(t2)
        val repository = newRepository(tagDao)

        assertThat(repository.findByIds(listOf("t1", "t2"))).containsExactly(t1, t2)
    }

    @Test
    fun `observeForOwner mapeia entidades para dominio`() = runTest {
        val tagDao = FakeTagDao()
        val tag = sampleTag()
        tagDao.tags[tag.id] = TagEntity.fromDomain(tag)
        val repository = newRepository(tagDao)

        assertThat(repository.observeForOwner("u1").first()).containsExactly(tag)
    }

    @Test
    fun `observeByProjectIds mapeia projeto para suas tags`() = runTest {
        val projectDao = FakeProjectDao()
        val t1 = sampleTag(id = "t1", name = "Urgente")
        val t2 = sampleTag(id = "t2", name = "Backlog")
        projectDao.tagAssociations["p1"] = listOf(t1.id, t2.id)
        projectDao.tagAssociations["p2"] = listOf(t1.id)
        projectDao.tags[t1.id] = TagEntity.fromDomain(t1)
        projectDao.tags[t2.id] = TagEntity.fromDomain(t2)
        val repository = newRepository(projectDao = projectDao)

        val map = repository.observeByProjectIds(setOf("p1", "p2")).first()

        assertThat(map.keys).containsExactly("p1", "p2")
        assertThat(map["p1"]).containsExactly(t1, t2)
        assertThat(map["p2"]).containsExactly(t1)
    }

    @Test
    fun `create persiste tag e enfileira op CREATE com id cliente-supplied`() = runTest {
        val tagDao = FakeTagDao()
        val pendingOpDao = FakePendingOpDao()
        val repository = newRepository(tagDao, pendingOpDao = pendingOpDao)
        val tag = sampleTag()

        repository.create(tag)

        assertThat(tagDao.tags[tag.id]?.toDomain()).isEqualTo(tag)
        val op = pendingOpDao.enqueued.single()
        assertThat(op.opType).isEqualTo("CREATE")
        assertThat(op.entityType).isEqualTo("TAG")
        assertThat(op.entityId).isEqualTo(tag.id)
        // P0-4 (R6): o payload de CREATE carrega o id do cliente.
        assertThat(op.payload).contains("\"id\":\"${tag.id}\"")
    }

    @Test
    fun `delete remove tag e enfileira op DELETE`() = runTest {
        val tagDao = FakeTagDao()
        val pendingOpDao = FakePendingOpDao()
        val repository = newRepository(tagDao, pendingOpDao = pendingOpDao)
        val tag = sampleTag()
        tagDao.tags[tag.id] = TagEntity.fromDomain(tag)

        repository.delete(tag.id)

        assertThat(tagDao.tags).doesNotContainKey(tag.id)
        assertThat(pendingOpDao.enqueued.map { it.opType }).containsExactly("DELETE")
    }

    private fun newRepository(
        tagDao: TagDao = FakeTagDao(),
        projectDao: ProjectDao = FakeProjectDao(),
        pendingOpDao: PendingOpDao = FakePendingOpDao(),
    ): TagRepository = TagRepositoryImpl(tagDao, projectDao, pendingOpDao)

    private fun sampleTag(
        id: String = "t1",
        ownerId: String = "u1",
        name: String = "Urgente",
    ): Tag = Tag(
        id = id,
        ownerId = ownerId,
        name = name,
        color = "#6750A4",
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    /** Fake mínimo de [TagDao]. */
    private class FakeTagDao : TagDao {
        val tags: MutableMap<String, TagEntity> = mutableMapOf()

        override fun observeForOwner(ownerId: String): Flow<List<TagEntity>> =
            MutableStateFlow(tags.values.filter { it.ownerId == ownerId })

        override suspend fun findById(id: String): TagEntity? = tags[id]

        override suspend fun findByIds(ids: List<String>): List<TagEntity> =
            ids.mapNotNull { tags[it] }

        override suspend fun insert(tag: TagEntity): Long {
            tags[tag.id] = tag
            return 1L
        }

        override suspend fun deleteById(id: String) {
            tags.remove(id)
        }
    }

    /**
     * Fake de [ProjectDao] focado em `observeTagsFor` (usado pelo
     * `observeByProjectIds`).
     */
    private class FakeProjectDao : ProjectDao {
        val tags: MutableMap<String, TagEntity> = mutableMapOf()
        val tagAssociations: MutableMap<String, List<String>> = mutableMapOf()

        override fun observeAllForOwner(ownerId: String): Flow<List<ProjectEntity>> =
            MutableStateFlow(emptyList())

        override fun searchProjects(
            ownerId: String,
            query: String,
            tagId: String?,
            sort: String,
        ): Flow<List<ProjectEntity>> = MutableStateFlow(emptyList())

        override suspend fun findById(id: String): ProjectEntity? = null

        override fun observeTagsFor(projectId: String): Flow<List<TagEntity>> =
            MutableStateFlow(tagAssociations[projectId].orEmpty().mapNotNull { tags[it] })

        override suspend fun insert(project: ProjectEntity) = Unit

        override suspend fun update(project: ProjectEntity) = Unit

        override suspend fun deleteById(id: String) = Unit

        override suspend fun insertProjectTags(refs: List<ProjectTagCrossRef>) = Unit

        override suspend fun clearProjectTags(projectId: String) = Unit

        override suspend fun replaceProjectTags(projectId: String, tagIds: List<String>) = Unit

        override suspend fun updateIsCompleted(id: String, isCompleted: Boolean) = Unit

        override suspend fun cascadeCompleteTask(
            taskDao: TaskDao,
            taskId: String,
            newStatus: String,
            completedAt: Instant,
            projectId: String,
        ) = Unit

        override suspend fun cascadeReopenTask(
            taskDao: TaskDao,
            taskId: String,
            newStatus: String,
            projectId: String,
        ) = Unit

        override suspend fun cascadeDeleteTask(
            taskDao: TaskDao,
            taskId: String,
            projectId: String,
            wasActive: Boolean,
        ) = Unit
    }

    /** Fake mínimo de [PendingOpDao]. */
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
