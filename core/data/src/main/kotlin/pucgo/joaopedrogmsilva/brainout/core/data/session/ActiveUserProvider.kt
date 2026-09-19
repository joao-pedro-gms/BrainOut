// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.session

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.UserRepository

/**
 * Combina [SessionStore] (DataStore) com [UserRepository] (Room) para
 * expor o [User] atualmente autenticado como um [Flow] reativo.
 *
 * Emite `null` quando:
 * - Não há id persistido em [SessionStore].
 * - O id está persistido mas o usuário correspondente não existe no
 *   Room (sessão órfã — limpamos o id para evitar loops).
 *
 * Consumidores típicos:
 * - `HomeScreen` para saudação + badge de papel (E1.7).
 * - `MainActivity` para decidir a rota inicial (E1.8).
 */
@Singleton
class ActiveUserProvider @Inject constructor(
    private val sessionStore: SessionStore,
    private val userRepository: UserRepository,
) {

    /**
     * [Flow] que emite o [User] ativo ou `null` se não houver sessão.
     *
     * Usa [flatMapLatest] para reagir a mudanças no id persistido:
     * cada novo id dispara uma busca em [UserRepository.findById]
     * automaticamente. Sessões órfãs (id presente mas `User` ausente)
     * são limpas no [SessionStore] para que o próximo start do app
     * leve o usuário de volta para a tela de Login.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeActiveUser(): Flow<User?> = sessionStore.observeUserId()
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) {
                flow { emit(null) }
            } else {
                flow {
                    val user = userRepository.findById(id)
                    if (user == null) {
                        sessionStore.clear()
                        emit(null)
                    } else {
                        emit(user)
                    }
                }
            }
        }

    /**
     * [Flow] que emite o id ([User.id]) do usuário ativo, ou `null`
     * se não houver sessão.
     *
     * Útil para consumidores que precisam apenas do id (ex.: filtros
     * `observeXxxForOwner(ownerId)` nos repositórios), sem ter que
     * materializar o [User] completo a cada emissão.
     *
     * Reage automaticamente a mudanças no id persistido em
     * [SessionStore] (mesma cadeia `flatMapLatest` de
     * [observeActiveUser]).
     */
    fun observeActiveUserId(): Flow<String?> = observeActiveUser().map { it?.id }

    /**
     * Versão suspensa que devolve o usuário ativo atual (ou `null`).
     * Útil para pontos de inicialização onde só o valor pontual
     * importa.
     */
    suspend fun currentActiveUser(): User? {
        val id = sessionStore.currentUserId() ?: return null
        return userRepository.findById(id)
    }

    /** Encerra a sessão atual (logout). */
    suspend fun signOut() {
        sessionStore.clear()
    }
}
