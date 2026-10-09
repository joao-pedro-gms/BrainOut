// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.repository

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Contrato de serialização de [SortOrder].
 *
 * Regressão do bug de ordenação da Home: `toStorageKey()` emitia
 * `name.lowercase()` (`nameasc`, `namedesc`, `createddesc`,
 * `createdasc`), enquanto a ordenação dinâmica de
 * `ProjectDao.searchProjects` compara `name_asc`, `name_desc`,
 * `created_desc` e `created_asc`. Nenhum dos quatro valores casava,
 * os quatro `CASE WHEN` devolviam `NULL` e a ordenação escolhida
 * pelo usuário nunca tinha efeito.
 *
 * A chave gravada é o contrato compartilhado entre a preferência
 * persistida em `DataStore` e a comparação feita em SQL, então
 * estes testes fixam a grafia canônica e o round-trip.
 */
class SortOrderStorageKeyTest {
    @Test
    fun `toStorageKey emite a grafia snake_case esperada pela ordenacao em SQL`() {
        assertThat(SortOrder.NameAsc.toStorageKey()).isEqualTo("name_asc")
        assertThat(SortOrder.NameDesc.toStorageKey()).isEqualTo("name_desc")
        assertThat(SortOrder.CreatedDesc.toStorageKey()).isEqualTo("created_desc")
        assertThat(SortOrder.CreatedAsc.toStorageKey()).isEqualTo("created_asc")
    }

    @Test
    fun `toStorageKey nao emite mais a grafia concatenada que quebrava o ORDER BY`() {
        val legadas = setOf("nameasc", "namedesc", "createddesc", "createdasc")

        assertThat(SortOrder.entries.map { it.toStorageKey() }).containsNoneIn(legadas)
    }

    @Test
    fun `fromStorageKey faz round-trip de toStorageKey em todas as ordenacoes`() {
        SortOrder.entries.forEach { order ->
            assertThat(SortOrder.fromStorageKey(order.toStorageKey())).isEqualTo(order)
        }
    }

    /**
     * Preferências escritas por versões anteriores usavam a grafia
     * concatenada; elas continuam legíveis para que o snapshot já
     * gravado no dispositivo não se perca no primeiro upgrade.
     */
    @Test
    fun `fromStorageKey ainda aceita a grafia concatenada legada`() {
        assertThat(SortOrder.fromStorageKey("nameasc")).isEqualTo(SortOrder.NameAsc)
        assertThat(SortOrder.fromStorageKey("namedesc")).isEqualTo(SortOrder.NameDesc)
        assertThat(SortOrder.fromStorageKey("createddesc")).isEqualTo(SortOrder.CreatedDesc)
        assertThat(SortOrder.fromStorageKey("createdasc")).isEqualTo(SortOrder.CreatedAsc)
    }

    @Test
    fun `fromStorageKey cai no default para ausente, vazio ou corrompido`() {
        assertThat(SortOrder.fromStorageKey(null)).isEqualTo(SortOrder.CreatedDesc)
        assertThat(SortOrder.fromStorageKey("")).isEqualTo(SortOrder.CreatedDesc)
        assertThat(SortOrder.fromStorageKey("lixo")).isEqualTo(SortOrder.CreatedDesc)
        assertThat(SortOrder.fromStorageKey("NAME_ASC")).isEqualTo(SortOrder.CreatedDesc)
    }

    @Test
    fun `cada valor do enum tem chave distinta`() {
        val keys = SortOrder.entries.map { it.toStorageKey() }

        assertThat(keys).containsNoDuplicates()
        assertThat(keys).hasSize(SortOrder.entries.size)
    }

    @Test
    fun `o default de ListingPreferences serializa para a chave created_desc`() {
        assertThat(ListingPreferences().sortOrder.toStorageKey()).isEqualTo("created_desc")
    }
}
