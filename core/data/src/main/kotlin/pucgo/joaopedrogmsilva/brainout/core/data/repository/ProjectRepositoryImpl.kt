// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pucgo.joaopedrogmsilva.brainout.core.data.local.LocalTransaction
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.ProjectDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.TagDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectEntity
import pucgo.joaopedrogmsilva.brainout.core.data.util.logDebug
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TagNotFoundException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TagOwnershipException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BrainOut:ProjectRepo"

/** Persistência local de project, com transações Room para escritas e cascatas. */
@Singleton
class ProjectRepositoryImpl
    @Inject
    constructor(
        private val projectDao: ProjectDao,
        private val tagDao: TagDao,
        private val localTransaction: LocalTransaction,
    ) : ProjectRepository {
        override fun observeAllForOwner(ownerId: String): Flow<List<Project>> =
            projectDao.observeAllForOwner(ownerId).map { rows -> rows.map { it.toDomain() } }

        override fun observeSearch(
            ownerId: String,
            query: String,
            tagId: String?,
            sortOrder: SortOrder,
        ): Flow<List<Project>> =
            projectDao
                .searchProjects(
                    ownerId = ownerId,
                    query = query,
                    tagId = tagId,
                    sort = sortOrder.toStorageKey(),
                ).map { rows -> rows.map { it.toDomain() } }

        override suspend fun findById(id: String): Project? = projectDao.findById(id)?.toDomain()

        override fun observeTagsFor(projectId: String): Flow<List<Tag>> =
            projectDao.observeTagsFor(projectId).map { rows -> rows.map { it.toDomain() } }

        override suspend fun create(
            project: Project,
            tagIds: List<String>,
        ): Project {
            logDebug(TAG, "Criando projeto id=${project.id}, name=${project.name}, tags=${tagIds.size}")
            localTransaction.run {
                projectDao.insert(ProjectEntity.fromDomain(project))
                validateTagOwnership(tagIds, project.ownerId)
                projectDao.replaceProjectTags(project.id, tagIds)
            }
            return project
        }

        override suspend fun update(
            project: Project,
            tagIds: List<String>,
        ): Project {
            logDebug(TAG, "Atualizando projeto id=${project.id}, name=${project.name}, tagsCount=${tagIds.size}")
            localTransaction.run {
                projectDao.update(ProjectEntity.fromDomain(project))
                validateTagOwnership(tagIds, project.ownerId)
                projectDao.replaceProjectTags(project.id, tagIds)
            }
            return project
        }

        override suspend fun delete(id: String) {
            logDebug(TAG, "Deletando projeto id=$id")
            localTransaction.run {
                projectDao.deleteById(id)
            }
        }

        private suspend fun validateTagOwnership(
            tagIds: List<String>,
            ownerId: String,
        ) {
            tagIds.forEach { tagId ->
                val tag = tagDao.findById(tagId) ?: throw TagNotFoundException(tagId)
                if (tag.ownerId != ownerId) throw TagOwnershipException(tagId)
            }
        }
    }
