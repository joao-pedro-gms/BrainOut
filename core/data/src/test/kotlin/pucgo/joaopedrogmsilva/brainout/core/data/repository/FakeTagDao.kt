// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import android.database.sqlite.SQLiteConstraintException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.TagDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectTagRow
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TagEntity

/**
 * Fake de [TagDao] compartilhado pelos testes de repositório.
 *
 * Havia duas cópias: uma reproduzia só o essencial e a outra também
 * reproduzia o índice UNIQUE `(owner_id, name)` e montava
 * `observeForProjects` a partir das associações. Esta é a segunda — a
 * completa — e serve às duas suítes: o [ProjectRepositoryImpl] usa
 * apenas `findById` (as tags sao gravadas direto no mapa pelo teste),
 * enquanto o [TagRepositoryImpl] exercita o resto.
 */
internal class FakeTagDao : TagDao {
    val tags: MutableMap<String, TagEntity> = mutableMapOf()
    val associations: MutableMap<String, List<String>> = mutableMapOf()

    override fun observeForOwner(ownerId: String): Flow<List<TagEntity>> =
        MutableStateFlow(tags.values.filter { it.ownerId == ownerId })

    override suspend fun findById(id: String): TagEntity? = tags[id]

    override fun observeForProjects(projectIds: List<String>): Flow<List<ProjectTagRow>> =
        MutableStateFlow(
            projectIds.flatMap { projectId ->
                associations[projectId].orEmpty().mapNotNull { tagId ->
                    tags[tagId]?.let { ProjectTagRow(tag = it, projectId = projectId) }
                }
            },
        )

    override suspend fun insert(tag: TagEntity): Long {
        // O índice UNIQUE `(owner_id, name)` é o que o Room rejeita
        // — o fake reproduz a mesma regra.
        if (tags.values.any { it.ownerId == tag.ownerId && it.name == tag.name }) {
            throw SQLiteConstraintException("UNIQUE constraint failed: tags.owner_id, tags.name")
        }
        tags[tag.id] = tag
        return 1L
    }

    override suspend fun deleteById(id: String) {
        tags.remove(id)
    }
}
