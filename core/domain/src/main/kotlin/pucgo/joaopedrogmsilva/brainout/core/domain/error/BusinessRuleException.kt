// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.error

/** Exceção base para violações de regras de negócio (RN01, RN02, RN03). */
open class BusinessRuleException(message: String) : RuntimeException(message)

class ProjectTaskLimitReachedException(projectId: String, limit: Int) :
    BusinessRuleException("Projeto $projectId já atingiu o limite de $limit tarefas ativas")
