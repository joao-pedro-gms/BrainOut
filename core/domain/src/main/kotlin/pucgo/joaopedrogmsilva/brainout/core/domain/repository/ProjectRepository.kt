// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag

interface ProjectRepository {
    fun observeAllForOwner(ownerId: String): Flow<List<Project>>
    suspend fun findById(id: String): Project?
    fun observeTagsFor(projectId: String): Flow<List<Tag>>
    suspend fun create(project: Project, tagIds: List<String>): Project
    suspend fun update(project: Project, tagIds: List<String>): Project
    suspend fun delete(id: String)
}
