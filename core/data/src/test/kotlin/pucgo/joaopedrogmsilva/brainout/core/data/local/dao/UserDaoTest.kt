// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
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
 * - Update preserva PK e atualiza colunas.
 * - Delete remove linhas.
 * - `Flow` emite valores iniciais e após mutações.
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
        database = Room.inMemoryDatabaseBuilder(context, BrainOutDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.userDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert followed by findById returns same entity`() = runTest {
        val entity = sampleEntity(id = "id-1", email = "joao@example.com")

        dao.insert(entity)
        val loaded = dao.findById("id-1")

        assertThat(loaded).isEqualTo(entity)
    }

    @Test
    fun `findByEmail is case insensitive`() = runTest {
        val entity = sampleEntity(id = "id-1", email = "Joao@Example.com")
        dao.insert(entity)

        assertThat(dao.findByEmail("joao@example.com")).isEqualTo(entity)
        assertThat(dao.findByEmail("JOAO@EXAMPLE.COM")).isEqualTo(entity)
    }

    @Test
    fun `findById returns null when missing`() = runTest {
        assertThat(dao.findById("missing")).isNull()
    }

    @Test
    fun `update preserves primary key and applies changes`() = runTest {
        val original = sampleEntity(id = "id-1", name = "João", email = "joao@example.com")
        dao.insert(original)

        val updated = original.copy(name = "João Pedro")
        dao.update(updated)

        val loaded = dao.findById("id-1")
        assertThat(loaded).isEqualTo(updated)
        assertThat(loaded?.id).isEqualTo("id-1")
    }

    @Test
    fun `deleteById removes the row`() = runTest {
        dao.insert(sampleEntity(id = "id-1"))
        val affected = dao.deleteById("id-1")

        assertThat(affected).isEqualTo(1)
        assertThat(dao.findById("id-1")).isNull()
    }

    @Test
    fun `count reflects number of rows`() = runTest {
        assertThat(dao.count()).isEqualTo(0)
        dao.insert(sampleEntity(id = "id-1", email = "a@example.com"))
        dao.insert(sampleEntity(id = "id-2", email = "b@example.com"))
        assertThat(dao.count()).isEqualTo(2)
    }

    @Test
    fun `countByEmail is case insensitive`() = runTest {
        dao.insert(sampleEntity(id = "id-1", email = "Mix@Case.com"))
        assertThat(dao.countByEmail("mix@case.com")).isEqualTo(1)
        assertThat(dao.countByEmail("MIX@CASE.COM")).isEqualTo(1)
    }

    @Test
    fun `observeById emits null then entity then null after delete`() = runTest {
        val entity = sampleEntity(id = "id-1", email = "joao@example.com")

        // Emite null inicialmente (snapshot do Flow antes do insert).
        assertThat(dao.observeById("id-1").first()).isNull()

        dao.insert(entity)
        assertThat(dao.observeById("id-1").first()).isEqualTo(entity)

        dao.deleteById("id-1")
        assertThat(dao.observeById("id-1").first()).isNull()
    }

    @Test
    fun `observeByEmail is case insensitive`() = runTest {
        val entity = sampleEntity(id = "id-1", email = "Joao@Example.com")
        dao.insert(entity)
        assertThat(dao.observeByEmail("joao@example.com").first()).isEqualTo(entity)
    }

    @Test
    fun `deleteAll clears the table`() = runTest {
        dao.insert(sampleEntity(id = "id-1", email = "a@example.com"))
        dao.insert(sampleEntity(id = "id-2", email = "b@example.com"))

        dao.deleteAll()

        assertThat(dao.count()).isEqualTo(0)
    }

    @Test
    fun `upsert inserts when missing then updates when present`() = runTest {
        dao.upsert(sampleEntity(id = "id-1", name = "Original", email = "joao@example.com"))
        assertThat(dao.findById("id-1")?.name).isEqualTo("Original")

        dao.upsert(sampleEntity(id = "id-1", name = "Atualizado", email = "joao@example.com"))
        assertThat(dao.findById("id-1")?.name).isEqualTo("Atualizado")
        assertThat(dao.count()).isEqualTo(1)
    }

    companion object {
        private fun sampleEntity(
            id: String = "id-default",
            name: String = "João",
            email: String = "joao@example.com",
            role: String = "MEMBER",
            createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
        ): UserEntity = UserEntity(
            id = id,
            name = name,
            email = email,
            passwordHash = "pkbdf2_sha256\$120000\$AAAA\$BBBB",
            role = role,
            createdAt = createdAt,
        )
    }
}
