// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.components

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme

/**
 * O campo de texto da identidade Neo.
 *
 * A receita estava escrita quatro vezes (nome e e-mail do cadastro,
 * e-mail e senha do login, senha do backup) e nenhuma delas tinha
 * teste próprio. O que se verifica aqui é o contrato do componente:
 * rótulo, texto de apoio no estado de erro e digitação.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "pt-rBR")
class NeoTextFieldTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `o rotulo e o texto de apoio aparecem`() {
        compose.setContent {
            BrainOutTheme {
                NeoTextField(
                    value = "ana@example.com",
                    onValueChange = {},
                    label = "E-mail",
                    supportingText = "E-mail inválido",
                    isError = true,
                )
            }
        }

        compose.onNodeWithText("E-mail").assertExists()
        compose.onNodeWithText("E-mail inválido").assertExists()
    }

    @Test
    fun `o placeholder aparece quando o campo esta vazio e focado`() {
        compose.setContent {
            BrainOutTheme {
                NeoTextField(
                    value = "",
                    onValueChange = {},
                    label = "E-mail",
                    placeholder = "voce@exemplo.com",
                )
            }
        }

        // O Material3 só revela o placeholder com o campo em foco (sem
        // foco, o rotulo ocupa o centro) - entao o teste foca primeiro.
        compose.onNode(hasSetTextAction()).performClick()
        compose.onNodeWithText("voce@exemplo.com").assertExists()
    }

    @Test
    fun `o campo aceita digitacao`() {
        var typed = ""
        compose.setContent {
            BrainOutTheme {
                NeoTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    label = "E-mail",
                )
            }
        }

        compose.onNodeWithText("E-mail").assertIsEnabled().performTextInput("a")
        compose.runOnIdle { assertThat(typed).isEqualTo("a") }
    }
}
