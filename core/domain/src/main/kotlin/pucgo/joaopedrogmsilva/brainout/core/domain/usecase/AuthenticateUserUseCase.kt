// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidCredentialsException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.PasswordHasher
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.UserRepository
import javax.inject.Inject

private const val UNKNOWN_USER_DUMMY_PASSWORD: String = "brainout-authentication-timing-dummy"

/** Autentica por comparação exata do nome; falhas não revelam se a conta existe. */
class AuthenticateUserUseCase
    @Inject
    constructor(
        private val userRepository: UserRepository,
        private val passwordHasher: PasswordHasher,
    ) {
        suspend operator fun invoke(
            username: String,
            rawPassword: String,
        ): User {
            val inputValid = User.USERNAME_REGEX.matches(username.trim()) && rawPassword.length >= MIN_PASSWORD_LENGTH
            val user = if (inputValid) userRepository.findByUsername(username.trim()) else null
            if (user == null) {
                if (inputValid) {
                    // Usa o mesmo KDF caro do caminho de senha incorreta para
                    // reduzir a diferença de tempo que revelaria contas existentes.
                    passwordHasher.hash(UNKNOWN_USER_DUMMY_PASSWORD)
                }
                throw InvalidCredentialsException()
            }
            if (!passwordHasher.verify(rawPassword, user.passwordHash)) {
                throw InvalidCredentialsException()
            }
            return user
        }
    }
