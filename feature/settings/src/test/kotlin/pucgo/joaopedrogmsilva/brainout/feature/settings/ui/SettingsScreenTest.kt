// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Testes Compose da [SettingsScreen] (E1.3). Robolectric JVM.
//
// Sem ViewModel próprio: a tela é stateless e delega ao NavHost via
// callback. Cobre (a) renderização das seções/opções, (b) o toque em
// "Sair" disparando a ação, (c) as linhas sem destino respondendo
// "Em breve" em vez de absorver o toque em silêncio (DEF-44) e (d) o
// ícone de logout para a opção SignOut.

package pucgo.joaopedrogmsilva.brainout.feature.settings.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme

/**
 * Cobertura mínima do esqueleto de configurações (E1.3): título,
 * subtítulo, três seções, quatro opções e o efeito do clique.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "pt-rBR")
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renderiza titulo, subtitulo e as quatro opcoes`() {
        composeRule.setContent {
            BrainOutTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(onSignOut = {})
                }
            }
        }

        // O título está na `TopAppBar`, fora da coluna rolável: sem
        // `performScrollTo`. O subtítulo e as opções vivem dentro da
        // coluna com `verticalScroll` e ficam abaixo da dobra na janela
        // do Robolectric, então cada asserção rola até o nó.
        composeRule.onNodeWithText("Configurações").assertIsDisplayed()
        composeRule
            .onNodeWithText("Conta local e preferências do aplicativo.")
            .performScrollTo()
            .assertIsDisplayed()
        // Os títulos de seção são renderizados em caixa alta pela própria
        // tela (`stringResource(...).uppercase()`), então a asserção usa o
        // texto como ele aparece para o usuário.
        composeRule.onNodeWithText("CONTA").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("APARÊNCIA").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Perfil").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Notificações").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Tema").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Sair").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `clicar em Sair despacha a acao de encerrar sessao`() {
        var signOutCount = 0
        composeRule.setContent {
            BrainOutTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(onSignOut = { signOutCount++ })
                }
            }
        }

        composeRule.onNodeWithTag("settings_option_signout").performScrollTo().performClick()

        assert(signOutCount == 1) { "esperava 1 dispatch de Sair, veio $signOutCount" }
    }

    /**
     * DEF-44 — as linhas sem destino não podem absorver o toque em
     * silêncio: o usuário recebe "Em breve" no snackbar.
     */
    @Test
    fun `clicar em linha sem destino mostra Em breve e nao despacha acao`() {
        var signOutCount = 0
        composeRule.setContent {
            BrainOutTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(onSignOut = { signOutCount++ })
                }
            }
        }

        composeRule.onNodeWithTag("settings_option_profile").performScrollTo().performClick()
        composeRule.onNodeWithText("Em breve").assertIsDisplayed()

        composeRule.onNodeWithTag("settings_option_notifications").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings_option_theme").performScrollTo().performClick()

        // Nenhuma delas despachou a ação de sair.
        assert(signOutCount == 0) { "linha sem destino despachou ação ($signOutCount)" }
    }
}
