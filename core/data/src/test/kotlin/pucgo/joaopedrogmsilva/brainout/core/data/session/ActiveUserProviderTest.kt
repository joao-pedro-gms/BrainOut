// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.session

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.UserRepository

/**
 * Testes do [ActiveUserProvider] — a fonte de "quem está logado" que
 * alimenta a Home, a rota inicial e o gate de permissão do domínio
 * (`requirePermission`).
 *
 * Cobre:
 * - sem sessão, o provider não emite usuário.
 * - com sessão válida, emite o usuário reidratado do Room.
 * - **sessão órfã** (id persistido mas usuário inexistente) emite
 *   `null` *e* limpa o id — sem isso, o app ficaria num laço de
 *   "sessão inválida" que nunca volta para o login.
 * - `currentActiveUser()` devolve o usuário do ponto, ou `null`.
 * - `signOut()` encerra a sessão.
 */
@RunWith(RobolectricTestRunner::class)
class ActiveUserProviderTest {
    private lateinit var sessionStore: SessionStore
    private lateinit var userRepository: FakeUserRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        sessionStore = SessionStore(dataStore = context.authDataStore)
        userRepository = FakeUserRepository()
    }

    @After
    fun tearDown() {
        // `clearAll` é suspend; o teardown roda fora de corrotina.
        runBlocking { sessionStore.clearAll() }
    }

    private fun provider() = ActiveUserProvider(sessionStore, userRepository)

    private fun usuario(id: String): User {
        val base =
            User.create(
                name = "Pessoa",
                email = "pessoa@example.com",
                passwordHash = "hash",
                role = UserRole.OWNER,
            )
        return base.copy(id = id)
    }

    @Test
    fun `sem sessao o provider nao emite usuario`() =
        runTest {
            sessionStore.clearAll()

            assertThat(provider().observeActiveUser().first()).isNull()
        }

    @Test
    fun `com sessao valida o provider emite o usuario reidratado`() =
        runTest {
            sessionStore.clearAll()
            sessionStore.saveUserId("u1")
            userRepository.usuarios["u1"] = usuario("u1")

            assertThat(provider().observeActiveUser().first()?.id).isEqualTo("u1")
        }

    @Test
    fun `sessao orfa e limpa e o provider emite null`() =
        runTest {
            sessionStore.clearAll()
            sessionStore.saveUserId("u-inexistente")

            assertThat(provider().observeActiveUser().first()).isNull()
            // O id órfão não pode ficar pendurado: sem isso, cada
            // tentativa de reabrir o app cairia no mesmo beco.
            assertThat(sessionStore.currentUserId()).isNull()
        }

    @Test
    fun `currentActiveUser devolve o usuario do ponto`() =
        runTest {
            sessionStore.clearAll()
            sessionStore.saveUserId("u1")
            userRepository.usuarios["u1"] = usuario("u1")

            assertThat(provider().currentActiveUser()?.id).isEqualTo("u1")
        }

    @Test
    fun `currentActiveUser devolve null sem sessao`() =
        runTest {
            sessionStore.clearAll()

            assertThat(provider().currentActiveUser()).isNull()
        }

    @Test
    fun `signOut encerra a sessao`() =
        runTest {
            sessionStore.clearAll()
            sessionStore.saveUserId("u1")

            provider().signOut()

            assertThat(sessionStore.currentUserId()).isNull()
        }

    @Test
    fun `observeActiveUserId propaga o id do usuario ativo`() =
        runTest {
            sessionStore.clearAll()
            sessionStore.saveUserId("u1")
            userRepository.usuarios["u1"] = usuario("u1")

            assertThat(provider().observeActiveUserId().first()).isEqualTo("u1")
        }

    @Test
    fun `o provider nao consulta o Room quando nao ha id salvo`() =
        runTest {
            sessionStore.clearAll()

            provider().currentActiveUser()

            assertThat(userRepository.consultas).isEmpty()
        }
}

/**
 * Falso [UserRepository] com um mapa em memória.
 *
 * `:core:data` não usa MockK nos testes (a convenção do módulo é
 * fake explícito para os DAOs) — e um mapa resolve o caso inteiro,
 * que é "o usuário existe ou não".
 */
private class FakeUserRepository : UserRepository {
    val usuarios: MutableMap<String, User> = mutableMapOf()

    /** Ids consultados — para provar que o Room não foi tocado. */
    val consultas: MutableList<String> = mutableListOf()

    override suspend fun findByEmail(email: String): User? = usuarios.values.firstOrNull { it.email == email }

    override suspend fun save(user: User): User {
        usuarios[user.id] = user
        return user
    }

    override suspend fun findById(id: String): User? {
        consultas.add(id)
        return usuarios[id]
    }
}
