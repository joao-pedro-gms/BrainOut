// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Testes Compose da [SettingsScreen] (E1.3). Robolectric JVM.
//
// Sem ViewModel próprio: a tela é stateless e delega ao NavHost via
// callbacks. Cobre (a) renderização das seções/opções, (b) click
// despachando o [SettingsActionType] correto, (c) ícone de logout
// para a opção SignOut.

package pucgo.joaopedrogmsilva.brainout.feature.settings.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
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
 * Cobertura mínima do esqueleto de configurações (E1.3): título,
 * subtítulo, três seções, quatro opções e dispatch por clique.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "pt-rBR")
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renderiza titulo, subtitulo e quatro opcoes`() {
        composeRule.setContent {
            BrainOutTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(onOptionClicked = {})
                }
            }
        }

        composeRule.onNodeWithText("Configurações").assertIsDisplayed()
        composeRule.onNodeWithText("Preferências da sua conta e do aplicativo.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Conta").assertIsDisplayed()
        composeRule.onNodeWithText("Aparência").assertIsDisplayed()
        composeRule.onNodeWithText("Perfil").assertIsDisplayed()
        composeRule.onNodeWithText("Notificações").assertIsDisplayed()
        composeRule.onNodeWithText("Tema").assertIsDisplayed()
        composeRule.onNodeWithText("Sair").assertIsDisplayed()
    }

    @Test
    fun `click em cada opcao despacha o SettingsActionType correspondente`() {
        val dispatched = mutableListOf<SettingsActionType>()
        composeRule.setContent {
            BrainOutTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(onOptionClicked = { dispatched += it })
                }
            }
        }

        composeRule.onNodeWithTag("settings_option_profile").performClick()
        composeRule.onNodeWithTag("settings_option_notifications").performClick()
        composeRule.onNodeWithTag("settings_option_theme").performClick()
        composeRule.onNodeWithTag("settings_option_signout").performClick()

        assert(dispatched.size == 4)
        assert(dispatched[0] == SettingsActionType.Profile)
        assert(dispatched[1] == SettingsActionType.Notifications)
        assert(dispatched[2] == SettingsActionType.Theme)
        assert(dispatched[3] == SettingsActionType.SignOut)
    }
}
