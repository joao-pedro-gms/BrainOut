// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Testes de UI da [DashboardScreen] — E2.7 do ROADMAP.
//
// Rodam em JVM via Robolectric + createComposeRule. Verificam os
// valores iniciais do estado: título do painel, contadores de
// projetos, barras do gráfico de prioridade (rótulos numéricos) e
// taxas de conclusão, usando fakes do estado (sem Hilt).

package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoErrorBannerTestTags
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoWindowProvider

/**
 * Verifica a renderização inicial da [DashboardContent] (E2.7):
 * título, cartão de estados de projeto, barras de prioridade e cartões
 * de taxa de conclusão exibem os valores recebidos no estado.
 */
@RunWith(AndroidJUnit4::class)
// pt-rBR garante resolução das strings do `values/` padrão (pt): com o
// locale default do Robolectric (en), `values-en` venceria a resolução.
@Config(sdk = [34], qualifiers = "pt-rBR")
class DashboardScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `valores iniciais sao renderizados nos cartoes e no grafico`() {
        val state =
            DashboardUiState(
                activeProjects = 3,
                completedProjects = 1,
                priorityCounts = listOf(2, 4, 3, 1, 0),
                totalTasks = 10,
                doneTasks = 4,
                weeklyCompletionPercent = 20,
                overallCompletionPercent = 40,
                isLoading = false,
            )

        composeRule.setContent {
            BrainOutTheme {
                Surface {
                    DashboardContent(state = state)
                }
            }
        }

        // Contadores de projetos, escopados por test tag (o número
        // "3" também aparece como rótulo de barra do gráfico).
        //
        // DEF-22: o título "Painel" saiu de `DashboardContent` e foi
        // para a `TasksTopBar` do `Scaffold` — o mesmo contrato de
        // LoginScreen/SettingsScreen, onde o corpo não repete o
        // título da barra. A asserção do título mora agora em
        // `DashboardScreenScaffoldTest`, que monta o `Scaffold`.
        composeRule.onNodeWithTag(DashboardTestTags.PROJECT_STATE_CARD).assertExists()
        composeRule
            .onNodeWithTag(DashboardTestTags.PROJECT_ACTIVE_COUNT, useUnmergedTree = true)
            .onChildren()
            .filter(hasTextExactly("3"))
            .assertCountEquals(1)
        composeRule
            .onNodeWithTag(DashboardTestTags.PROJECT_COMPLETED_COUNT, useUnmergedTree = true)
            .onChildren()
            .filter(hasTextExactly("1"))
            .assertCountEquals(1)

        // Gráfico de prioridade: 5 barras (níveis 0..4), cada uma com
        // test tag próprio.
        composeRule.onNodeWithTag(DashboardTestTags.PRIORITY_CHART).assertExists()
        (0..4).forEach { level ->
            composeRule.onNodeWithTag(DashboardTestTags.priorityBar(level)).assertExists()
        }

