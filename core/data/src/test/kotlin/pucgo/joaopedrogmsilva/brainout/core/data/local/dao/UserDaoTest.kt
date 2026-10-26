// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.dao

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.UserEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.inMemoryDatabase
import pucgo.joaopedrogmsilva.brainout.core.data.repository.UserRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.domain.error.DuplicateUsernameException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import java.time.Instant

/**
 * Testes do [UserDao] rodando em Robolectric com banco em memória.
 *
 * Cobre os caminhos críticos:
 * - Insert + findById/findByUsername round-trip.
 * - Insert com e-mail duplicado é rejeitado.
 * - Busca case-insensitive via `LOWER(username)`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class UserDaoTest {
    private lateinit var database: BrainOutDatabase
    private lateinit var dao: UserDao

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        dao = database.userDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert followed by findById returns same entity`() =
        runTest {
            val entity = sampleEntity(id = "id-1", username = "joao")

            dao.insert(entity)
            val loaded = dao.findById("id-1")

            assertThat(loaded).isEqualTo(entity)
        }

    @Test
    fun `findByUsername distingue maiusculas`() =
        runTest {
            val entity = sampleEntity(id = "id-1", username = "Joao")
            dao.insert(entity)

            assertThat(dao.findByUsername("Joao")).isEqualTo(entity)
            assertThat(dao.findByUsername("joao")).isNull()
            assertThat(dao.findByUsername("JOAO")).isNull()
        }

    @Test
    fun `findById returns null when missing`() =
        runTest {
            assertThat(dao.findById("missing")).isNull()
        }

    @Test
    fun `insert with duplicate username is rejected`() =
        runTest {
            dao.insert(sampleEntity(id = "id-1", username = "joao"))

            val thrown =
                runCatching {
                    dao.insert(sampleEntity(id = "id-2", username = "joao"))
                }.exceptionOrNull()

            assertThat(thrown).isNotNull()
            assertThat(dao.findById("id-2")).isNull()
        }

    @Test
    fun `nomes distintos por caixa coexistem e cadastro concorrente respeita unicidade`() =
        runTest {
            dao.insert(sampleEntity(id = "upper", username = "Joao"))
            dao.insert(sampleEntity(id = "lower", username = "joao"))
            assertThat(dao.findByUsername("Joao")?.id).isEqualTo("upper")
            assertThat(dao.findByUsername("joao")?.id).isEqualTo("lower")
            val repository = UserRepositoryImpl(dao)
            val user = User.create("Unico", "hash")
            val attempts =
                coroutineScope {
                    listOf(
                        async(Dispatchers.Default) { runCatching { repository.save(user) } },
                        async(Dispatchers.Default) { runCatching { repository.save(user.copy(id = "second")) } },
                    ).map { it.await() }
                }
            assertThat(attempts.count { it.isSuccess }).isEqualTo(1)
            assertThat(
                attempts.single { it.isFailure }.exceptionOrNull(),
            ).isInstanceOf(DuplicateUsernameException::class.java)
        }

    companion object {
        private fun sampleEntity(
            id: String = "id-default",
            username: String = "joao",
            createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
        ): UserEntity =
            UserEntity(
                id = id,
                username = username,
                passwordHash = "pkbdf2_sha256\$120000\$AAAA\$BBBB",
                createdAt = createdAt,
            )
    }
}
