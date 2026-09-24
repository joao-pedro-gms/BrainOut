// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import javax.inject.Inject
import pucgo.joaopedrogmsilva.brainout.core.domain.error.DuplicateEmailException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.PasswordHasher
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.UserRepository

/**
 * Regra de tamanho mínimo da senha.
 *
 * Única autoridade — a ViewModel e a camada de UI apenas espelham
 * esta constante para mensagens e validações antecipadas. Centralizar
 * evita divergência entre a regra client-side (UX rápida) e a regra
 * de domínio (defesa em profundidade).
 */
const val MIN_PASSWORD_LENGTH: Int = 8

/**
 * Caso de uso responsável por cadastrar um novo usuário.
 *
 * Fluxo:
 * 1. Normaliza e valida o e-mail (regex).
 * 2. Verifica duplicidade via [UserRepository.findByEmail].
 * 3. Aplica o hash da senha via [PasswordHasher].
 * 4. Persiste o novo [User] retornando a instância resultante.
 *
 * Lança:
 * - [pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException]
 *   se nome ou e-mail forem inválidos.
 * - [BusinessRuleException] se a senha for mais curta que [MIN_PASSWORD_LENGTH].
 * - [DuplicateEmailException] se já existir usuário com o mesmo e-mail.
 *
 * Detalhes completos ficam em [invoke].
 */
class CreateUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
) {
    /**
     * @param name Nome completo (1..120 caracteres).
     * @param email E-mail válido.
     * @param rawPassword Senha em texto puro. Mínimo [MIN_PASSWORD_LENGTH]
     *   caracteres (será hashed).
     * @param role Papel inicial ([UserRole.OWNER] ou [UserRole.MEMBER]).
     * @return O [User] criado e persistido.
     */
    suspend operator fun invoke(
        name: String,
        email: String,
        rawPassword: String,
        role: UserRole,
    ): User {
        User.requireValidName(name)
        User.requireValidEmail(email)
        require(rawPassword.length >= MIN_PASSWORD_LENGTH) {
            "Senha deve ter pelo menos $MIN_PASSWORD_LENGTH caracteres"
        }

        val normalizedEmail = email.trim()
        userRepository.findByEmail(normalizedEmail)?.let {
            throw DuplicateEmailException(normalizedEmail)
        }

        val passwordHash = passwordHasher.hash(rawPassword)
        val newUser = User.create(
            name = name,
            email = normalizedEmail,
            passwordHash = passwordHash,
            role = role,
        )
        return userRepository.save(newUser)
    }
}
