// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.repository

import pucgo.joaopedrogmsilva.brainout.core.domain.model.User

/** Identidade local; nomes são únicos por comparação exata. */
interface UserRepository {
    suspend fun findByUsername(username: String): User?

    suspend fun save(user: User): User

    suspend fun findById(id: String): User?
}
