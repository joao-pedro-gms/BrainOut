// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ActiveUserPort
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository
import javax.inject.Inject

/**
 * Caso de uso responsável por remover um [pucgo.joaopedrogmsilva.brainout.core.domain.model.Project].
 *
 * Tarefas e associações `project_tags` são removidas em cascata pela
 * implementação — não é necessário tratamento adicional aqui.
 *
 * A operação exige uma conta ativa. O repositório valida que o projeto
 * pertence à conta atual antes de executar a exclusão em cascata.
 */
class DeleteProjectUseCase
    @Inject
    constructor(
        private val repository: ProjectRepository,
        private val activeUser: ActiveUserPort,
    ) {
        /** @param projectId Identificador do projeto a remover. */
        suspend operator fun invoke(projectId: String) {
            activeUser.currentUser() ?: throw pucgo.joaopedrogmsilva.brainout.core.domain.error
                .UserNotFoundException()
            repository.delete(projectId)
        }
    }
