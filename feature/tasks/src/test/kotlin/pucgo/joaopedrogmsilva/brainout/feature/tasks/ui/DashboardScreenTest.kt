// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Testes de UI da [DashboardScreen] — E2.7 do ROADMAP.
//
// Rodam em JVM via Robolectric + createComposeRule. Verificam os
// valores iniciais do estado: título do painel, contadores de
// projetos, barras do gráfico de prioridade (rótulos numéricos) e
// taxas de conclusão, usando fakes do estado (sem Hilt).

package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme

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
        val state = DashboardUiState(
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

        // Título do painel.
        composeRule.onNodeWithText("Painel").assertIsDisplayed()

        // Contadores de projetos, escopados por test tag (o número
        // "3" também aparece como rótulo de barra do gráfico).
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
                    state = DashboardUiState(
                        isLoading = false,
                        errorMessage = DashboardViewModel.ERROR_LOAD_FAILED,
                    ),
                    onRetry = {},
                    onDismissError = {},
                )
            }
        }

        composeRule.onNodeWithTag(DashboardTestTags.ERROR_BANNER).assertExists()
        composeRule.onNodeWithTag(DashboardTestTags.ERROR_RETRY).assertExists()
        composeRule.onNodeWithTag(DashboardTestTags.ERROR_DISMISS).assertExists()
        composeRule.onNodeWithText("Não foi possível carregar o painel").assertExists()
    }
}
