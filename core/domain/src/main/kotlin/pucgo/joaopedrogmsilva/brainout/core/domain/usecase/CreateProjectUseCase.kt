// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import javax.inject.Inject
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository

/**
 * Caso de uso responsável por criar um novo [Project].
 *
 * Constrói a entidade via [Project.create] (valida nome, descrição
 * e demais invariantes) e delega a persistência ao
 * [ProjectRepository.create], que valida a propriedade das tags
 * antes de associá-las ao projeto.
 *
 * Lança [pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException]
 * se `name` ou `description` violarem as invariantes do domínio.
 */
class CreateProjectUseCase @Inject constructor(
    private val repository: ProjectRepository,
) {
    /**
     * @param name Nome do projeto (1..120 caracteres após trim).
     * @param ownerId [Project.ownerId] — id do usuário dono.
     * @param description Descrição opcional (até 500 caracteres).
     * @param tagIds Tags a associar — cada uma deve existir e
     *   pertencer ao mesmo `ownerId` do projeto.
     */
    suspend operator fun invoke(
        name: String,
        ownerId: String,
        description: String? = null,
        tagIds: List<String> = emptyList(),
    ): Project {
        val project = Project.create(
            name = name,
            ownerId = ownerId,
            description = description,
        )
        return repository.create(project, tagIds)
    }
}
