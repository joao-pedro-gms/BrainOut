// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Use case responsável por remover uma [Task] pelo id.

package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import pucgo.joaopedrogmsilva.brainout.core.domain.model.Permission
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler
import pucgo.joaopedrogmsilva.brainout.core.domain.permission.requirePermission
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ActiveUserPort
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Remove a [pucgo.joaopedrogmsilva.brainout.core.domain.model.Task]
 * identificada por [taskId] e cancela o lembrete de prazo associado
 * (marco E3.6). Cancelar após a remoção é sempre seguro — para o
 * scheduler é no-op quando não há agendamento pendente.
 *
 * A remoção exige [Permission.DELETE_TASK]: a matriz de papéis dá a
 * permissão de edição a `Member`, mas não a de exclusão (DEF-36).
 */
class DeleteTaskUseCase
    @Inject
    constructor(
        private val repository: TaskRepository,
        private val deadlineScheduler: DeadlineNotificationScheduler,
        private val activeUser: ActiveUserPort,
    ) {
        /** @param taskId Identificador da tarefa a remover. */
        suspend operator fun invoke(taskId: String) {
            activeUser.requirePermission(Permission.DELETE_TASK)
            repository.delete(taskId)
            deadlineScheduler.cancel(taskId)
        }
    }
