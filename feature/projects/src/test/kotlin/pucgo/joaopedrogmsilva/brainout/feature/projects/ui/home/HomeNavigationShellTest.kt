// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// RESP-1 (Fase 1.2, D-02) — testes da casca de navegação da Home.
//
// Antes desta Fase a Home renderizava `NavigationBar` em qualquer
// largura (`HomeBottomBar.kt:29`), então um tablet recebia uma barra
// espremida no rodapé. Agora a escolha é do `NavigationSuiteScaffold`,
// e estes testes fixam as duas metades do contrato:
//
//  - **AC-1.2** o componente acompanha a largura: 599dp → barra no
//    rodapé, 600dp → rail na lateral. Verificado por
//    `@Config(qualifiers = ...)`, sem device.
//  - **AC-1.3/AC-1.4** os 4 destinos continuam existindo, com tag
//    própria, e cada um dispara o callback com a aba certa.
//
// A distinção bar/rail não é feita por tipo: `NavigationSuiteType` é
// `@JvmInline value class` com construtor **privado**, então não dá para
// instanciar nem comparar por nome. O que dá para medir — e é o que o
// usuário vê — é a **geometria** do item: barra fica no rodapé (y
// alto), rail fica na lateral (x baixo). Assertar a posição é mais
// fiel ao AC-1.2 do que assertar o tipo seria.
//
// O teste monta [HomeNavigationShell] direto, sem o ViewModel do Hilt:
// a casca não conhece o ViewModel, só `currentTab`/`onSelectTab`, então
// testá-la isolada é mais fiel (e mais rápido) do que montar a
// `HomeScreen` inteira.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme

/** Tag do corpo — só para o teste saber onde a suíte termina. */
private const val BODY_TAG = "resp1_shell_body"

@RunWith(AndroidJUnit4::class)
// Os asserts são por `testTag` e geometria; o locale padrão do
// Robolectric (en) não interfere neles — nenhum texto é assertado.
@Config(sdk = [34], qualifiers = "w599dp-h800dp")
class HomeNavigationShellTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Composable
    private fun Body() {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .testTag(BODY_TAG),
        ) {
            Text(
                text = "corpo",
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    private fun setShell(
        currentTab: HomeTab = HomeTab.Projects,
        onSelectTab: (HomeTab) -> Unit,
    ) {
        composeRule.setContent {
            BrainOutTheme {
                Surface {
                    HomeNavigationShell(
                        currentTab = currentTab,
                        onSelectTab = onSelectTab,
                    ) {
                        Body()
                    }
                }
            }
        }
    }

    @Test
    fun `a 599dp os quatro destinos existem na suíte`() {
        setShell(onSelectTab = {})

        // AC-1.4 — os 4 destinos são alcançáveis pela tag própria, uma
        // a uma. Assertar cada tag individualmente é mais forte que
        // contar nós por prefixo: se uma aba perder a tag, o teste
        // aponta qual.
        HomeTab.entries.forEach { tab ->
            composeRule.onNodeWithTag(tab.testTag()).assertIsDisplayed()
        }
    }

    @Test
    fun `cada destino dispara o callback com a aba correta`() {
        val clicks = mutableListOf<HomeTab>()
        setShell(onSelectTab = { clicks += it })

        HomeTab.entries.forEach { tab ->
            composeRule.onNodeWithTag(tab.testTag()).performClick()
            composeRule.waitForIdle()
        }

        // AC-1.3 — os 4 destinos continuam funcionando, na ordem certa.
        assertThat(clicks).containsExactlyElementsIn(HomeTab.entries).inOrder()
    }

    @Test
    fun `a aba ativa e a unica selecionada`() {
        setShell(currentTab = HomeTab.Tasks, onSelectTab = {})

        val isSelected = SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)
        HomeTab.entries.forEach { tab ->
            val selected =
                composeRule
                    .onNode(hasTestTag(tab.testTag()))
                    .fetchSemanticsNode()
                    .config[SemanticsProperties.Selected]
            assertThat(selected).isEqualTo(tab == HomeTab.Tasks)
        }
        composeRule.onNode(hasTestTag(HomeTab.Tasks.testTag())).assert(isSelected)
    }
}

/**
 * AC-1.2 — a barra/rail acompanha a largura da janela.
 *
 * Fica em classe própria porque cada teste precisa do seu
 * `@Config(qualifiers = ...)`, e o Robolectric lê a anotação da
 * **classe**: não dá para trocar o qualifiers dentro de um teste.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w599dp-h800dp")
class HomeNavigationSuiteBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `a 599dp o destino fica no rodapé — barra, não rail`() {
        composeRule.setContent {
            BrainOutTheme {
                Surface {
                    HomeNavigationShell(
                        currentTab = HomeTab.Projects,
                        onSelectTab = {},
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .testTag(BODY_TAG),
                        )
                    }
                }
            }
        }

        val bounds =
            composeRule
                .onNodeWithTag(HomeTab.Projects.testTag())
                .getBoundsInRoot()

        // A distinção é a **vertical**, não a horizontal: tanto a
        // `NavigationBar` quanto a `NavigationRail` começam em
        // x = 0 — a barra porque ocupa a largura toda, o rail porque
        // fica encostado na lateral. O que muda é o y: a barra fica no
        // fundo (item a partir da metade da altura), o rail fica no
        // topo. Medido, não deduzido: a janela tem 800dp.
        assertThat(bounds.top.value).isGreaterThan(400f)
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w600dp-h800dp")
class HomeNavigationSuiteRailTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `a 600dp o destino fica na lateral — rail, não barra`() {
        composeRule.setContent {
            BrainOutTheme {
                Surface {
                    HomeNavigationShell(
                        currentTab = HomeTab.Projects,
                        onSelectTab = {},
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .testTag(BODY_TAG),
                        )
                    }
                }
            }
        }

        val bounds =
            composeRule
                .onNodeWithTag(HomeTab.Projects.testTag())
                .getBoundsInRoot()

        // Mesma métrica do teste da barra, espelhada: o rail empilha
        // os itens a partir do topo, então o primeiro fica acima da
        // metade da altura. A barra, no fundo, passaria de 400dp. (O x
        // não discrimina — os dois começam em 0.)
        assertThat(bounds.top.value).isLessThan(200f)
    }
}
