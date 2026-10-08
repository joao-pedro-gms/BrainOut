// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.error

/**
 * Exceção base para violações de regras de negócio (RN01, RN02, RN03).
 *
 * **Hierarquia (2026-10-08):** esta classe deixou de estender
 * `RuntimeException` diretamente e passou a estender [DomainException].
 * Antes, `BusinessRuleException` e [DomainException] eram raízes
 * irmãs e desconectadas: nenhum `catch (DomainException)` no repositório
 * capturava uma violação de RN. O impacto real era no
 * `CompleteTaskWorker` (`:app`), que captura [DomainException] para
 * tratar violação permanente como falha idempotente — as violações de
 * RN que escapavam dessa captura eram
 * [ProjectTaskLimitReachedException] e
 * [TaskPriorityChangeForbiddenException].
 *
 * Como [DomainException] é `sealed` em Kotlin, as subclasses que já
 * moravam neste arquivo ([ProjectTaskLimitReachedException],
 * [TaskPriorityChangeForbiddenException]) continuam válidas; a
 * selagem é resolvida no mesmo pacote **e arquivo** de compilação.
 *
 * A raiz continua sendo `IllegalArgumentException` via [DomainException],
 * de modo que os testes existentes que asseguram `IllegalArgumentException`
 * para as `require()` dos modelos permanecem válidos.
 */
open class BusinessRuleException(
    message: String,
) : DomainException(message)

class ProjectTaskLimitReachedException(
    projectId: String,
    limit: Int,
) : BusinessRuleException("Projeto $projectId já atingiu o limite de $limit tarefas ativas")

/**
 * RN02 (E2.4): alteração de prioridade em uma tarefa já concluída é
 * proibida. Lançada por
 * [pucgo.joaopedrogmsilva.brainout.core.domain.model.Task.changePriority]
 * e pelos use cases que revalidam contra o registro persistido
 * ([pucgo.joaopedrogmsilva.brainout.core.domain.usecase.UpdateTaskUseCase]
 * e [pucgo.joaopedrogmsilva.brainout.core.domain.usecase.ChangeTaskStatusUseCase]).
 *
 * Mantida como subclasse para que a camada de UI/ViewModel possa
 * distingui-la do [BusinessRuleException] genérico (RN01) quando
 * precisar — por exemplo, para exibir uma mensagem específica
 * ("Tarefa concluída não permite alterar prioridade").
 */
class TaskPriorityChangeForbiddenException(
    taskId: String,
) : BusinessRuleException("RN02: alteração de prioridade bloqueada em tarefa concluída ($taskId)")
