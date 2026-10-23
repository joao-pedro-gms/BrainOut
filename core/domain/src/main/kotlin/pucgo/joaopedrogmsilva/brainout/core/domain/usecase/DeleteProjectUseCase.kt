// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import pucgo.joaopedrogmsilva.brainout.core.domain.model.Permission
import pucgo.joaopedrogmsilva.brainout.core.domain.permission.requirePermission
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ActiveUserPort
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository
import javax.inject.Inject

/**
 * Caso de uso responsável por remover um [pucgo.joaopedrogmsilva.brainout.core.domain.model.Project].
 *
 * Tarefas e associações `project_tags` são removidas em cascata pela
 * implementação — não é necessário tratamento adicional aqui.
 *
 * A exclusão exige [Permission.DELETE_PROJECT]: só o dono do projeto
 * pode apagá-lo, porque o efeito é em cascata sobre todas as tarefas
 * (DEF-36). A regra é consultada **no domínio**, antes da escrita —
 * esconder o botão na UI seria só defesa secundária.
 */
class DeleteProjectUseCase
    @Inject
    constructor(
        private val repository: ProjectRepository,
        private val activeUser: ActiveUserPort,
    ) {
        /** @param projectId Identificador do projeto a remover. */
        suspend operator fun invoke(projectId: String) {
            activeUser.requirePermission(Permission.DELETE_PROJECT)
            repository.delete(projectId)
        }
    }