        // Taxas de conclusão.
        composeRule.onNodeWithTag(DashboardTestTags.WEEKLY_RATE_CARD).assertExists()
        composeRule.onNodeWithTag(DashboardTestTags.OVERALL_RATE_CARD).assertExists()
        composeRule.onNodeWithText("20%").assertExists()
        composeRule.onNodeWithText("40%").assertExists()
    }

    @Test
    fun `taxas ficam indisponiveis quando ainda nao existem tarefas`() {
        composeRule.setContent {
            BrainOutTheme {
                Surface {
                    DashboardContent(
                        state =
                            DashboardUiState(
                                activeProjects = 1,
                                totalTasks = 0,
                                weeklyCompletionPercent = 0,
                                overallCompletionPercent = 0,
                                isLoading = false,
                            ),
                    )
                }
            }
        }

        composeRule.onAllNodesWithText("Indisponível").assertCountEquals(2)
        composeRule.onNodeWithText("0%").assertDoesNotExist()
    }

    @Test
    fun `estado vazio exibe empty state quando nao ha dados`() {
        composeRule.setContent {
            MaterialTheme {
                DashboardContent(
                    state = DashboardUiState(isLoading = false),
                )
            }
        }

        composeRule.onNodeWithTag(DashboardTestTags.EMPTY).assertExists()
        composeRule.onNodeWithText("Nada para mostrar ainda").assertExists()
    }

    @Test
    fun `estado de erro exibe banner com botoes de retry e dispensar`() {
        composeRule.setContent {
            MaterialTheme {
                DashboardContent(
                    state =
                        DashboardUiState(
                            isLoading = false,
                            errorMessage = TasksErrorType.LoadFailed,
                        ),
                    onRetry = {},
                    onDismissError = {},
                )
            }
        }

        composeRule.onNodeWithTag(NeoErrorBannerTestTags.BANNER).assertExists()
        composeRule.onNodeWithTag(NeoErrorBannerTestTags.RETRY).assertExists()
        composeRule.onNodeWithTag(NeoErrorBannerTestTags.DISMISS).assertExists()
        composeRule.onNodeWithText("Não foi possível carregar o painel").assertExists()
    }

    @Test
    fun `grafico com lista de contagens vazia nao quebra`() {
        composeRule.setContent {
            MaterialTheme {
                PriorityChart(counts = emptyList())
            }
        }

        composeRule.onNodeWithTag(DashboardTestTags.PRIORITY_CHART).assertExists()
    }

    @Test
    fun `fontScale dois mantem textos do estado vazio dentro do cartao`() {
        composeRule.setContent {
            BrainOutTheme {
                val currentDensity = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(currentDensity.density, 2f)) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        DashboardContent(state = DashboardUiState(isLoading = false))
                    }
                }
            }
        }

        val card = composeRule.onNodeWithTag(DashboardTestTags.EMPTY).fetchSemanticsNode().boundsInRoot
        val title = composeRule.onNodeWithText("Nada para mostrar ainda").fetchSemanticsNode().boundsInRoot
        val body =
            composeRule
                .onNodeWithText("Crie projetos e tarefas para ver as estatísticas aqui.")
                .fetchSemanticsNode()
                .boundsInRoot

        assertTrue("título deve permanecer dentro do cartão", title.top >= card.top && title.bottom <= card.bottom)
        assertTrue("corpo deve permanecer dentro do cartão", body.top >= card.top && body.bottom <= card.bottom)
    }

    @Test
    fun `fontScale dois mantem rotulo de carregamento dentro do container`() {
        composeRule.setContent {
            BrainOutTheme {
                val currentDensity = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(currentDensity.density, 2f)) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        DashboardLoading()
                    }
                }
            }
        }

        val container =
            composeRule.onNodeWithTag(DashboardTestTags.LOADING).fetchSemanticsNode().boundsInRoot
        val label = composeRule.onNodeWithText("Carregando painel").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "rótulo deve permanecer dentro do container",
            label.top >= container.top && label.bottom <= container.bottom,
        )
    }

    @Test
    fun `fontScale dois mantem legenda do grafico dentro do cartao`() {
        composeRule.setContent {
            BrainOutTheme {
                val currentDensity = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(currentDensity.density, 2f)) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        PriorityChart(counts = listOf(1, 2, 3, 4, 5))
                    }
                }
            }
        }

        val chart = composeRule.onNodeWithTag(DashboardTestTags.PRIORITY_CHART).fetchSemanticsNode().boundsInRoot
        listOf("Tarefas por prioridade", "Baixa", "Média", "Alta", "Urgente", "Crítica").forEach { text ->
            val bounds = composeRule.onNodeWithText(text).fetchSemanticsNode().boundsInRoot
            assertTrue(
                "$text deve permanecer dentro do cartão do gráfico",
                bounds.top >= chart.top && bounds.bottom <= chart.bottom,
            )
        }
    }

    @Test
    fun `grafico com mais contagens que niveis nao estoura os rotulos`() {
        // O gráfico tem cinco níveis de prioridade; a lista de
        // contagens vem do agrupamento do Room e não tem tamanho
        // garantido. Indexar rótulo e cor por posição estourava
        // `IndexOutOfBoundsException`.
        composeRule.setContent {
            MaterialTheme {
                PriorityChart(counts = listOf(1, 2, 3, 4, 5, 6))
            }
        }

        composeRule.onNodeWithTag(DashboardTestTags.PRIORITY_CHART).assertExists()
        composeRule.onNodeWithTag(DashboardTestTags.priorityBar(4)).assertExists()
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w393dp-h800dp")
class DashboardCompactWidthTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `cartoes de taxa empilham em largura compacta`() {
        composeRule.setContent {
            BrainOutTheme {
                NeoWindowProvider {
                    Surface {
                        DashboardContent(
                            state =
                                DashboardUiState(
                                    totalTasks = 2,
                                    weeklyCompletionPercent = 50,
                                    overallCompletionPercent = 50,
                                    isLoading = false,
                                ),
                        )
                    }
                }
            }
        }

        val weekly = composeRule.onNodeWithTag(DashboardTestTags.WEEKLY_RATE_CARD).fetchSemanticsNode().boundsInRoot
        val overall = composeRule.onNodeWithTag(DashboardTestTags.OVERALL_RATE_CARD).fetchSemanticsNode().boundsInRoot
        assertTrue("taxas devem ficar em linhas distintas na largura compacta", overall.top > weekly.top)
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w840dp-h800dp")
class DashboardWideWidthTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `cartoes de taxa permanecem lado a lado em largura expandida`() {
        composeRule.setContent {
            BrainOutTheme {
                NeoWindowProvider {
                    Surface {
                        DashboardContent(
                            state =
                                DashboardUiState(
                                    totalTasks = 2,
                                    weeklyCompletionPercent = 50,
                                    overallCompletionPercent = 50,
                                    isLoading = false,
                                ),
                        )
                    }
                }
            }
        }

        val weekly = composeRule.onNodeWithTag(DashboardTestTags.WEEKLY_RATE_CARD).fetchSemanticsNode().boundsInRoot
        val overall = composeRule.onNodeWithTag(DashboardTestTags.OVERALL_RATE_CARD).fetchSemanticsNode().boundsInRoot
        assertTrue("taxas devem compartilhar a linha em largura expandida", overall.top == weekly.top)
        assertTrue("cartão geral deve estar à direita do semanal", overall.left > weekly.left)
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "en-rUS")
class DashboardEnglishScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `taxas indisponiveis usam traducao em ingles`() {
        composeRule.setContent {
            BrainOutTheme {
                Surface {
                    DashboardContent(
                        state = DashboardUiState(activeProjects = 1, totalTasks = 0, isLoading = false),
                    )
                }
            }
        }

        composeRule.onAllNodesWithText("Unavailable").assertCountEquals(2)
        composeRule.onNodeWithText("0%").assertDoesNotExist()
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w840dp-h800dp")
class DashboardLargeFontScaleTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `cartoes de taxa empilham com fonte grande mesmo em tela larga`() {
        composeRule.setContent {
            BrainOutTheme {
                val currentDensity = LocalDensity.current
                val largeFontConfiguration = Configuration(LocalConfiguration.current).apply { fontScale = 1.5f }
                CompositionLocalProvider(
                    LocalConfiguration provides largeFontConfiguration,
                    LocalDensity provides Density(currentDensity.density, 1.5f),
                ) {
                    NeoWindowProvider {
                        Surface {
                            DashboardContent(
                                state =
                                    DashboardUiState(
                                        totalTasks = 2,
                                        weeklyCompletionPercent = 50,
                                        overallCompletionPercent = 50,
                                        isLoading = false,
                                    ),
                            )
                        }
                    }
                }
            }
        }

        val weekly = composeRule.onNodeWithTag(DashboardTestTags.WEEKLY_RATE_CARD).fetchSemanticsNode().boundsInRoot
        val overall = composeRule.onNodeWithTag(DashboardTestTags.OVERALL_RATE_CARD).fetchSemanticsNode().boundsInRoot
        assertTrue("fonte grande deve empilhar as taxas", overall.top > weekly.top)
    }
}
