// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// DEF-23b — `home_filter_aria_label` é um "rótulo de acessibilidade
// oculto" que estava **visível**.
//
// O `Text` que consumia `home_filter_aria_label` não tinha
// `clearAndSetSemantics`, nem `alpha = 0f`, nem `graphicsLayer`: nada o
// escondia. Ele renderizava "Filtrar projetos" solto entre os chips de
// filtro e o ícone de ordenação (lixo visual), e o TalkBack lia esse
// texto avulso em vez de um agrupamento com nome.
//
// A correção trocou o `Text` por um nó de grupo com
// `contentDescription` + `stateDescription` (DESIGN.md §6). Estes testes
// fixam os dois lados do invariante:
//
//   1. o texto NÃO existe na árvore — nem como texto, nem como nó
//      avulso de descrição (é o que prova o defeito);
//   2. o GRUPO se anuncia com nome + seleção atual, e a seleção muda
//      quando o filtro muda;
//   3. os chips seguem focáveis e clicáveis individualmente.
//
// Nota de API: `assertDoesNotExist()` NÃO existe no Compose 1.7.5
// (verificado com `javap` em `ui-test-android-1.7.5.aar`); o equivalente
// é contar os nós com `onAllNodes(...).fetchSemanticsNodes()`, que é o
// que estes testes fazem.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme

/**
 * Cobre o agrupamento de filtros da Home sem montar o `HomeViewModel`
 * (Hilt): `HomeProjectFilterRow` é a unidade testável e é exatamente onde
 * o defeito mora.
 */
@RunWith(AndroidJUnit4::class)
// pt-rBR: resolve as strings do `values/` padrão ("Filtrar projetos",
// "Ativos", "Concluídos"). Com o locale default do Robolectric (en),
// `values-en` venceria a resolução.
@Config(sdk = [34], qualifiers = "pt-rBR")
class HomeFilterRowTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setRow(
        current: HomeProjectFilter = HomeProjectFilter.Active,
        onSelect: (HomeProjectFilter) -> Unit = {},
    ) {
        composeRule.setContent {
            BrainOutTheme {
                Surface {
                    HomeProjectFilterRow(
                        current = current,
                        onSelect = onSelect,
                        sortOrder = SortOrder.NameAsc,
                        onSortOrderChange = {},
                    )
                }
            }
        }
    }

    /** Nó do agrupamento, para ler `contentDescription`/`stateDescription`. */
    private fun groupConfig() =
        composeRule
            .onNodeWithTag(HomeTestTags.FILTER_GROUP)
            .fetchSemanticsNode()
            .config

    @Test
    fun `o rotulo de acessibilidade nao renderiza texto na tela`() {
        setRow()

        // O defeito: este texto era desenhado. Nenhum nó o carrega como
        // texto agora.
        val asText =
            composeRule
                .onAllNodes(hasText("Filtrar projetos"))
                .fetchSemanticsNodes()

        assertThat(asText).isEmpty()
    }

    @Test
    fun `somente o grupo carrega o rotulo como descricao`() {
        setRow()

        // O rótulo continua alcançável pelo leitor de tela, mas só pelo
        // nó do grupo — nunca por um nó de texto avulso.
        val asContentDescription =
            composeRule
                .onAllNodes(hasContentDescription("Filtrar projetos"))
                .fetchSemanticsNodes()

        assertThat(asContentDescription).hasSize(1)
        assertThat(
            asContentDescription.single().config[SemanticsProperties.TestTag],
        ).isEqualTo(HomeTestTags.FILTER_GROUP)
    }

    @Test
    fun `o grupo de filtros se anuncia com nome e filtro ativo`() {
        setRow(current = HomeProjectFilter.Active)

        // O `Row` do agrupamento carrega `contentDescription` (nome) e
        // `stateDescription` (seleção atual) — a receita do DESIGN.md §6.
        val config = groupConfig()

        assertThat(config.getOrNull(SemanticsProperties.ContentDescription))
            .contains("Filtrar projetos")
        assertThat(config.getOrNull(SemanticsProperties.StateDescription))
            .isEqualTo("Ativos")
    }

    @Test
    fun `a selecao anunciada no grupo acompanha o filtro escolhido`() {
        // Concluídos é o outro valor de `HomeProjectFilter`: o
        // `stateDescription` tem que mudar junto.
        setRow(current = HomeProjectFilter.Completed)

        assertThat(groupConfig().getOrNull(SemanticsProperties.StateDescription))
            .isEqualTo("Concluídos")
    }

    @Test
    fun `os chips continuam focaveis individualmente`() {
        setRow()

        // A correção NÃO pode ter fundido os chips num alvo único: o
        // usuário de leitor de tela precisa alcançar cada filtro.
        composeRule
            .onNodeWithTag(HomeTestTags.FILTER_ACTIVE)
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag(HomeTestTags.FILTER_COMPLETED)
            .assertIsDisplayed()
    }

    @Test
    fun `o clique no chip ainda dispara o callback de selecao`() {
        var selected: HomeProjectFilter? = null
        setRow(current = HomeProjectFilter.Active, onSelect = { selected = it })

        composeRule
            .onNodeWithTag(HomeTestTags.FILTER_COMPLETED)
            .performClick()

        assertThat(selected).isEqualTo(HomeProjectFilter.Completed)
    }
}
