// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.PendingOpDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.ProjectDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.TagDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.PendingOpEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.SyncEntityType
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.SyncOpType
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TagEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TagSyncPayload
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TagRepository

/**
 * Implementação Room de [TagRepository] com **escrita dual**
 * offline-first (E3.3).
 *
 * Toda mutação grava no Room **e** enfileira a op em `pending_ops`
 * na mesma transação Room ([PendingOpDao.enqueueInTx]).
 *
 * Particularidade das tags no contrato do stub
 * (`backend-stub/server.py`): o `id` de tag é **gerado pelo servidor**
 * (vocabulário sem identidade local forte antes do upload). A op
 * CREATE carrega apenas `name`/`color`; a op DELETE usa o id local
 * e é idempotente (204 mesmo ausente) — se o CREATE ainda não
 * sincronizou, o DELETE descarta no servidor sem erro.
 *
 * O índice único `(owner_id, name)` no Room garante a invariante de
 * unicidade por usuário — colisões no `insert` propagam como
 * `SQLiteConstraintException` (estratégia `ABORT`) e serão
 * convertidas em erro de domínio quando um caso de uso específico
 * for adicionado (ex.: `CreateTagUseCase`).
 *
 * @property tagDao DAO de tags injetado pelo Hilt via `DataModule`.
 * @property pendingOpDao DAO da fila de sincronização (E3.3).
 */
@Singleton
class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao,
    private val projectDao: ProjectDao,
    private val pendingOpDao: PendingOpDao,
) : TagRepository {

    override fun observeForOwner(ownerId: String): Flow<List<Tag>> =
        tagDao.observeForOwner(ownerId).map { rows -> rows.map { it.toDomain() } }

    override fun observeByProjectIds(ids: Set<String>): Flow<Map<String, List<Tag>>> =
        combine(
            ids.map { id -> projectDao.observeTagsFor(id).map { id to it } },
        ) { pairs ->
            @Suppress("UNCHECKED_CAST")
            (pairs as Array<Pair<String, List<TagEntity>>>)
                .associate { (projectId, tags) -> projectId to tags.map { it.toDomain() } }
        }

    override suspend fun findById(id: String): Tag? =
        tagDao.findById(id)?.toDomain()

    override suspend fun findByIds(ids: Collection<String>): List<Tag> =
        tagDao.findByIds(ids.toList()).map { it.toDomain() }

    /**
     * Cria a tag localmente e enfileira a op CREATE na mesma
     * transação. Colisão de nome (índice único) reverte ambas as
     * escritas — a fila nunca guarda op de tag que não existe.
     */
    override suspend fun create(tag: Tag): Tag {
        pendingOpDao.enqueueInTx(
            op = PendingOpEntity.enqueue(
                entityType = SyncEntityType.TAG,
                entityId = tag.id,
                opType = SyncOpType.CREATE,
                payloadObj = TagSyncPayload(
                    id = tag.id,
                    name = tag.name,
                    color = tag.color,
                ),
            ),
        ) {
            tagDao.insert(TagEntity.fromDomain(tag))
        }
        return tag
    }

    /**
     * Remove a tag localmente e enfileira a op DELETE na mesma
     * transação. Associações em `project_tags` são removidas via
     * `ON DELETE CASCADE`.
     */
    override suspend fun delete(id: String) {
        pendingOpDao.enqueueInTx(
            op = PendingOpEntity.enqueue(
                entityType = SyncEntityType.TAG,
                entityId = id,
                opType = SyncOpType.DELETE,
            ),
        ) {
            tagDao.deleteById(id)
        }
    }
}
