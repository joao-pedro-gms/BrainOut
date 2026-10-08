// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.UserEntity
import java.time.Instant

/**
 * Teste instrumentado do [UserDao] usando `Room.inMemoryDatabaseBuilder`
 * no dispositivo/emulador. Verifica de ponta a ponta que a geração de
 * SQL pelo Room + a abertura do DB funcionam no Android real.
 *
 * Não há auto-pulo (`assumeTrue`) aqui: um teste instrumentado só roda
 * com dispositivo/emulador, então um guarda desse tipo nunca exercita o
 * caminho pretendido e apenas mascara a ausência de cobertura. O
 * `InstrumentationRegistry` falha alto e explícito quando não há
 * runtime Android — que é exatamente o diagnóstico correto.
 *
 * A cobertura de [UserDao] no escopo JVM continua garantida por
 * `UserDaoTest` rodando em Robolectric em `:core:data:test`.
 */
@RunWith(AndroidJUnit4::class)
class UserDaoInstrumentedTest {
    private lateinit var database: BrainOutDatabase
    private lateinit var dao: UserDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database =
            Room
                .inMemoryDatabaseBuilder(context, BrainOutDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = database.userDao()
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
    }

    @Test
    fun insert_and_findByEmail_returns_inserted_entity() =
        runTest {
            val entity =
                UserEntity(
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
