// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import javax.inject.Inject
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository

/**
 * Caso de uso responsável por atualizar um [Project] existente.
 *
 * Recebe o projeto já modificado pela camada de apresentação
 * (via [Project.copy]) e a nova lista de `tagIds`; a validação
 * das tags (existência e mesma propriedade) acontece no
 * [ProjectRepository.update].
 */
class UpdateProjectUseCase @Inject constructor(
    private val repository: ProjectRepository,
) {
    /**
     * @param project Projeto com os campos atualizados — `id` deve
     *   referenciar um projeto já persistido.
     * @param tagIds Lista final de tags a associar — substitui a anterior.
     */
    suspend operator fun invoke(
        project: Project,
        tagIds: List<String>,
    ): Project = repository.update(project, tagIds)
}
