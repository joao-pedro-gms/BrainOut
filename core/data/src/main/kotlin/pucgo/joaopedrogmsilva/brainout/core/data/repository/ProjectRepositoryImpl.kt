// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.ProjectDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.TagDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectEntity
import pucgo.joaopedrogmsilva.brainout.core.data.session.AccountAccess
import pucgo.joaopedrogmsilva.brainout.core.data.util.logDebug
import pucgo.joaopedrogmsilva.brainout.core.domain.error.ProjectNotFoundException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TagNotFoundException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TagOwnershipException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
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
        private val access: AccountAccess,
    ) : ProjectRepository {
        override fun observeAllForOwner(ownerId: String): Flow<List<Project>> =
            access.observeOwner(ownerId, emptyList()) {
                projectDao.observeAllForOwner(ownerId).map { rows ->
                    rows.map { it.toDomain() }
                }
            }

        override fun observeSearch(
            ownerId: String,
            query: String,
            tagId: String?,
            sortOrder: SortOrder,
        ): Flow<List<Project>> =
            access.observeOwner(ownerId, emptyList()) {
                projectDao
                    .searchProjects(
                        ownerId = ownerId,
                        query = query,
                        tagId = tagId,
                        sort = sortOrder.toStorageKey(),
                    ).map { rows -> rows.map { it.toDomain() } }
            }

        override suspend fun findById(id: String): Project? =
            projectDao
                .findById(id)
                ?.takeIf {
                    access.owns(it.ownerId)
                }?.toDomain()

        override suspend fun create(
            project: Project,
            tagIds: List<String>,
        ): Project {
            logDebug(TAG, "Criando projeto id=${project.id}, name=${project.name}, tags=${tagIds.size}")
            return persistProject(project, tagIds) {
                projectDao.insert(ProjectEntity.fromDomain(project))
            }
        }

        override suspend fun update(
            project: Project,
            tagIds: List<String>,
        ): Project {
            logDebug(TAG, "Atualizando projeto id=${project.id}, name=${project.name}, tagsCount=${tagIds.size}")
            return persistProject(project, tagIds) {
                val persisted = projectDao.findById(project.id) ?: throw ProjectNotFoundException(project.id)
                access.requireOwner(persisted.ownerId)
                projectDao.update(ProjectEntity.fromDomain(project))
            }
        }

        override suspend fun delete(id: String) {
            logDebug(TAG, "Deletando projeto id=$id")
            val project = findById(id) ?: throw ProjectNotFoundException(id)
            access.write(project.ownerId) {
                projectDao.deleteById(id)
            }
        }

        /**
         * Grava o projeto e substitui o conjunto de tags na **mesma**
         * transação: ou a entidade e as associações são persistidas, ou
         * nada é. [writeEntity] faz a escrita da entidade — `insert` na
         * criação, `update` na edição.
         */
        private suspend fun persistProject(
            project: Project,
            tagIds: List<String>,
            writeEntity: suspend () -> Unit,
        ): Project {
            access.write(project.ownerId) {
                writeEntity()
                validateTagOwnership(tagIds, project.ownerId)
                projectDao.replaceProjectTags(project.id, tagIds)
            }
            return project
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
