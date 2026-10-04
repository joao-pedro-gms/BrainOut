// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pucgo.joaopedrogmsilva.brainout.core.data.session.SessionStore
import pucgo.joaopedrogmsilva.brainout.core.data.session.authDataStore
import pucgo.joaopedrogmsilva.brainout.core.domain.model.ThemeMode
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ThemePreferencesRepository

/**
 * Testes do [ThemePreferencesRepositoryImpl] com Robolectric, no mesmo
 * arranjo do [ListingPreferencesRepositoryTest]: `DataStore` real sobre
 * o diretório temporário do teste (NB-06 / issue #93).
 *
 * Cobre o «preferência persistida» do critério de aceite:
 * - default `SYSTEM` antes de qualquer escrita;
 * - escrita sobrevive a reinício (instância nova do repositório);
 * - a escolha de tema é **de dispositivo**: sobrevive ao logout
 *   (`SessionStore.clear()` não pode apagar o modo do tema);
 * - chave corrompida cai no `SYSTEM` em vez de quebrar a UI;
 * - `observe` reflete cada uma das três escritas.
 */
@RunWith(RobolectricTestRunner::class)
class ThemePreferencesRepositoryTest {

    private lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dataStore = context.authDataStore
        // Chave única por app (não é namespaced por usuário), então o
        // teste zera o estado residual no início de cada caso.
        runBlocking {
            dataStore.edit { prefs ->
                prefs.remove(ThemePreferencesRepositoryImpl.THEME_MODE_KEY)
            }
        }
    }

    private fun newRepo(): ThemePreferencesRepositoryImpl =
        ThemePreferencesRepositoryImpl(dataStore = dataStore)

    @Test
    fun `observe emite SYSTEM antes de qualquer escrita`() = runTest {
        val repo: ThemePreferencesRepository = newRepo()

        assertThat(repo.observe().first()).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun `set persiste a escolha e observe emite o modo atual`() = runTest {
        val repo: ThemePreferencesRepository = newRepo()

        repo.set(ThemeMode.DARK)

        assertThat(repo.observe().first()).isEqualTo(ThemeMode.DARK)
    }

    @Test
    fun `escolha sobrevive a reinicio (nova instancia do repositorio)`() = runTest {
        newRepo().set(ThemeMode.LIGHT)

        val reopened = newRepo()

        assertThat(reopened.observe().first()).isEqualTo(ThemeMode.LIGHT)
    }

    @Test
    fun `observe reflete as tres escolhas em sequencia`() = runTest {
        val repo: ThemePreferencesRepository = newRepo()

        for (mode in ThemeMode.entries) {
            repo.set(mode)
            assertThat(repo.observe().first()).isEqualTo(mode)
        }
    }

    @Test
    fun `escolha sobrevive ao logout (SessionStore clear nao limpa o tema)`() = runTest {
        val repo: ThemePreferencesRepository = newRepo()
        repo.set(ThemeMode.DARK)

        SessionStore(dataStore = dataStore).clear()

        assertThat(repo.observe().first()).isEqualTo(ThemeMode.DARK)
    }

    @Test
    fun `chave corrompida cai no SYSTEM`() = runTest {
        dataStore.edit { prefs ->
            prefs[ThemePreferencesRepositoryImpl.THEME_MODE_KEY] = "neon"
        }

        assertThat(newRepo().observe().first()).isEqualTo(ThemeMode.SYSTEM)
    }
}
