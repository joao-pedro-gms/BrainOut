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
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.TaskDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.PendingOpEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectTagCrossRef
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TagEntity
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TagNotFoundException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TagOwnershipException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository

/**
 * Testes diretos do [ProjectRepositoryImpl].
 *
 * Cobre a escrita dual (Room + `pending_ops`), a validação de
 * propriedade das tags via `validateTagOwnership` (RN05) e os fluxos
 * de observação reativa.
 */
class ProjectRepositoryImplTest {

    private val ownerId: String = "u1"

    @Test
    fun `findById mapeia entidade para dominio`() = runTest {
        val projectDao = FakeProjectDao()
        val project = sampleProject()
        projectDao.projects[project.id] = ProjectEntity.fromDomain(project)
        val repository = newRepository(projectDao)

        assertThat(repository.findById(project.id)).isEqualTo(project)
    }

    @Test
    fun `findById retorna null quando ausente`() = runTest {
        assertThat(newRepository().findById("ausente")).isNull()
    }

    @Test
    fun `observeAllForOwner mapeia entidades para dominio`() = runTest {
        val projectDao = FakeProjectDao()
        val p1 = sampleProject(id = "p1", name = "P1")
        val p2 = sampleProject(id = "p2", name = "P2")
        projectDao.projects[p1.id] = ProjectEntity.fromDomain(p1)
        projectDao.projects[p2.id] = ProjectEntity.fromDomain(p2)
        val repository = newRepository(projectDao)

        val result = repository.observeAllForOwner(ownerId).first()

        assertThat(result).containsExactly(p1, p2)
    }

    @Test
    fun `observeTagsFor mapeia entidades para dominio`() = runTest {
        val projectDao = FakeProjectDao()
        val tag = sampleTag(id = "t1", ownerId = ownerId)
        projectDao.tags[tag.id] = TagEntity.fromDomain(tag)
        projectDao.associations["p1"] = mutableListOf(tag.id)
        val repository = newRepository(projectDao)

        assertThat(repository.observeTagsFor("p1").first()).containsExactly(tag)
    }

    @Test
    fun `create persiste projeto e enfileira op CREATE`() = runTest {
        val projectDao = FakeProjectDao()
        val pendingOpDao = FakePendingOpDao()
        val repository = newRepository(projectDao, pendingOpDao)
        val project = sampleProject()

        repository.create(project, emptyList())

        assertThat(projectDao.projects[project.id]?.toDomain()).isEqualTo(project)
        assertThat(pendingOpDao.enqueued.map { it.opType }).containsExactly("CREATE")
    }

    @Test
    fun `create rejeita tag inexistente antes de persistir projeto`() = runTest {
        val projectDao = FakeProjectDao()
        val tagDao = FakeTagDao()
        val pendingOpDao = FakePendingOpDao()
        val repository = newRepository(projectDao, pendingOpDao, tagDao)
        val project = sampleProject()

        val thrown = runCatching { repository.create(project, listOf("tag-ausente")) }
            .exceptionOrNull()

        assertThat(thrown).isInstanceOf(TagNotFoundException::class.java)
        // Escrita dual aborta: a op não é enfileirada quando a
        // validação de tag lança (write() lança antes de insert).
        assertThat(pendingOpDao.enqueued).isEmpty()
    }

    @Test
    fun `create rejeita tag de outro owner antes de persistir`() = runTest {
        val projectDao = FakeProjectDao()
        val tagDao = FakeTagDao()
        val pendingOpDao = FakePendingOpDao()
        val foreignTag = sampleTag(id = "t1", ownerId = "outro-owner")
        tagDao.tags[foreignTag.id] = TagEntity.fromDomain(foreignTag)
        val repository = newRepository(projectDao, pendingOpDao, tagDao)
        val project = sampleProject()

        val thrown = runCatching { repository.create(project, listOf(foreignTag.id)) }
            .exceptionOrNull()

        assertThat(thrown).isInstanceOf(TagOwnershipException::class.java)
        assertThat(pendingOpDao.enqueued).isEmpty()
    }

    @Test
    fun `create substitui associacoes de tags atomicamente`() = runTest {
        val projectDao = FakeProjectDao()
        val tagDao = FakeTagDao()
        val pendingOpDao = FakePendingOpDao()
        val repository = newRepository(projectDao, pendingOpDao, tagDao)
        val project = sampleProject()
        val tagA = sampleTag(id = "ta", ownerId = ownerId)
        val tagB = sampleTag(id = "tb", ownerId = ownerId)
        tagDao.tags[tagA.id] = TagEntity.fromDomain(tagA)
        tagDao.tags[tagB.id] = TagEntity.fromDomain(tagB)

        repository.create(project, listOf(tagA.id, tagB.id))

        assertThat(projectDao.associations[project.id])
            .containsExactly(tagA.id, tagB.id)
            .inOrder()
        assertThat(pendingOpDao.enqueued.map { it.opType }).containsExactly("CREATE")
    }

