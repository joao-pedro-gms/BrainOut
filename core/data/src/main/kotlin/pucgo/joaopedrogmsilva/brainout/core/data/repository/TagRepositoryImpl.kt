// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import pucgo.joaopedrogmsilva.brainout.core.data.local.LocalTransaction
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.ProjectDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.TagDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TagEntity
import pucgo.joaopedrogmsilva.brainout.core.data.util.logDebug
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TagRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BrainOut:TagRepo"

/** Persistência local de tag, com transações Room para escritas e cascatas. */
@Singleton
class TagRepositoryImpl
    @Inject
    constructor(
        private val tagDao: TagDao,
        private val projectDao: ProjectDao,
        private val localTransaction: LocalTransaction,
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

        override suspend fun findById(id: String): Tag? = tagDao.findById(id)?.toDomain()

        override suspend fun findByIds(ids: Collection<String>): List<Tag> =
            tagDao.findByIds(ids.toList()).map { it.toDomain() }

        override suspend fun create(tag: Tag): Tag {
            logDebug(TAG, "Criando tag id=${tag.id}, name=${tag.name}, color=${tag.color}, ownerId=${tag.ownerId}")
            localTransaction.run {
                tagDao.insert(TagEntity.fromDomain(tag))
            }
            return tag
        }

        override suspend fun delete(id: String) {
            logDebug(TAG, "Deletando tag id=$id")
            localTransaction.run {
                tagDao.deleteById(id)
            }
        }
    }
