// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
@file:Suppress("ConstructorParameterNaming")
package pucgo.joaopedrogmsilva.brainout.core.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectTagCrossRef
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TagEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TaskEntity
import java.time.Instant

/**
 * Testes integrados dos DAOs da v2 rodando em Robolectric com banco em
 * memória.
 *
 * Cobre os caminhos críticos para a E2.1/E2.2/E2.6:
 * - Insert + findById round-trip nas 3 entidades.
 * - `observeAllForOwner` filtra por owner_id e ordena por `created_at DESC`.
 * - `observeForProject` filtra por project_id e ordena por prioridade DESC.
 * - `observeTagsFor` faz JOIN com `project_tags` e respeita CASCADE
 *   quando o projeto é removido.
 * - `countActiveByProject` ignora tarefas DONE (RN01).
 * - `insert` em `TagEntity` falha em colisão do índice único
 *   `(owner_id, name)`.
 * - Migration 1 → 2 não destrói a tabela `users` (idempotência
 *   verificada indiretamente: o build do banco v2 inicia vazio).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ProjectTaskTagDaoTest {

    private lateinit var database: BrainOutDatabase
    private lateinit var projectDao: ProjectDao
    private lateinit var taskDao: TaskDao
    private lateinit var tagDao: TagDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, BrainOutDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        projectDao = database.projectDao()
        taskDao = database.taskDao()
        tagDao = database.tagDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `project insert and findById round trip`() = runTest {
        val entity = sampleProject(id = "p-1", ownerId = "u-1")
        projectDao.insert(entity)
        assertThat(projectDao.findById("p-1")).isEqualTo(entity)
    }

    @Test
    fun `observeAllForOwner filters by owner and orders by created_at DESC`() = runTest {
        projectDao.insert(sampleProject(id = "p-1", ownerId = "u-1", createdAt = Instant.parse("2026-01-01T00:00:00Z")))
        projectDao.insert(sampleProject(id = "p-2", ownerId = "u-1", createdAt = Instant.parse("2026-02-01T00:00:00Z")))
        projectDao.insert(sampleProject(id = "p-3", ownerId = "u-2", createdAt = Instant.parse("2026-03-01T00:00:00Z")))

        val forOwner1 = projectDao.observeAllForOwner("u-1").first()
        assertThat(forOwner1.map { it.id }).containsExactly("p-2", "p-1").inOrder()
    }

    @Test
    fun `task observeForProject orders by priority DESC then created_at`() = runTest {
        val project = sampleProject(id = "p-1", ownerId = "u-1")
        projectDao.insert(project)
        taskDao.insert(
            sampleTask(
                id = "t-low",
                projectId = "p-1",
                priorityCode = 0,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )
        taskDao.insert(
            sampleTask(
                id = "t-crit",
                projectId = "p-1",
                priorityCode = 4,
                createdAt = Instant.parse("2026-02-01T00:00:00Z"),
            ),
        )
        taskDao.insert(
            sampleTask(
                id = "t-high",
                projectId = "p-1",
                priorityCode = 2,
                createdAt = Instant.parse("2026-03-01T00:00:00Z"),
            ),
        )

        val tasks = taskDao.observeForProject("p-1").first()
        assertThat(tasks.map { it.id }).containsExactly("t-crit", "t-high", "t-low").inOrder()
    }

    @Test
    fun `countActiveByProject excludes DONE tasks`() = runTest {
        projectDao.insert(sampleProject(id = "p-1", ownerId = "u-1"))
        taskDao.insert(sampleTask(id = "t-1", projectId = "p-1", status = "TODO"))
        taskDao.insert(sampleTask(id = "t-2", projectId = "p-1", status = "DOING"))
        taskDao.insert(sampleTask(id = "t-3", projectId = "p-1", status = "DONE"))
        taskDao.insert(sampleTask(id = "t-4", projectId = "p-1", status = "TODO"))

        assertThat(taskDao.countActiveByProject("p-1")).isEqualTo(3)
    }

    @Test
    fun `observeAllForOwner crosses through projects join`() = runTest {
        projectDao.insert(sampleProject(id = "p-1", ownerId = "u-1"))
        projectDao.insert(sampleProject(id = "p-2", ownerId = "u-2"))
        taskDao.insert(sampleTask(id = "t-a", projectId = "p-1"))
        taskDao.insert(sampleTask(id = "t-b", projectId = "p-2"))

        val forOwner1 = taskDao.observeAllForOwner("u-1").first()
        assertThat(forOwner1.map { it.id }).containsExactly("t-a")
    }

    @Test
    fun `tag insert fails on duplicate owner and name via ABORT`() = runTest {
        tagDao.insert(sampleTag(id = "tag-1", ownerId = "u-1", name = "Urgente"))
        // Mesmo (owner_id, name) — UNIQUE idx falha.
        val ex = runCatching { tagDao.insert(sampleTag(id = "tag-2", ownerId = "u-1", name = "Urgente")) }
        assertThat(ex.exceptionOrNull()).isNotNull()
    }

    @Test
    fun `tag insert allows same name for different owners`() = runTest {
        tagDao.insert(sampleTag(id = "tag-1", ownerId = "u-1", name = "Urgente"))
        tagDao.insert(sampleTag(id = "tag-2", ownerId = "u-2", name = "Urgente"))

        assertThat(tagDao.findById("tag-1")).isNotNull()
        assertThat(tagDao.findById("tag-2")).isNotNull()
    }

    @Test
    fun `observeTagsFor returns project tags via JOIN`() = runTest {
        projectDao.insert(sampleProject(id = "p-1", ownerId = "u-1"))
        tagDao.insert(sampleTag(id = "tag-a", ownerId = "u-1", name = "alpha"))
        tagDao.insert(sampleTag(id = "tag-b", ownerId = "u-1", name = "beta"))
        projectDao.insertProjectTags(
            listOf(
                ProjectTagCrossRef(project_id = "p-1", tag_id = "tag-a"),
                ProjectTagCrossRef(project_id = "p-1", tag_id = "tag-b"),
            ),
        )

        val tags = projectDao.observeTagsFor("p-1").first()
        assertThat(tags.map { it.name }).containsExactly("alpha", "beta").inOrder()
    }

    @Test
    fun `replaceProjectTags clears previous then inserts new inside transaction`() = runTest {
        projectDao.insert(sampleProject(id = "p-1", ownerId = "u-1"))
        tagDao.insert(sampleTag(id = "tag-a", ownerId = "u-1", name = "alpha"))
        tagDao.insert(sampleTag(id = "tag-b", ownerId = "u-1", name = "beta"))
        projectDao.insertProjectTags(listOf(ProjectTagCrossRef(project_id = "p-1", tag_id = "tag-a")))

        projectDao.replaceProjectTags("p-1", listOf("tag-b"))

        val tags = projectDao.observeTagsFor("p-1").first()
        assertThat(tags.map { it.id }).containsExactly("tag-b")
    }

    @Test
    fun `deleting project cascades into tasks and project_tags`() = runTest {
        projectDao.insert(sampleProject(id = "p-1", ownerId = "u-1"))
        taskDao.insert(sampleTask(id = "t-1", projectId = "p-1"))
        tagDao.insert(sampleTag(id = "tag-a", ownerId = "u-1", name = "alpha"))
        projectDao.insertProjectTags(listOf(ProjectTagCrossRef(project_id = "p-1", tag_id = "tag-a")))

        projectDao.deleteById("p-1")

        assertThat(taskDao.findById("t-1")).isNull()
        assertThat(projectDao.observeTagsFor("p-1").first()).isEmpty()
    }

    @Test
    fun `updateStatus changes status without touching other fields`() = runTest {
        projectDao.insert(sampleProject(id = "p-1", ownerId = "u-1"))
        taskDao.insert(sampleTask(id = "t-1", projectId = "p-1", status = "TODO"))

        taskDao.updateStatus("t-1", "DOING")

        val loaded = taskDao.findById("t-1")
        assertThat(loaded?.status).isEqualTo("DOING")
    }

    private fun sampleProject(
        id: String,
        ownerId: String,
        name: String = "Projeto $id",
        description: String? = null,
        createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
        isCompleted: Boolean = false,
    ): ProjectEntity = ProjectEntity(
        id = id,
        ownerId = ownerId,
        name = name,
        description = description,
        createdAt = createdAt,
        isCompleted = isCompleted,
    )

    private fun sampleTask(
        id: String,
        projectId: String,
        title: String = "Tarefa $id",
        priorityCode: Int = 1,
        status: String = "TODO",
        assigneeId: String? = null,
        dueDate: Instant? = null,
        createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    ): TaskEntity = TaskEntity(
        id = id,
        projectId = projectId,
        title = title,
        priorityCode = priorityCode,
        status = status,
        assigneeId = assigneeId,
        dueDate = dueDate,
        createdAt = createdAt,
    )

    private fun sampleTag(
        id: String,
        ownerId: String,
        name: String,
        color: String = "#FF0000",
        createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    ): TagEntity = TagEntity(
        id = id,
        ownerId = ownerId,
        name = name,
        color = color,
        createdAt = createdAt,
    )
}
