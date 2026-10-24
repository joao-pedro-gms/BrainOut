// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.ProjectDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.TaskDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectTagCrossRef
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TagEntity
import java.time.Instant

/**
 * Fake de [ProjectDao] compartilhado pelos testes de repositório.
 *
 * A interface tem 13 métodos e dois testes mantinham uma cópia cada —
 * um arranjo em que um método novo na interface quebrava os dois ao
 * mesmo tempo. Aqui a implementação mora uma vez.
 *
 * As três operações de cascata são `open`: o
 * [TaskRepositoryImplTest] precisa que elas **atuem** (o registro das
 * chamadas é o que o teste observa), enquanto o
 * [ProjectRepositoryImplTest] só precisa que existam. A implementação
 * padrão de todas as outras operações é a mesma nos dois.
 */
internal open class FakeProjectDao : ProjectDao {
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

    override suspend fun updateIsCompleted(
        id: String,
        isCompleted: Boolean,
    ) {
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
