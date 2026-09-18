// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.repository

import pucgo.joaopedrogmsilva.brainout.core.domain.model.User

/**
 * Porta (interface) do repositório de usuários.
 *
 * Implementações concretas vivem em `:core:data`. Esta abstração mantém
 * `:core:domain` livre de dependências de Android, Room ou rede.
 */
interface UserRepository {

    /** Busca um [User] pelo e-mail ou retorna null se não existir. */
    suspend fun findByEmail(email: String): User?

    /** Persiste um novo [User]. Lança se o e-mail já existir. */
    suspend fun save(user: User): User

    /** Busca um [User] pelo seu identificador ou retorna null se não existir. */
    suspend fun findById(id: String): User?
}
