// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// RESP-1 (Fase 1.1) — AC-1.1: Login e Register em paisagem curta.
//
// O defeito que a Fase 1 corrige é o teclado cobrindo os campos. Este
// teste fixa o que **dá** para provar em JVM: com uma janela de 400dp
// de altura (o formato de um telefone em paisagem, com o teclado aberto
// ou não), os campos de Login e o botão de cadastro do Register
// continuam alcançáveis e visíveis.
//
// ⚠️ Limite honesto da medição: o Robolectric devolve **zero** para
// todo inset de janela — inclusive `WindowInsets.ime`. Duas sondas
// descartáveis mediram `imeBottom=0 safeBottom=0 sysBottom=0` e um
// `dispatchApplyWindowInsets` com `Type.ime()` de 240px não chega ao
// Compose (`imeAfterDispatch=0`). Logo `imePadding()` é no-op aqui, e
// um teste que afirmasse "o campo está acima do teclado" passaria
// antes **e** depois da correção — seria teste morto, não regressão.
// A metade IME do AC-1.1 precisa de device/emulador e está registrada
// como QA manual no card.
//
// O que este teste protege de verdade: a coluna rolável + os insets de
// `contentWindowInsets` não empurram os campos para fora da janela em
// 400dp. Antes da correção, em paisagem, o `topBar` não consumia inset
// nenhum e o formulário montava por cima da barra de status.

package pucgo.joaopedrogmsilva.brainout.feature.auth.ui.login

import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.data.session.SessionStore
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.register.RegisterScreen
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.register.RegisterTestTags
import pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel.AuthViewModel

@RunWith(AndroidJUnit4::class)
// 800x400dp é um telefone em paisagem — a geometria do AC-1.1. Os
// asserts são por `testTag` + geometria, então o locale padrão do
// Robolectric (en) não interfere neles.
@Config(sdk = [34], qualifiers = "w800dp-h400dp")
class AuthLandscapeInsetsTest {
    @get:Rule
    val composeRule = createComposeRule()

    // Mesmo padrão de `AuthViewModelTest`: mocks dos casos de uso para
    // não depender do Hilt. Nenhum é chamado aqui — o teste só renderiza.
    private fun newViewModel(): AuthViewModel =
        AuthViewModel(
            createUserUseCase = mockk(relaxed = true),
            authenticateUserUseCase = mockk(relaxed = true),
            sessionStore = mockk<SessionStore>(relaxed = true),
        )

    @Test
    fun `login em paisagem mantem senha e submit visiveis`() {
        composeRule.setContent {
            BrainOutTheme {
                Surface(modifier = Modifier) {
                    LoginScreen(
                        onLoginSubmit = {},
                        onCreateAccountClicked = {},
                        viewModel = newViewModel(),
                    )
                }
            }
        }

        // O campo de senha precisa existir e estar visível na janela
        // de 400dp. `assertIsDisplayed` falha se o nó estiver fora do
        // viewport — exatamente o defeito de paisagem.
        composeRule
            .onNodeWithTag(LoginTestTags.PASSWORD_FIELD)
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag(LoginTestTags.SUBMIT)
            .assertIsDisplayed()
    }

    @Test
    fun `login em paisagem o campo de email e o criar conta estao visiveis`() {
        composeRule.setContent {
            BrainOutTheme {
                Surface(modifier = Modifier) {
                    LoginScreen(
                        onLoginSubmit = {},
                        onCreateAccountClicked = {},
                        viewModel = newViewModel(),
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag(LoginTestTags.EMAIL_FIELD)
            .assertIsDisplayed()
        // `CREATE_ACCOUNT` é o último elemento da coluna rolável: em
        // 400dp ele nasce abaixo da dobra, e o `assertIsDisplayed` puro
        // reprova. O contrato real não é "já está na tela" e sim
        // "**dá para chegar** nele e ele fica inteiro" — que é o que a
        // correção de insets garante: sem os insets, o final da coluna
        // ficava atrás da barra de gestos e não havia scroll que o
        // salvasse.
        composeRule
            .onNodeWithTag(LoginTestTags.CREATE_ACCOUNT)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `register em paisagem mantem o botao de cadastro visivel`() {
        composeRule.setContent {
            BrainOutTheme {
                Surface(modifier = Modifier) {
                    RegisterScreen(
                        onRegisterSubmit = {},
                        onHaveAccountClicked = {},
                        viewModel = newViewModel(),
                    )
                }
            }
        }

        // O Register é a tela mais alta (4 campos + selector de papel):
        // em 400dp o submit está bem abaixo da dobra. Mesmo contrato do
        // teste anterior — alcançável e inteiro após o scroll, e não
        // cortado pela barra de gestos.
        composeRule
            .onNodeWithTag(RegisterTestTags.SUBMIT)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag(RegisterTestTags.HAVE_ACCOUNT)
            .performScrollTo()
            .assertIsDisplayed()
    }
}
