// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// DEF-26 — `ProjectDetailErrorBanner` recebia `message` e descartava
// (`@Suppress("UNUSED_PARAMETER")`), exibindo sempre "não foi possível
// carregar as tarefas do projeto". Um erro de escrita (título vazio,
// RN02 prioridade bloqueada) chegava ao usuário disfarçado de falha de
// leitura do Room.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme

/**
 * Testa o banner direto (mesmo critério de `TasksErrorBannerTest`): o
 * defeito era do texto renderizado, não da tela inteira.
 */
@RunWith(AndroidJUnit4::class)
// pt-rBR: resolve as strings do `values/` padrão. Com o locale default
// do Robolectric (en), `values-en` venceria a resolução.
@Config(sdk = [34], qualifiers = "pt-rBR")
class ProjectDetailErrorBannerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `falha de leitura mostra a mensagem localizada`() {
        composeRule.setContent {
            BrainOutTheme {
                ProjectDetailErrorBanner(
                    error = ProjectDetailError.LoadFailed,
                    onRetry = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Não foi possível carregar as tarefas do projeto").assertIsDisplayed()
    }

    @Test
    fun `falha de escrita mostra a mensagem original`() {
        composeRule.setContent {
            BrainOutTheme {
                ProjectDetailErrorBanner(
                    error = ProjectDetailError.EmptyTitle,
                    onRetry = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Título da tarefa não pode ser vazio").assertIsDisplayed()
    }
}
