// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import javax.inject.Inject
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository

/**
 * Caso de uso responsável por atualizar uma [Task] existente.
 *
 * O caller deve aplicar as invariantes do domínio via métodos
 * específicos ([Task.rename], [Task.changePriority], [Task.reassign],
 * [Task.changeDueDate]) antes de invocar este caso de uso — a
 * validação de invariantes já ocorreu nessa etapa.
 */
class UpdateTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    /** @param task Tarefa com os campos atualizados — `id` deve existir. */
    suspend operator fun invoke(task: Task): Task = repository.update(task)
}
