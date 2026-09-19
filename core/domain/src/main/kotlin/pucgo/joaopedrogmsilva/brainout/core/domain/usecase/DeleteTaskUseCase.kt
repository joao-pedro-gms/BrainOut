// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Use case responsável por remover uma [Task] pelo id.

package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import javax.inject.Inject
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository

/**
 * Remove a [pucgo.joaopedrogmsilva.brainout.core.domain.model.Task]
 * identificada por [taskId]. Não há regra de negócio associada —
 * a remoção é simplesmente propagada para o repositório.
 */
class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    /** @param taskId Identificador da tarefa a remover. */
    suspend operator fun invoke(taskId: String) = repository.delete(taskId)
}
