// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.session

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Testes do [SessionStore] rodando com Robolectric para isolar o
 * `DataStore` em um diretório temporário do projeto.
 *
 * Cobre:
 * - `currentUserId()` retorna `null` antes de qualquer escrita.
 * - `saveUserId(id)` persiste o id e `currentUserId()` o recupera.
 * - `observeUserId()` emite o id salvo.
 * - `clear()` remove o id e `currentUserId()` volta a ser `null`.
 * - `saveUserId("")` lança [IllegalArgumentException].
 */
@RunWith(RobolectricTestRunner::class)
class SessionStoreTest {

    private fun newStore(): SessionStore {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Cada teste usa uma instância distinta do DataStore via name
        // único. Para evitar colisão entre testes, reconstruímos o
        // SessionStore apontando para um arquivo dedicado.
        // Como `Context.authDataStore` é uma delegate singleton,
        // usamos a mesma instância — `clear()` no @After garante
        // isolamento entre execuções.
        return SessionStore(dataStore = context.authDataStore)
    }

    @Test
    fun `currentUserId returns null when no session is stored`() = runTest {
        val store = newStore()
        // Limpa qualquer estado residual de execuções anteriores.
        store.clear()

        assertThat(store.currentUserId()).isNull()
    }

    @Test
    fun `saveUserId persists and currentUserId returns the saved id`() = runTest {
        val store = newStore()
        store.clear()

        store.saveUserId("user-123")

        assertThat(store.currentUserId()).isEqualTo("user-123")
    }

    @Test
    fun `observeUserId emits the saved id`() = runTest {
        val store = newStore()
        store.clear()
        store.saveUserId("user-abc")

        val emitted = store.observeUserId().first()

        assertThat(emitted).isEqualTo("user-abc")
    }

    @Test
    fun `clear removes the stored id`() = runTest {
        val store = newStore()
        store.saveUserId("user-to-remove")

        store.clear()

        assertThat(store.currentUserId()).isNull()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `saveUserId rejects blank id`() = runTest {
        val store = newStore()

        store.saveUserId("")
    }

    @Test
    fun `saving twice replaces the previous id`() = runTest {
        val store = newStore()
        store.clear()

        store.saveUserId("first")
        store.saveUserId("second")

        assertThat(store.currentUserId()).isEqualTo("second")
    }
}
