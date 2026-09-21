// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag

interface ProjectRepository {
    fun observeAllForOwner(ownerId: String): Flow<List<Project>>

    /**
     * Observa os projetos de [ownerId] aplicando busca textual por
     * nome, filtro opcional por tag e ordenação configurável
     * (E2.6 do ROADMAP).
     *
     * - [query]: string livre da busca; vazia desativa o filtro de
     *   texto.
     * - [tagId]: id da tag selecionada como filtro; `null` desativa.
     * - [sortOrder]: chave da ordenação (ver [SortOrder]).
     */
    fun observeSearch(
        ownerId: String,
        query: String,
        tagId: String?,
        sortOrder: SortOrder,
    ): Flow<List<Project>>

    suspend fun findById(id: String): Project?
    fun observeTagsFor(projectId: String): Flow<List<Tag>>
    suspend fun create(project: Project, tagIds: List<String>): Project
    suspend fun update(project: Project, tagIds: List<String>): Project
    suspend fun delete(id: String)
}
