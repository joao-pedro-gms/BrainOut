// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// DEF-26 — `TasksErrorBanner` recebia `message` e descartava
// (`@Suppress("UNUSED_PARAMETER")`): o texto era sempre "não foi
// possível carregar suas tarefas", então uma falha de escrita aparecia
// como se a leitura do Room tivesse quebrado.
//
// O erro agora chega **tipado** (`TasksErrorType`): a tela de Tarefas
// só produz falha de carga, e o banner mostra o texto pt/en dela.

package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

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
 * O banner é testado direto: o defeito era do texto renderizado, não da
 * tela inteira, e `TasksErrorBanner` não exige ViewModel de Hilt.
 */
@RunWith(AndroidJUnit4::class)
// pt-rBR: resolve as strings do `values/` padrão. Com o locale default
// do Robolectric (en), `values-en` venceria a resolução.
@Config(sdk = [34], qualifiers = "pt-rBR")
class TasksErrorBannerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `falha de leitura mostra a mensagem localizada`() {
        composeRule.setContent {
            BrainOutTheme {
                TasksErrorBanner(
                    message = TasksErrorType.LoadFailed,
                    onRetry = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Não foi possível carregar suas tarefas").assertIsDisplayed()
    }
}
