// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.UserDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.UserEntity
import pucgo.joaopedrogmsilva.brainout.core.domain.error.DuplicateUsernameException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl
    @Inject
    constructor(
        private val dao: UserDao,
    ) : UserRepository {
        override suspend fun findByUsername(username: String): User? = dao.findByUsername(username.trim())?.toDomain()

        override suspend fun findById(id: String): User? = dao.findById(id)?.toDomain()

        override suspend fun save(user: User): User {
            try {
                dao.insert(UserEntity.fromDomain(user))
            } catch (error: Exception) {
                if (error.message?.contains("UNIQUE constraint failed: users.username") ==
                    true
                ) {
                    throw DuplicateUsernameException(user.username)
                }
                throw error
            }
            return user
        }
    }
