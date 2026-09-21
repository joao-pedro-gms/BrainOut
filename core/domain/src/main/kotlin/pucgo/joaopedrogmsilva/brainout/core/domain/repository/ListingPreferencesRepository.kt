// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Snapshot das preferências de listagem da aba Projetos (E2.6 do
 * ROADMAP).
 *
 * Persistido por usuário ativo em `DataStore` (Preferences), de
 * modo que o estado da busca/filtro/ordenação sobreviva a:
 * - rotação de tela,
 * - navegação entre abas,
 * - logout/login (desde que o `userId` se mantenha).
 *
 * Implementação concreta em `:core:data` (veja
 * [pucgo.joaopedrogmsilva.brainout.core.data.preferences.ListingPreferencesRepositoryImpl]).
 *
 * @property searchQuery texto livre da busca por nome de projeto.
 *  Vazio significa "sem filtro de texto".
 * @property selectedTagId id da tag selecionada como filtro, ou
 *  `null` para "todas as tags".
 * @property sortOrder chave da ordenação — ver [SortOrder] para os
 *  valores aceitos.
 */
data class ListingPreferences(
    val searchQuery: String = "",
    val selectedTagId: String? = null,
    val sortOrder: SortOrder = SortOrder.CreatedDesc,
)

/**
 * Ordenações válidas para a listagem de projetos (E2.6).
 *
 * Mantido como enum para que a UI use `when` exaustivo na hora de
 * mapear para rótulos localizados e para que o repositório valide a
 * chave antes de gravá-la (defesa contra entradas inválidas via
 * `DataStore`).
 *
 * A serialização em `DataStore` usa o nome do enum em
 * minúsculas (`name.lowercase()`).
 */
enum class SortOrder {
    NameAsc,
    NameDesc,
    CreatedDesc,
    CreatedAsc,
    ;

    /** Serialização textual usada em `DataStore`. */
    fun toStorageKey(): String = name.lowercase()

    companion object {
        /** Desserializa uma chave de `DataStore`, com fallback seguro. */
        fun fromStorageKey(value: String?): SortOrder = when (value) {
            "nameasc", "name_asc" -> NameAsc
            "namedesc", "name_desc" -> NameDesc
            "createddesc", "created_desc" -> CreatedDesc
            "createdasc", "created_asc" -> CreatedAsc
            null, "" -> CreatedDesc
            else -> CreatedDesc
        }
    }
}

/**
 * Contrato de persistência das preferências de listagem por
 * usuário (E2.6). As implementações devem:
 * - manter um snapshot por `userId` (não há interferência entre
 *   contas no mesmo dispositivo);
 * - expor um `Flow<ListingPreferences>` que reflete o estado mais
 *   recente da chave;
 * - aceitar `String` vazia e `null` em [setSearchQuery] e
 *   [setSelectedTagId] para limpar o filtro.
 */
interface ListingPreferencesRepository {

    /**
     * Observa as preferências de listagem para o [userId]. Se nunca
     * houve escrita, emite o [ListingPreferences] default (sem
     * busca, todas as tags, mais recentes primeiro).
     */
    fun observe(userId: String): Flow<ListingPreferences>

    /** Atualiza a query de busca (string vazia = limpar). */
    suspend fun setSearchQuery(userId: String, query: String)

    /**
     * Atualiza a tag selecionada como filtro (`null` = limpar o
     * filtro, mostrar todas).
     */
    suspend fun setSelectedTagId(userId: String, tagId: String?)

    /** Atualiza a ordenação. Valores fora de [SortOrder] caem no default. */
    suspend fun setSortOrder(userId: String, order: SortOrder)
}
