// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.error

/**
 * Exceção base da camada de domínio.
 *
 * Capturada por `Result.failure` nos use cases — e, a partir de
 * 2026-10-08, também por `catch (DomainException)` nas camadas acima —
 * para que a UI, o WorkManager ou a retaguarda traduzam em mensagens
 * localizadas sem precisar conhecer os detalhes de validação.
 *
 * **Hierarquia (RN):** `DomainException` é a raiz única de todo o
 * pacote `error/`. `IllegalArgumentException` continua ancestral
 * (`IllegalArgumentException` → `DomainException`) por dois motivos:
 * - `kotlin.require()` nos modelos ([pucgo.joaopedrogmsilva.brainout.core.domain.model.User],
 *   `Project`, `Task`, `Tag`) e nos use cases de usuário lança
 *   `IllegalArgumentException` crua, e os testes asseguram esse
 *   contrato (`UserTest`, `ProjectTest`, `TagInvariantTest`,
 *   `TaskTest`, `TaskPriorityTest`, `CreateUserUseCaseTest`,
 *   `AuthenticateUserUseCaseTest`). Remover esse ancestral quebraria
 *   o contrato testado sem ganho de segurança.
 * - Um `catch (IllegalArgumentException)` legado em código externo
 *   continua capturando erros de domínio em vez de escapar.
 *
 * Já [BusinessRuleException] **não** é mais uma raiz separada: ela
 * estende [DomainException], então um `catch (DomainException)`
 * agora captura também violações de RN (antes elas escapavam).
 */
sealed class DomainException(
    message: String,
) : IllegalArgumentException(message)

/** Uma invariante do modelo foi violada (ex.: nome vazio). */
class InvalidModelException(
    message: String,
) : DomainException(message)

/** Tentativa de criar um [User] com e-mail já utilizado. */
class DuplicateEmailException(
    val email: String,
) : DomainException("E-mail já cadastrado: $email")

/** Credenciais informadas não correspondem a um usuário válido. */
class InvalidCredentialsException : DomainException("Credenciais inválidas.")

/** Transição de estado inválida (ex.: [Task] Done -> Todo). */
class InvalidStateTransitionException(
    message: String,
) : DomainException(message)

/** Tarefa referenciada por uma operação não existe. */
class TaskNotFoundException(
    taskId: String,
) : DomainException("Tarefa não encontrada: $taskId")

/** Projeto referenciado por uma operação não existe. */
class ProjectNotFoundException(
    projectId: String,
) : DomainException("Projeto não encontrado: $projectId")

/** Tag existe mas pertence a outro usuário (owner diferente). */
class TagOwnershipException(
    tagId: String,
) : DomainException("Tag $tagId não pertence ao owner do projeto")
