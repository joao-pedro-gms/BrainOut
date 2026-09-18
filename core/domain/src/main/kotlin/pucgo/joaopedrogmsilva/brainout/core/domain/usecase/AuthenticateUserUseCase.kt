// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidCredentialsException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.PasswordHasher
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.UserRepository

/**
 * Caso de uso responsável por autenticar um usuário a partir de e-mail
 * e senha em texto puro.
 *
 * Fluxo:
 * 1. Normaliza e valida o e-mail.
 * 2. Busca o [User] pelo e-mail.
 * 3. Compara a senha fornecida com o hash armazenado via
 *    [PasswordHasher.verify].
 *
 * Lança [InvalidCredentialsException] em qualquer falha (e-mail
 * inexistente, hash inválido ou senha incorreta) para evitar
 * enumeração de contas.
 */
class AuthenticateUserUseCase(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
) {
    /**
     * @param email E-mail do usuário.
     * @param rawPassword Senha em texto puro.
     * @return O [User] autenticado.
     */
    suspend operator fun invoke(email: String, rawPassword: String): User {
        User.requireValidEmail(email)
        require(rawPassword.isNotBlank()) { "Senha não pode ser vazia" }

        val normalizedEmail = email.trim()
        val user = userRepository.findByEmail(normalizedEmail)
            ?: throw InvalidCredentialsException()

        val valid = passwordHasher.verify(rawPassword, user.passwordHash)
        if (!valid) throw InvalidCredentialsException()
        return user
    }
}
