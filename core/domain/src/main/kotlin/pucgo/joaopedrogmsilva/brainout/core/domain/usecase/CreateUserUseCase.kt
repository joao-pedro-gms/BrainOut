// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import pucgo.joaopedrogmsilva.brainout.core.domain.error.DuplicateUsernameException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.PasswordHasher
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.UserRepository
import javax.inject.Inject

/** Cadastra uma conta local; o banco também garante a unicidade em escritas concorrentes. */
class CreateUserUseCase
    @Inject
    constructor(
        private val userRepository: UserRepository,
        private val passwordHasher: PasswordHasher,
    ) {
        suspend operator fun invoke(
            username: String,
            rawPassword: String,
        ): User {
            User.requireValidUsername(username)
            requireValidPassword(rawPassword)
            val normalized = username.trim()
            if (userRepository.findByUsername(normalized) != null) throw DuplicateUsernameException(normalized)
            return userRepository.save(User.create(normalized, passwordHasher.hash(rawPassword)))
        }
    }
