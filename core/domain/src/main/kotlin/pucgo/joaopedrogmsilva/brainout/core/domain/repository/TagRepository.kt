// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag

interface TagRepository {
    fun observeForOwner(ownerId: String): Flow<List<Tag>>
    suspend fun findById(id: String): Tag?
    suspend fun findByIds(ids: Collection<String>): List<Tag>

    /**
     * Mapa de `projectId` para as tags associadas via `project_tags`.
     *
     * @param ids conjunto de projetos a observar. Vazio → Flow vazio.
     */
    fun observeByProjectIds(ids: Set<String>): Flow<Map<String, List<Tag>>>

    suspend fun create(tag: Tag): Tag
    suspend fun delete(id: String)
}