    @Test
    fun `update persiste projeto e enfileira op UPDATE`() = runTest {
        val projectDao = FakeProjectDao()
        val pendingOpDao = FakePendingOpDao()
        val repository = newRepository(projectDao, pendingOpDao)
        val project = sampleProject()
        projectDao.projects[project.id] = ProjectEntity.fromDomain(project)

        repository.update(project, emptyList())

        assertThat(pendingOpDao.enqueued.map { it.opType }).containsExactly("UPDATE")
    }

    @Test
    fun `update rejeita tag de outro owner antes de persistir`() = runTest {
        val projectDao = FakeProjectDao()
        val tagDao = FakeTagDao()
        val foreignTag = sampleTag(id = "t1", ownerId = "outro-owner")
        tagDao.tags[foreignTag.id] = TagEntity.fromDomain(foreignTag)
        val repository = newRepository(projectDao, tagDao = tagDao)
        val project = sampleProject()
        projectDao.projects[project.id] = ProjectEntity.fromDomain(project)

        val thrown = runCatching { repository.update(project, listOf(foreignTag.id)) }
            .exceptionOrNull()

        assertThat(thrown).isInstanceOf(TagOwnershipException::class.java)
    }

    @Test
    fun `delete remove projeto e enfileira op DELETE`() = runTest {
        val projectDao = FakeProjectDao()
        val pendingOpDao = FakePendingOpDao()
        val repository = newRepository(projectDao, pendingOpDao)
        val project = sampleProject()
        projectDao.projects[project.id] = ProjectEntity.fromDomain(project)

        repository.delete(project.id)

        assertThat(projectDao.projects).doesNotContainKey(project.id)
        assertThat(pendingOpDao.enqueued.map { it.opType }).containsExactly("DELETE")
    }

    @Test
    fun `observeSearch delega query ao DAO com SortOrder toStorageKey`() = runTest {
        val projectDao = FakeProjectDao()
        val repository = newRepository(projectDao)

        repository.observeSearch(ownerId, "query", "tag-1", SortOrder.NameAsc)

        assertThat(projectDao.lastSearch).isNotNull()
        val search = projectDao.lastSearch!!
        assertThat(search[0]).isEqualTo(ownerId)
        assertThat(search[1]).isEqualTo("query")
        assertThat(search[2]).isEqualTo("tag-1")
        assertThat(search[3]).isEqualTo("nameasc")
    }

    private fun newRepository(
        projectDao: ProjectDao = FakeProjectDao(),
        pendingOpDao: PendingOpDao = FakePendingOpDao(),
        tagDao: TagDao = FakeTagDao(),
    ): ProjectRepository = ProjectRepositoryImpl(projectDao, tagDao, pendingOpDao)

    private fun sampleProject(
        id: String = "p1",
        name: String = "Projeto",
        owner: String = ownerId,
    ): Project = Project(
        id = id,
        name = name,
        description = null,
        ownerId = owner,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        isCompleted = false,
    )

    private fun sampleTag(
        id: String = "t1",
        ownerId: String = "u1",
        name: String = "Tag",
    ): Tag = Tag(
        id = id,
        ownerId = ownerId,
        name = name,
        color = "#6750A4",
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    /**
     * Fake de [ProjectDao] usado por [ProjectRepositoryImpl]. Registra
     * as últimas chamadas para asserções e mantém a tabela de projetos.
     */
    private class FakeProjectDao : ProjectDao {
        val projects: MutableMap<String, ProjectEntity> = mutableMapOf()
        val tags: MutableMap<String, TagEntity> = mutableMapOf()
        val associations: MutableMap<String, MutableList<String>> = mutableMapOf()
        var lastSearch: Array<Any?>? = null

        override fun observeAllForOwner(ownerId: String): Flow<List<ProjectEntity>> =
            MutableStateFlow(projects.values.filter { it.ownerId == ownerId })

        override fun searchProjects(
            ownerId: String,
            query: String,
            tagId: String?,
            sort: String,
        ): Flow<List<ProjectEntity>> {
            lastSearch = arrayOf(ownerId, query, tagId, sort)
            return MutableStateFlow(emptyList())
        }

        override suspend fun findById(id: String): ProjectEntity? = projects[id]

        override fun observeTagsFor(projectId: String): Flow<List<TagEntity>> =
            MutableStateFlow(
                associations[projectId].orEmpty().mapNotNull { tags[it] },
            )

        override suspend fun insert(project: ProjectEntity) {
            projects[project.id] = project
        }

        override suspend fun update(project: ProjectEntity) {
            projects[project.id] = project
        }

        override suspend fun deleteById(id: String) {
            projects.remove(id)
        }

        override suspend fun insertProjectTags(refs: List<ProjectTagCrossRef>) {
            refs.forEach { ref ->
                associations.getOrPut(ref.project_id) { mutableListOf() } += ref.tag_id
            }
        }

        override suspend fun clearProjectTags(projectId: String) {
            associations.remove(projectId)
        }

        override suspend fun replaceProjectTags(projectId: String, tagIds: List<String>) {
            clearProjectTags(projectId)
            if (tagIds.isNotEmpty()) {
                insertProjectTags(tagIds.map { ProjectTagCrossRef(project_id = projectId, tag_id = it) })
            }
        }

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
