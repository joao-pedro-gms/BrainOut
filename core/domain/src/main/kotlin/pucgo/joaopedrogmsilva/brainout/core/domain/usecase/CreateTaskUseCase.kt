// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import java.time.Instant
import javax.inject.Inject
import pucgo.joaopedrogmsilva.brainout.core.domain.error.ProjectTaskLimitReachedException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository

/**
 * Caso de uso responsável por criar uma nova [Task].
 *
 * Aplica a regra de negócio **RN01** antes de persistir: o projeto
 * não pode ter mais que [MAX_ACTIVE_TASKS_PER_PROJECT] tarefas com
 * `status != DONE`. A contagem é feita via
 * [TaskRepository.countActiveByProject], permitindo que o domínio
 * permaneça independente do `:core:data`.
 *
 * Lança:
 * - [ProjectTaskLimitReachedException] quando o limite já foi atingido.
 * - [pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException]
 *   se o `title` violar as invariantes do domínio.
 */
class CreateTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    /**
     * @param projectId Projeto ao qual a tarefa pertence.
     * @param title Título (1..200 caracteres após trim).
     * @param priority Prioridade (padrão [TaskPriority.MEDIUM]).
     * @param dueDate Prazo opcional.
     * @param assigneeId Responsável opcional.
     */
    suspend operator fun invoke(
        projectId: String,
        title: String,
        priority: TaskPriority = TaskPriority.MEDIUM,
        dueDate: Instant? = null,
        assigneeId: String? = null,
    ): Task {
        val active = repository.countActiveByProject(projectId)
        if (active >= MAX_ACTIVE_TASKS_PER_PROJECT) {
            throw ProjectTaskLimitReachedException(
                projectId = projectId,
                limit = MAX_ACTIVE_TASKS_PER_PROJECT,
            )
        }
        val task = Task.create(
            projectId = projectId,
            title = title,
            priority = priority,
            status = TaskStatus.TODO,
            dueDate = dueDate,
            assigneeId = assigneeId,
        )
        return repository.create(task)
    }
}
