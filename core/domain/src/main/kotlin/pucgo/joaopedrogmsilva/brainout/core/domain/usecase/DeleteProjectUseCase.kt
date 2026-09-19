// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import javax.inject.Inject
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository

/**
 * Caso de uso responsável por remover um [pucgo.joaopedrogmsilva.brainout.core.domain.model.Project].
 *
 * Tarefas e associações `project_tags` são removidas via `ON DELETE
 * CASCADE` do schema Room — não é necessário tratamento adicional.
 */
class DeleteProjectUseCase @Inject constructor(
    private val repository: ProjectRepository,
) {
    /** @param projectId Identificador do projeto a remover. */
    suspend operator fun invoke(projectId: String) = repository.delete(projectId)
}
