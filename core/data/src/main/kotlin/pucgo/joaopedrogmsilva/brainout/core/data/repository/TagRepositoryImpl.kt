// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import android.database.sqlite.SQLiteConstraintException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.ProjectDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.TagDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TagEntity
import pucgo.joaopedrogmsilva.brainout.core.data.session.AccountAccess
import pucgo.joaopedrogmsilva.brainout.core.data.util.logDebug
import pucgo.joaopedrogmsilva.brainout.core.domain.error.DuplicateTagNameException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TagNotFoundException
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
        private val access: AccountAccess,
        private val projectDao: ProjectDao,
    ) : TagRepository {
        override fun observeForOwner(ownerId: String): Flow<List<Tag>> =
            access.observeOwner(ownerId, emptyList()) {
                tagDao.observeForOwner(ownerId).map { rows ->
                    rows.map { it.toDomain() }
                }
            }

        override fun observeByProjectIds(ids: Set<String>): Flow<Map<String, List<Tag>>> =
            access.observe(emptyMap()) { owner ->
                val ownedIds = ids.filter { projectDao.findById(it)?.ownerId == owner }
                tagDao.observeForProjects(ownedIds).map { rows ->
                    val byProject =
                        rows.filter { it.tag.ownerId == owner }.groupBy(
                            { it.projectId },
                            { it.tag.toDomain() },
                        )
                    ids.associateWith { projectId -> byProject[projectId].orEmpty() }
                }
            }

        override suspend fun findById(id: String): Tag? =
            tagDao.findById(id)?.takeIf { access.owns(it.ownerId) }?.toDomain()

        override suspend fun create(tag: Tag): Tag {
            logDebug(TAG, "Criando tag id=${tag.id}, name=${tag.name}, color=${tag.color}, ownerId=${tag.ownerId}")
            try {
                access.write(tag.ownerId) {
                    tagDao.insert(TagEntity.fromDomain(tag))
                }
            } catch (e: SQLiteConstraintException) {
                // O índice UNIQUE `(owner_id, name)` é a fonte da verdade
                // para "essa tag já existe". Traduzir aqui permite que a
                // tela diga "já existe uma tag com esse nome" em vez da
                // mensagem genérica de falha (DEF-61).
                logDebug(TAG, "Nome de tag duplicado para o owner ${tag.ownerId}: ${tag.name}")
                throw DuplicateTagNameException(tag.name)
            }
            return tag
        }

        override suspend fun delete(id: String) {
            logDebug(TAG, "Deletando tag id=$id")
            val tag = findById(id) ?: throw TagNotFoundException(id)
            access.write(tag.ownerId) {
                tagDao.deleteById(id)
            }
        }
    }
