// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.PendingOpDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.ProjectDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.TagDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.PendingOpEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectSyncPayload
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.SyncEntityType
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.SyncOpType
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TagNotFoundException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TagOwnershipException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder

/**
 * Implementação Room de [ProjectRepository].
 *
 * Mantém o mapeamento entre [Project] (domínio) e [ProjectEntity] (Room).
 * Em [create] e [update], valida que cada `tagId` informado existe e
 * pertence ao mesmo `ownerId` do projeto antes de associar — caso
 * contrário lança [TagNotFoundException] ou `IllegalArgumentException`.
 * A substituição das tags é feita dentro de uma transação Room
 * ([ProjectDao.replaceProjectTags]) para garantir atomicidade.
 *
 * As remoções de associações `project_tags` ao apagar o projeto são
 * feitas em cascata pelo schema (FK com `ON DELETE CASCADE`).
 *
 * @property projectDao DAO de projetos injetado pelo Hilt via `DataModule`.
 * @property tagDao DAO de tags injetado pelo Hilt via `DataModule` — usado
 * para validar a propriedade das tags antes de associá-las.
 * @property pendingOpDao DAO da fila de sincronização (E3.3).
 */
@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val tagDao: TagDao,
    private val pendingOpDao: PendingOpDao,
) : ProjectRepository {

    override fun observeAllForOwner(ownerId: String): Flow<List<Project>> =
        projectDao.observeAllForOwner(ownerId).map { rows -> rows.map { it.toDomain() } }

    override fun observeSearch(
        ownerId: String,
        query: String,
        tagId: String?,
        sortOrder: SortOrder,
    ): Flow<List<Project>> = projectDao.searchProjects(
        ownerId = ownerId,
        query = query,
        tagId = tagId,
        sort = sortOrder.toStorageKey(),
    ).map { rows -> rows.map { it.toDomain() } }

    override suspend fun findById(id: String): Project? =
        projectDao.findById(id)?.toDomain()

    override fun observeTagsFor(projectId: String): Flow<List<Tag>> =
        projectDao.observeTagsFor(projectId).map { rows -> rows.map { it.toDomain() } }

    /**
     * Cria o projeto e enfileira a op CREATE na mesma transação.
     *
     * A validação das tags acontece dentro da transação — se falhar,
     * nem o projeto nem a op são gravados (rollback total).
     */
    override suspend fun create(project: Project, tagIds: List<String>): Project {
        pendingOpDao.enqueueInTx(
            op = PendingOpEntity.enqueue(
                entityType = SyncEntityType.PROJECT,
                entityId = project.id,
                opType = SyncOpType.CREATE,
                payloadObj = payloadOf(project),
            ),
        ) {
            projectDao.insert(ProjectEntity.fromDomain(project))
            validateTagOwnership(tagIds, project.ownerId)
            projectDao.replaceProjectTags(project.id, tagIds)
        }
        return project
    }

    /** Atualiza o projeto e enfileira a op UPDATE na mesma transação. */
    override suspend fun update(project: Project, tagIds: List<String>): Project {
        pendingOpDao.enqueueInTx(
            op = PendingOpEntity.enqueue(
                entityType = SyncEntityType.PROJECT,
                entityId = project.id,
                opType = SyncOpType.UPDATE,
                payloadObj = payloadOf(project),
            ),
        ) {
            projectDao.update(ProjectEntity.fromDomain(project))
            validateTagOwnership(tagIds, project.ownerId)
            projectDao.replaceProjectTags(project.id, tagIds)
        }
        return project
    }

    /**
     * Remove o projeto e enfileira a op DELETE na mesma transação.
     * Associações em `project_tags` e tarefas filhas são removidas
     * em cascata pelo schema; a op DELETE do backend também cascata
     * no stub.
     */
    override suspend fun delete(id: String) {
        pendingOpDao.enqueueInTx(
            op = PendingOpEntity.enqueue(
                entityType = SyncEntityType.PROJECT,
                entityId = id,
                opType = SyncOpType.DELETE,
            ),
        ) {
            projectDao.deleteById(id)
        }
    }

    /**
     * Garante que cada `tagId` exista e pertença ao mesmo `ownerId`
     * do projeto. Lança [TagNotFoundException] quando a tag não existe
     * e `IllegalArgumentException` quando existe mas pertence a outro
     * usuário — em ambos os casos a operação como um todo é abortada
     * porque a validação ocorre antes de qualquer escrita em
     * `project_tags`.
     */
    private suspend fun validateTagOwnership(tagIds: List<String>, ownerId: String) {
        tagIds.forEach { tagId ->
            val tag = tagDao.findById(tagId) ?: throw TagNotFoundException(tagId)
            if (tag.ownerId != ownerId) throw TagOwnershipException(tagId)
        }
    }

    /** Payload de sincronização do projeto (contrato cliente-supplied). */
    private fun payloadOf(project: Project) = ProjectSyncPayload(
        id = project.id,
        name = project.name,
        description = project.description,
    )
}
