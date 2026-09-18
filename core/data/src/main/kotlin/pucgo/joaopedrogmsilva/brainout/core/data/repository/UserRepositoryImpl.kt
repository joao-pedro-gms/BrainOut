// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.UserDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.UserEntity
import pucgo.joaopedrogmsilva.brainout.core.domain.error.DuplicateEmailException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.UserRepository

/**
 * Implementação Room de [UserRepository].
 *
 * Mantém o mapeamento entre [User] (domínio) e [UserEntity] (Room) e
 * traduz erros de banco em exceções de domínio. Os métodos são
 * `suspend` para garantir que a chamada seja feita em uma `Dispatcher`
 * de IO (configurada automaticamente pelo Room via `room-ktx`).
 *
 * @property dao DAO injetado pelo Hilt via `DataModule`.
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val dao: UserDao,
) : UserRepository {

    override suspend fun findByEmail(email: String): User? {
        val normalized = email.trim()
        return dao.findByEmail(normalized)?.toDomain()
    }

    override suspend fun save(user: User): User {
        val entity = UserEntity.fromDomain(user)
        try {
            dao.insert(entity)
        } catch (error: Throwable) {
            if (isUniqueEmailViolation(error)) {
                throw DuplicateEmailException(user.email)
            }
            throw error
        }
        return user
    }

    override suspend fun findById(id: String): User? = dao.findById(id)?.toDomain()

    // --- Reactive API (Flow) ---

    /**
     * Observa um usuário pelo `id`. Emite `null` se não existir (ou se
     * for deletado).
     */
    fun observeById(id: String): Flow<User?> = dao.observeById(id).map { it?.toDomain() }

    /**
     * Observa um usuário pelo `email`. Emite `null` se não existir.
     */
    fun observeByEmail(email: String): Flow<User?> =
        dao.observeByEmail(email.trim()).map { it?.toDomain() }

    companion object {
        /**
         * Heurística para detectar violação de `UNIQUE` na coluna
         * `email`. Evita depender da classe concreta de
         * `SQLiteException` (varia entre plataformas/encodings) e
         * funciona com as mensagens retornadas por Room 2.6+ e
         * SQLiteAndroid.
         */
        private fun isUniqueEmailViolation(error: Throwable): Boolean {
            val message = error.message?.lowercase() ?: return false
            val isConstraint = message.contains("unique") &&
                (message.contains("constraint") || message.contains("failed"))
            val mentionsEmail = message.contains("users.email") || message.contains("email")
            return isConstraint && mentionsEmail
        }
    }
}
