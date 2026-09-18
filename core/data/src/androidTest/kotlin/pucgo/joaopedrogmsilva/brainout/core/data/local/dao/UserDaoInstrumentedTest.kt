// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.UserEntity
import java.time.Instant

/**
 * Smoke instrumentado do [UserDao] usando `Room.inMemoryDatabaseBuilder`
 * no dispositivo/emulador. Verifica de ponta a ponta que a geração de
 * SQL pelo Room + a abertura do DB funcionam no Android real.
 *
 * Em ambientes sem emulador (CI sem AVD, `adb` indisponível), o teste
 * é pulado via `assumeTrue` para não quebrar a pipeline — a cobertura
 * completa continua sendo garantida por [UserDaoTest] rodando em
 * Robolectric no escopo unitário.
 */
@RunWith(AndroidJUnit4::class)
class UserDaoInstrumentedTest {

    private lateinit var database: BrainOutDatabase
    private lateinit var dao: UserDao

    @Before
    fun setUp() {
        // Pula se não houver um Android Runtime válido (ex.: CI sem
        // emulador configurado). `InstrumentationRegistry` lança
        // `IllegalStateException` em ambiente JVM puro.
        val context = try {
            InstrumentationRegistry.getInstrumentation().targetContext
        } catch (error: IllegalStateException) {
            assumeTrue(false, "Sem InstrumentationRegistry: ${error.message}")
            return
        }
        database = Room.inMemoryDatabaseBuilder(context, BrainOutDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.userDao()
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
    }

    @Test
    fun insert_and_findByEmail_returns_inserted_entity() = runTest {
        val entity = UserEntity(
            id = "id-1",
            name = "João",
            email = "joao@example.com",
            passwordHash = "x",
            role = "MEMBER",
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        )

        dao.insert(entity)
        val loaded = dao.findByEmail("joao@example.com")

        assertThat(loaded).isEqualTo(entity)
    }
}
