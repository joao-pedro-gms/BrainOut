// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.error

/** Exceção base para violações de regras de negócio (RN01, RN02, RN03). */
open class BusinessRuleException(message: String) : RuntimeException(message)

class ProjectTaskLimitReachedException(projectId: String, limit: Int) :
    BusinessRuleException("Projeto $projectId já atingiu o limite de $limit tarefas ativas")

/**
 * RN02 (E2.4): alteração de prioridade em uma tarefa já concluída é
 * proibida. Lançada por
 * [pucgo.joaopedrogmsilva.brainout.core.domain.model.Task.changePriority].
 *
 * Mantida como subclasse para que a camada de UI/ViewModel possa
 * distingui-la do [BusinessRuleException] genérico (RN01) quando
 * precisar — por exemplo, para exibir uma mensagem específica
 * ("Tarefa concluída não permite alterar prioridade").
 */
class TaskPriorityChangeForbiddenException(taskId: String) :
    BusinessRuleException("RN02: alteração de prioridade bloqueada em tarefa concluída ($taskId)")
