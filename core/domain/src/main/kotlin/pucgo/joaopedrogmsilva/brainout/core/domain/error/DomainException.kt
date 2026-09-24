// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.error

/**
 * Exceção base da camada de domínio.
 *
 * Capturada por `Result.failure` nos use cases para que a camada
 * superior (UI, retaguarda) traduza em mensagens localizadas sem
 * precisar conhecer os detalhes de validação.
 */
sealed class DomainException(message: String) : IllegalArgumentException(message)

/** Uma invariante do modelo foi violada (ex.: nome vazio). */
class InvalidModelException(message: String) : DomainException(message)

/** Tentativa de criar um [User] com e-mail já utilizado. */
class DuplicateEmailException(val email: String) :
    DomainException("E-mail já cadastrado: $email")

/** Credenciais informadas não correspondem a um usuário válido. */
class InvalidCredentialsException :
    DomainException("Credenciais inválidas.")

/** Transição de estado inválida (ex.: [Task] Done -> Todo). */
class InvalidStateTransitionException(message: String) : DomainException(message)

/** Tarefa referenciada por uma operação não existe. */
class TaskNotFoundException(taskId: String) :
    DomainException("Tarefa não encontrada: $taskId")

/** Projeto referenciado por uma operação não existe. */
class ProjectNotFoundException(projectId: String) :
    DomainException("Projeto não encontrado: $projectId")
