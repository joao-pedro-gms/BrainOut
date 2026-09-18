// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.UserDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.UserEntity
import pucgo.joaopedrogmsilva.brainout.core.domain.error.DuplicateEmailException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole

/**
 * Testes do [UserRepositoryImpl] usando um fake de [UserDao].
 *
 * Cobre:
 * - Round-trip User ↔ UserEntity via `findByEmail`/`findById`.
 * - `save` lança [DuplicateEmailException] quando o DAO simula
 *   violação de `UNIQUE`.
 * - `observeById`/`observeByEmail` propagam `null` quando ausente.
 * - Trim do e-mail nas buscas.
 */
class UserRepositoryImplTest {

    @Test
    fun `save persists a User and findByEmail returns it`() = runTest {
        val dao = FakeUserDao()
        val repository = UserRepositoryImpl(dao)
        val user = sampleUser(email = "joao@example.com")

        val returned = repository.save(user)
        val loaded = repository.findByEmail("joao@example.com")

        assertThat(returned).isEqualTo(user)
        assertThat(loaded).isEqualTo(user)
    }

    @Test
    fun `findByEmail returns null when missing`() = runTest {
        val repository = UserRepositoryImpl(FakeUserDao())
        assertThat(repository.findByEmail("nobody@example.com")).isNull()
    }

    @Test
    fun `findById returns user when present`() = runTest {
        val dao = FakeUserDao()
        val user = sampleUser()
        dao.storage[user.id] = UserEntity.fromDomain(user)
        val repository = UserRepositoryImpl(dao)

        assertThat(repository.findById(user.id)).isEqualTo(user)
    }

    @Test
    fun `findById returns null when missing`() = runTest {
        val repository = UserRepositoryImpl(FakeUserDao())
        assertThat(repository.findById("missing")).isNull()
    }

    @Test
    fun `save translates unique violation into DuplicateEmailException`() = runTest {
        val dao = FakeUserDao().apply {
            nextInsertError = FakeUserDao.UNIQUE_EMAIL_VIOLATION
        }
        val repository = UserRepositoryImpl(dao)

        val thrown = runCatching {
            repository.save(sampleUser(email = "joao@example.com"))
        }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(DuplicateEmailException::class.java)
        assertThat((thrown as DuplicateEmailException).email).isEqualTo("joao@example.com")
    }

    @Test
    fun `save propagates non-unique errors unchanged`() = runTest {
        val generic = IllegalStateException("boom")
        val dao = FakeUserDao().apply { nextInsertError = generic }
        val repository = UserRepositoryImpl(dao)

        val thrown = runCatching {
            repository.save(sampleUser())
        }.exceptionOrNull()

        assertThat(thrown).isSameInstanceAs(generic)
    }

    @Test
    fun `findByEmail trims whitespace`() = runTest {
        val dao = FakeUserDao()
        val user = sampleUser(email = "joao@example.com")
        dao.storage[user.id] = UserEntity.fromDomain(user)
        val repository = UserRepositoryImpl(dao)

        assertThat(repository.findByEmail("  joao@example.com  ")).isEqualTo(user)
    }

    @Test
    fun `observeById emits null then user`() = runTest {
        val dao = FakeUserDao()
        val repository = UserRepositoryImpl(dao)
        val user = sampleUser()

        // Emite null antes de qualquer inserção (snapshot do Flow).
        assertThat(repository.observeById(user.id).first()).isNull()

        dao.simulateInsert(user)
        assertThat(repository.observeById(user.id).first()).isEqualTo(user)
    }

    @Test
    fun `observeByEmail emits user after insertion`() = runTest {
        val dao = FakeUserDao()
        val repository = UserRepositoryImpl(dao)
        val user = sampleUser(email = "joao@example.com")

        assertThat(repository.observeByEmail("joao@example.com").first()).isNull()

        dao.simulateInsert(user)
        assertThat(repository.observeByEmail("joao@example.com").first()).isEqualTo(user)
    }

    private fun sampleUser(
        id: String = UUID.randomUUID().toString(),
        name: String = "João",
        email: String = "joao@example.com",
        role: UserRole = UserRole.MEMBER,
        createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    ): User = User(
        id = id,
        name = name,
        email = email,
        passwordHash = "pbkdf2_sha256\$120000\$AAAA\$BBBB",
        role = role,
        createdAt = createdAt,
    )

    /**
     * Implementação fake de [UserDao] suficiente para exercitar o
     * repositório sem precisar do Room/Robolectric. Assegura
     * equivalência por id/email e mantém os `Flow`s sincronizados
     * com o `storage`.
     */
    private class FakeUserDao : UserDao {
        val storage: MutableMap<String, UserEntity> = mutableMapOf()
        val flows: MutableMap<String, MutableStateFlow<UserEntity?>> = mutableMapOf()

        var nextInsertError: Throwable? = null

        override fun observeById(id: String): Flow<UserEntity?> =
            flows.getOrPut("byId:$id") { MutableStateFlow(storage[id]) }

        override fun observeByEmail(email: String): Flow<UserEntity?> {
            val key = "byEmail:${email.lowercase()}"
            return flows.getOrPut(key) {
                MutableStateFlow(storage.values.firstOrNull { it.email.equals(email, ignoreCase = true) })
            }
        }

        override suspend fun findById(id: String): UserEntity? = storage[id]

        override suspend fun findByEmail(email: String): UserEntity? =
            storage.values.firstOrNull { it.email.equals(email, ignoreCase = true) }

        override suspend fun countByEmail(email: String): Int =
            storage.values.count { it.email.equals(email, ignoreCase = true) }

        override suspend fun insert(user: UserEntity) {
            nextInsertError?.let {
                nextInsertError = null
                throw it
            }
            check(!storage.containsKey(user.id)) { "duplicate id" }
            storage[user.id] = user
            notifyFlowsChanged(user)
        }

        override suspend fun upsert(user: UserEntity) {
            storage[user.id] = user
            notifyFlowsChanged(user)
        }

        override suspend fun update(user: UserEntity) {
            storage[user.id] = user
            notifyFlowsChanged(user)
        }

        override suspend fun delete(user: UserEntity) {
            storage.remove(user.id)
            notifyFlowsChanged(user.copy(id = user.id))
        }

        override suspend fun deleteById(id: String): Int =
            if (storage.remove(id) != null) 1 else 0

        override suspend fun count(): Int = storage.size

        override suspend fun deleteAll() {
            val removed = storage.keys.toList()
            storage.clear()
            removed.forEach { id ->
                flows["byId:$id"]?.value = null
            }
        }

        private fun notifyFlowsChanged(user: UserEntity) {
            flows["byId:${user.id}"]?.value = storage[user.id]
            flows["byEmail:${user.email.lowercase()}"]?.value = storage[user.id]
        }

        /** Helper de teste: persiste um User atualizando os Flows. */
        fun simulateInsert(user: User) {
            storage[user.id] = UserEntity.fromDomain(user)
            notifyFlowsChanged(UserEntity.fromDomain(user))
        }

        companion object {
            val UNIQUE_EMAIL_VIOLATION: Throwable = RuntimeException(
                "android.database.sqlite.SQLiteConstraintException: UNIQUE constraint failed: users.email " +
                    "(code 2067 SQLITE_CONSTRAINT_UNIQUE)",
            )
        }
    }
}
