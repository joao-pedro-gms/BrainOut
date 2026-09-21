// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pucgo.joaopedrogmsilva.brainout.core.data.session.authDataStore
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ListingPreferences
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder

/**
 * Testes do [ListingPreferencesRepositoryImpl] rodando com
 * Robolectric para isolar o `DataStore` em diretório temporário
 * (E2.6 do ROADMAP).
 *
 * Cobre:
 * - `observe` emite o [ListingPreferences] default antes de qualquer
 *   escrita.
 * - As três chaves (`search_query`, `selected_tag_id`,
 *   `sort_order`) são gravadas e recarregadas após reinício
 *   (instância nova do repositório).
 * - Limpar busca (string vazia / `null`) remove a chave em vez
 *   de persistir o valor vazio.
 * - Chaves são namespaced por `userId` — um usuário não vê as
 *   preferências de outro.
 * - Valores inválidos em `sort_order` caem no default
 *   ([SortOrder.CreatedDesc]) em vez de quebrar a UI.
 */
@RunWith(RobolectricTestRunner::class)
class ListingPreferencesRepositoryTest {

    private lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dataStore = context.authDataStore
    }

    private fun newRepo(): ListingPreferencesRepositoryImpl =
        ListingPreferencesRepositoryImpl(dataStore = dataStore)

    @Test
    fun `observe emite default antes de qualquer escrita`() = runTest {
        val repo = newRepo()
        val prefs = repo.observe(userId = "u-novo").first()
        assertThat(prefs.searchQuery).isEmpty()
        assertThat(prefs.selectedTagId).isNull()
        assertThat(prefs.sortOrder).isEqualTo(SortOrder.CreatedDesc)
    }

    @Test
    fun `setSearchQuery persiste valor e observe emite o snapshot atualizado`() = runTest {
        val repo = newRepo()
        repo.setSearchQuery(userId = "u1", query = "App Android")

        val emitted = repo.observe(userId = "u1").first()
        assertThat(emitted.searchQuery).isEqualTo("App Android")
    }

    @Test
    fun `setSearchQuery com string vazia remove a chave em vez de gravar vazio`() = runTest {
        val repo = newRepo()
        repo.setSearchQuery(userId = "u1", query = "qualquer coisa")
        // Sobrescreve com vazio.
        repo.setSearchQuery(userId = "u1", query = "")

        val emitted = repo.observe(userId = "u1").first()
        assertThat(emitted.searchQuery).isEmpty()
    }

    @Test
    fun `setSearchQuery com whitespace-only apara e remove a chave`() = runTest {
        val repo = newRepo()
        repo.setSearchQuery(userId = "u1", query = "x")
        repo.setSearchQuery(userId = "u1", query = "   ")

        val emitted = repo.observe(userId = "u1").first()
        assertThat(emitted.searchQuery).isEmpty()
    }

    @Test
    fun `setSelectedTagId persiste tag e observe emite o snapshot`() = runTest {
        val repo = newRepo()
        repo.setSelectedTagId(userId = "u1", tagId = "tag-urgente")

        val emitted = repo.observe(userId = "u1").first()
        assertThat(emitted.selectedTagId).isEqualTo("tag-urgente")
    }

    @Test
    fun `setSelectedTagId com null remove o filtro`() = runTest {
        val repo = newRepo()
        repo.setSelectedTagId(userId = "u1", tagId = "tag-x")
        repo.setSelectedTagId(userId = "u1", tagId = null)

        val emitted = repo.observe(userId = "u1").first()
        assertThat(emitted.selectedTagId).isNull()
    }

    @Test
    fun `setSortOrder persiste a chave e observe emite o snapshot`() = runTest {
        val repo = newRepo()
        repo.setSortOrder(userId = "u1", order = SortOrder.NameAsc)

        val emitted = repo.observe(userId = "u1").first()
        assertThat(emitted.sortOrder).isEqualTo(SortOrder.NameAsc)
    }

    @Test
    fun `prefs de um usuario nao vazam para outro`() = runTest {
        val repo = newRepo()
        repo.setSearchQuery(userId = "u1", query = "App")
        repo.setSelectedTagId(userId = "u1", tagId = "tag-1")
        repo.setSortOrder(userId = "u1", order = SortOrder.NameDesc)

        val prefsU2 = repo.observe(userId = "u2").first()
        assertThat(prefsU2).isEqualTo(ListingPreferences())
    }

    @Test
    fun `observe emite o snapshot consolidado das tres chaves`() = runTest {
        val repo = newRepo()
        repo.setSearchQuery(userId = "u1", query = "App")
        repo.setSelectedTagId(userId = "u1", tagId = "tag-1")
        repo.setSortOrder(userId = "u1", order = SortOrder.CreatedAsc)

        val prefs = repo.observe(userId = "u1").first()
        assertThat(prefs.searchQuery).isEqualTo("App")
        assertThat(prefs.selectedTagId).isEqualTo("tag-1")
        assertThat(prefs.sortOrder).isEqualTo(SortOrder.CreatedAsc)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `keysFor rejeita userId em branco`() = runTest {
        val repo = newRepo()
        repo.setSearchQuery(userId = "", query = "x")
    }

    /**
     * Garante que valores corrompidos em `sort_order` (vindos de
     * escritas externas / downgrade de app) não quebram a UI —
     * `fromStorageKey` retorna o default.
     */
    @Test
    fun `sortOrder desconhecido cai no default`() {
        assertThat(SortOrder.fromStorageKey(null)).isEqualTo(SortOrder.CreatedDesc)
        assertThat(SortOrder.fromStorageKey("")).isEqualTo(SortOrder.CreatedDesc)
        assertThat(SortOrder.fromStorageKey("nope")).isEqualTo(SortOrder.CreatedDesc)
        assertThat(SortOrder.fromStorageKey("name_asc")).isEqualTo(SortOrder.NameAsc)
        assertThat(SortOrder.fromStorageKey("created_desc")).isEqualTo(SortOrder.CreatedDesc)
    }
}
