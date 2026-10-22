// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag

interface TagRepository {
    fun observeForOwner(ownerId: String): Flow<List<Tag>>

    suspend fun findById(id: String): Tag?

    /**
     * Mapa de `projectId` para as tags associadas via `project_tags`.
     *
     * Todo id informado aparece como chave — com lista vazia quando o
     * projeto não tem tags. Conjunto vazio → mapa vazio.
     *
     * @param ids conjunto de projetos a observar.
     */
    fun observeByProjectIds(ids: Set<String>): Flow<Map<String, List<Tag>>>

    suspend fun create(tag: Tag): Tag

    suspend fun delete(id: String)
}
