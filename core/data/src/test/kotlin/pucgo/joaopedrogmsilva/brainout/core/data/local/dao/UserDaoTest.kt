// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.UserEntity
import java.time.Instant

/**
 * Testes do [UserDao] rodando em Robolectric com banco em memória.
 *
 * Cobre os caminhos críticos:
 * - Insert + findById/findByEmail round-trip.
 * - Insert com e-mail duplicado é rejeitado.
 * - Busca case-insensitive via `LOWER(email)`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class UserDaoTest {
    private lateinit var database: BrainOutDatabase
    private lateinit var dao: UserDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, BrainOutDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = database.userDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert followed by findById returns same entity`() =
        runTest {
            val entity = sampleEntity(id = "id-1", email = "joao@example.com")

            dao.insert(entity)
            val loaded = dao.findById("id-1")

            assertThat(loaded).isEqualTo(entity)
        }

    @Test
    fun `findByEmail is case insensitive`() =
        runTest {
            val entity = sampleEntity(id = "id-1", email = "Joao@Example.com")
            dao.insert(entity)

            assertThat(dao.findByEmail("joao@example.com")).isEqualTo(entity)
            assertThat(dao.findByEmail("JOAO@EXAMPLE.COM")).isEqualTo(entity)
        }

    @Test
    fun `findById returns null when missing`() =
        runTest {
            assertThat(dao.findById("missing")).isNull()
        }

    @Test
    fun `insert with duplicate email is rejected`() =
        runTest {
            dao.insert(sampleEntity(id = "id-1", email = "joao@example.com"))

            val thrown =
                runCatching {
                    dao.insert(sampleEntity(id = "id-2", email = "joao@example.com"))
                }.exceptionOrNull()

            assertThat(thrown).isNotNull()
            assertThat(dao.findById("id-2")).isNull()
        }

    companion object {
        private fun sampleEntity(
            id: String = "id-default",
            name: String = "João",
            email: String = "joao@example.com",
            role: String = "MEMBER",
            createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
        ): UserEntity =
            UserEntity(
                id = id,
                name = name,
                email = email,
                passwordHash = "pkbdf2_sha256\$120000\$AAAA\$BBBB",
                role = role,
                createdAt = createdAt,
            )
    }
}
