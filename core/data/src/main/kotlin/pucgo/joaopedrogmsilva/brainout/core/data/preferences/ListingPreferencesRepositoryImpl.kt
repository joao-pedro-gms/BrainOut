// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ListingPreferences
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ListingPreferencesRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder

/**
 * Implementação DataStore (Preferences) de [ListingPreferencesRepository]
 * (E2.6 do ROADMAP).
 *
 * Reaproveita o [DataStore]<[Preferences]> singleton do módulo
 * (`auth_prefs`) já provido pelo `:core:data` para a sessão —
 * preferências de listagem não têm volume nem frequência que
 * justifiquem um arquivo separado, e o `SessionStore.clear()`
 * preserva intencionalmente estas chaves (não é "clear all", é
 * apenas logout).
 *
 * As chaves são namespaced por `userId` para isolar contas no
 * mesmo dispositivo. O `clear()` do `SessionStore` apenas remove
 * `user_id` e a flag de permissão de notificação — `search_query`,
 * `selected_tag_id` e `sort_order` de cada usuário permanecem
 * intactos para que o próximo login encontre o snapshot anterior.
 *
 * @property dataStore instância do `DataStore<Preferences>` injetada
 *  pelo Hilt via [pucgo.joaopedrogmsilva.brainout.core.data.di.DataModule].
 */
@Singleton
class ListingPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ListingPreferencesRepository {

    override fun observe(userId: String): Flow<ListingPreferences> {
        val keys = keysFor(userId)
        return dataStore.data.map { prefs ->
            ListingPreferences(
                searchQuery = prefs[keys.searchQuery] ?: "",
                selectedTagId = prefs[keys.selectedTagId],
                sortOrder = SortOrder.fromStorageKey(prefs[keys.sortOrder]),
            )
        }
    }

    override suspend fun setSearchQuery(userId: String, query: String) {
        val key = keysFor(userId).searchQuery
        dataStore.edit { prefs ->
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                prefs.remove(key)
            } else {
                prefs[key] = trimmed
            }
        }
    }

    override suspend fun setSelectedTagId(userId: String, tagId: String?) {
        val key = keysFor(userId).selectedTagId
        dataStore.edit { prefs ->
            if (tagId.isNullOrBlank()) {
                prefs.remove(key)
            } else {
                prefs[key] = tagId
            }
        }
    }

    override suspend fun setSortOrder(userId: String, order: SortOrder) {
        val key = keysFor(userId).sortOrder
        dataStore.edit { prefs ->
            prefs[key] = order.toStorageKey()
        }
    }

    /**
     * Estrutura com as três chaves namespaced por `userId`. Mantida
     * interna porque o prefixo `"listing_$userId"` é detalhe de
     * implementação deste repositório.
     */
    private data class PrefKeys(
        val searchQuery: Preferences.Key<String>,
        val selectedTagId: Preferences.Key<String>,
        val sortOrder: Preferences.Key<String>,
    )

    private fun keysFor(userId: String): PrefKeys {
        require(userId.isNotBlank()) { "userId não pode ser vazio" }
        val prefix = "listing_${userId}_"
        return PrefKeys(
            searchQuery = stringPreferencesKey("${prefix}search_query"),
            selectedTagId = stringPreferencesKey("${prefix}selected_tag_id"),
            sortOrder = stringPreferencesKey("${prefix}sort_order"),
        )
    }
}
