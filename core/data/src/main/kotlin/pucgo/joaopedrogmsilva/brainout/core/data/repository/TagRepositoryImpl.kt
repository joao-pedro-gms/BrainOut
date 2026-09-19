// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.TagDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TagEntity
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TagRepository

/**
 * Implementação Room de [TagRepository].
 *
 * Mantém o mapeamento entre [Tag] (domínio) e [TagEntity] (Room). O
 * índice único `(owner_id, name)` no Room garante a invariante de
 * unicidade por usuário — colisões no `insert` propagam como
 * `SQLiteConstraintException` (estratégia `ABORT`) e serão
 * convertidas em erro de domínio quando um caso de uso específico
 * for adicionado (ex.: `CreateTagUseCase`).
 *
 * @property tagDao DAO de tags injetado pelo Hilt via `DataModule`.
 */
@Singleton
class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao,
) : TagRepository {

    override fun observeForOwner(ownerId: String): Flow<List<Tag>> =
        tagDao.observeForOwner(ownerId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun findById(id: String): Tag? =
        tagDao.findById(id)?.toDomain()

    override suspend fun findByIds(ids: Collection<String>): List<Tag> =
        tagDao.findByIds(ids.toList()).map { it.toDomain() }

    override suspend fun create(tag: Tag): Tag {
        tagDao.insert(TagEntity.fromDomain(tag))
        return tag
    }

    override suspend fun delete(id: String) {
        // Associações em `project_tags` são removidas via ON DELETE CASCADE.
        tagDao.deleteById(id)
    }
}
