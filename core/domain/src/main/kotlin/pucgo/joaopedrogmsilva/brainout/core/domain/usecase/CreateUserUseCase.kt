// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import pucgo.joaopedrogmsilva.brainout.core.domain.error.DuplicateEmailException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.PasswordHasher
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.UserRepository

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
 * - [DuplicateEmailException] se já existir usuário com o mesmo e-mail.
 *
 * Detalhes completos ficam em [invoke].
 */
class CreateUserUseCase(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
) {
    /**
     * @param name Nome completo (1..120 caracteres).
     * @param email E-mail válido.
     * @param rawPassword Senha em texto puro (será hashed).
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
        require(rawPassword.isNotBlank()) { "Senha não pode ser vazia" }

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
