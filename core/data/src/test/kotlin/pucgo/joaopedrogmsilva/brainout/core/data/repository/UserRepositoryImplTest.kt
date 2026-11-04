// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.UserDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.UserEntity
import pucgo.joaopedrogmsilva.brainout.core.domain.error.DuplicateUsernameException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import java.time.Instant
import java.util.UUID

/**
 * Testes do [UserRepositoryImpl] usando um fake de [UserDao].
 *
 * Cobre:
 * - Round-trip User ↔ UserEntity via `findByUsername`/`findById`.
 * - `save` lança [DuplicateUsernameException] quando o DAO simula
 *   violação de `UNIQUE`.
 * - Trim do e-mail nas buscas.
 */
class UserRepositoryImplTest {
    @Test
    fun `save persists a User and findByUsername returns it`() =
        runTest {
            val dao = FakeUserDao()
            val repository = UserRepositoryImpl(dao)
            val user = sampleUser(username = "joao")

            val returned = repository.save(user)
            val loaded = repository.findByUsername("joao")

            assertThat(returned).isEqualTo(user)
            assertThat(loaded).isEqualTo(user)
        }

    @Test
    fun `findByUsername returns null when missing`() =
        runTest {
            val repository = UserRepositoryImpl(FakeUserDao())
            assertThat(repository.findByUsername("nobody")).isNull()
        }

    @Test
    fun `findById returns user when present`() =
        runTest {
            val dao = FakeUserDao()
            val user = sampleUser()
            dao.storage[user.id] = UserEntity.fromDomain(user)
            val repository = UserRepositoryImpl(dao)

            assertThat(repository.findById(user.id)).isEqualTo(user)
        }

    @Test
    fun `findById returns null when missing`() =
        runTest {
            val repository = UserRepositoryImpl(FakeUserDao())
            assertThat(repository.findById("missing")).isNull()
        }

    @Test
    fun `save translates unique violation into DuplicateUsernameException`() =
        runTest {
            val dao =
                FakeUserDao().apply {
                    nextInsertError = FakeUserDao.UNIQUE_EMAIL_VIOLATION
                }
            val repository = UserRepositoryImpl(dao)

            val thrown =
                runCatching {
                    repository.save(sampleUser(username = "joao"))
                }.exceptionOrNull()

            assertThat(thrown).isInstanceOf(DuplicateUsernameException::class.java)
            assertThat((thrown as DuplicateUsernameException).username).isEqualTo("joao")
        }

    @Test
    fun `save propagates non-unique errors unchanged`() =
        runTest {
            val generic = IllegalStateException("boom")
            val dao = FakeUserDao().apply { nextInsertError = generic }
            val repository = UserRepositoryImpl(dao)

            val thrown =
                runCatching {
                    repository.save(sampleUser())
                }.exceptionOrNull()

            assertThat(thrown).isSameInstanceAs(generic)
        }

    @Test
    fun `findByUsername trims whitespace`() =
        runTest {
            val dao = FakeUserDao()
            val user = sampleUser(username = "joao")
            dao.storage[user.id] = UserEntity.fromDomain(user)
            val repository = UserRepositoryImpl(dao)

            assertThat(repository.findByUsername("  joao  ")).isEqualTo(user)
        }

    private fun sampleUser(
        id: String = UUID.randomUUID().toString(),
        username: String = "joao",
        createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    ): User =
        User(
            id = id,
            username = username,
            passwordHash = "pbkdf2_sha256\$120000\$AAAA\$BBBB",
            createdAt = createdAt,
        )

    /**
     * Implementação fake de [UserDao] suficiente para exercitar o
     * repositório sem precisar do Room/Robolectric.
     */
    private class FakeUserDao : UserDao {
        override suspend fun legacyLogins(): List<UserEntity> = emptyList()

        override suspend fun dismissLegacyLogins() = Unit

        val storage: MutableMap<String, UserEntity> = mutableMapOf()

        var nextInsertError: Throwable? = null

        override suspend fun findById(id: String): UserEntity? = storage[id]

        override suspend fun findByUsername(username: String): UserEntity? =
            storage.values.firstOrNull { it.username.equals(username, ignoreCase = true) }

        override suspend fun insert(user: UserEntity) {
            nextInsertError?.let {
                nextInsertError = null
                throw it
            }
            check(!storage.containsKey(user.id)) { "duplicate id" }
            storage[user.id] = user
        }

        companion object {
            val UNIQUE_EMAIL_VIOLATION: Throwable =
                RuntimeException(
                    "android.database.sqlite.SQLiteConstraintException: UNIQUE constraint failed: users.username " +
                        "(code 2067 SQLITE_CONSTRAINT_UNIQUE)",
                )
        }
    }
}
