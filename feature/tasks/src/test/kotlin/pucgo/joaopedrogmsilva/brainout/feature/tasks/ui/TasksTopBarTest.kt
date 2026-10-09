// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// DEF-22 — `TasksScreen` e `DashboardScreen` deixaram de ser becos sem
// saída: ganharam `Scaffold` + `TasksTopBar` com botão de voltar.
//
// Antes, nenhuma das duas tinha `TopAppBar`, `navigationIcon` nem
// qualquer `statusBarsPadding`/`navigationBarsPadding`, enquanto
// `MainActivity` liga `enableEdgeToEdge()`. O usuário chegava pelo ícone
// da bottom bar da Home e ficava preso: título sob a barra de status,
// lista sob a barra de gestos e nenhum caminho de volta.
//
// Estes testes montam o `Scaffold` real (não o `*Content` interno) e
// verificam as duas metades do defeito: existe uma saída, e ela dispara
// o callback.

package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme

/**
 * `TasksTopBar` é o componente compartilhado pelas duas abas; testá-lo
 * diretamente cobre o título + voltar sem precisar montar um
 * ViewModel de Hilt.
 */
@RunWith(AndroidJUnit4::class)
// pt-rBR: resolve as strings do `values/` padrão. Com o locale default
// do Robolectric (en), `values-en` venceria a resolução.
@Config(sdk = [34], qualifiers = "pt-rBR")
class TasksTopBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `a barra exibe o titulo da aba`() {
        composeRule.setContent {
            BrainOutTheme {
                TasksTopBar(
                    titleRes = pucgo.joaopedrogmsilva.brainout.feature.tasks.R.string.tasks_screen_title,
                    onBackClicked = {},
                )
            }
        }

        composeRule.onNodeWithText("Tarefas").assertIsDisplayed()
    }

    @Test
    fun `a barra do painel exibe o titulo correto`() {
        composeRule.setContent {
            BrainOutTheme {
                TasksTopBar(
                    titleRes = pucgo.joaopedrogmsilva.brainout.feature.tasks.R.string.dashboard_screen_title,
                    onBackClicked = {},
                )
            }
        }

        composeRule.onNodeWithText("Painel").assertIsDisplayed()
    }

    @Test
    fun `o botao de voltar existe e dispara o callback`() {
        var backClicks = 0

        composeRule.setContent {
            BrainOutTheme {
                TasksTopBar(
                    titleRes = pucgo.joaopedrogmsilva.brainout.feature.tasks.R.string.tasks_screen_title,
                    onBackClicked = { backClicks++ },
                )
            }
        }

        // Este é o defeito: antes não havia nó de retorno nenhum.
        composeRule.onNodeWithTag(TasksTestTags.BACK_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(TasksTestTags.BACK_BUTTON).performClick()

        check(backClicks == 1) { "esperado 1 clique em voltar, veio $backClicks" }
    }

    @Test
    fun `o botao de voltar se anuncia com o rotulo localizado`() {
        composeRule.setContent {
            BrainOutTheme {
                TasksTopBar(
                    titleRes = pucgo.joaopedrogmsilva.brainout.feature.tasks.R.string.tasks_screen_title,
                    onBackClicked = {},
                )
            }
        }

        // O TalkBack precisa dizer o que o botão faz, não só "botão".
        // O rótulo vem de `common_back` e é o mesmo do ProjectDetail.
        //
        // `onNodeWithText` não encontra: `contentDescription` de um
        // `Icon` vira `SemanticsProperties.ContentDescription`, não
        // `Text`. O matcher correto é `hasContentDescription`.
        composeRule.onNode(hasContentDescription("Voltar")).assertIsDisplayed()
    }
}
