// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// DEF-27 — regressão dos insets de IME nos formulários de login/cadastro.
//
// Com `enableEdgeToEdge()` a janela NÃO é redimensionada quando o teclado
// abre: quem precisa ceder espaço é a própria tela. Estes testes publicam um
// `WindowInsets.Type.ime()` de verdade na `ComposeView` (robolectric +
// `dispatchApplyWindowInsets`) e verificam a propriedade geométrica que o
// bug quebrava — a área rolável do formulário precisa terminar ACIMA da
// linha do teclado, senão o campo de senha e o botão de envio ficam atrás
// dele. Rodam em paisagem (`qualifiers = "land"`), o pior caso da issue.

package pucgo.joaopedrogmsilva.brainout.feature.auth.ui

import android.view.ViewGroup
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.login.LoginBody
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.login.LoginCallbacks
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.login.LoginTestTags
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.register.RegisterBody
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.register.RegisterCallbacks
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.register.RegisterTestTags
import pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel.AuthUiState
import android.graphics.Insets as AndroidInsets

/** Altura do teclado simulado, como fração da altura da janela. */
private const val IME_FRACTION = 0.4f

/** Callbacks inertes: o teste valida insets e rolagem, não submissão. */
private val noopLoginCallbacks =
    LoginCallbacks(
        onEmailChange = {},
        onPasswordChange = {},
        onSubmit = {},
        onCreateAccountClicked = {},
    )

private val noopRegisterCallbacks =
    RegisterCallbacks(
        onNameChange = {},
        onEmailChange = {},
        onPasswordChange = {},
        onPasswordConfirmationChange = {},
        onRoleChange = {},
        onSubmit = {},
        onHaveAccountClicked = {},
    )

/**
 * DEF-27: com o teclado visível em paisagem, a área rolável do formulário de
 * login termina acima da linha do teclado e o botão de envio, alcançável por
 * rolagem, fica inteiro na faixa livre.
 */
@RunWith(AndroidJUnit4::class)
// `pt-rBR` resolve as strings do `values/` padrão (com o locale default do
// Robolectric, en, `values-en` venceria). `land` é o pior caso da issue:
// janela baixa, formulário maior que a área útil.
@Config(sdk = [34], qualifiers = "pt-rBR-land")
class AuthImeInsetsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `login com teclado visivel mantem o botao de envio acima do ime`() {
        composeRule.setContent {
            BrainOutTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LoginBody(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        uiState = AuthUiState(),
                        callbacks = noopLoginCallbacks,
                    )
                }
            }
        }

        val keyboardTop = showKeyboard()

        composeRule.onNodeWithTag(LoginTestTags.SCROLL).assertBottomAbove(keyboardTop, LoginTestTags.SCROLL)
        composeRule
            .onNodeWithTag(LoginTestTags.SCROLL)
            .performScrollToNode(hasTestTag(LoginTestTags.SUBMIT))
        composeRule.onNodeWithTag(LoginTestTags.SUBMIT).assertIsDisplayed()
        composeRule.onNodeWithTag(LoginTestTags.SUBMIT).assertBottomAbove(keyboardTop, LoginTestTags.SUBMIT)
    }

    @Test
    fun `cadastro com teclado visivel mantem o botao de envio acima do ime`() {
        composeRule.setContent {
            BrainOutTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RegisterBody(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        uiState = AuthUiState(),
                        callbacks = noopRegisterCallbacks,
                    )
                }
            }
        }

        val keyboardTop = showKeyboard()

        composeRule.onNodeWithTag(RegisterTestTags.SCROLL).assertBottomAbove(keyboardTop, RegisterTestTags.SCROLL)
        composeRule
            .onNodeWithTag(RegisterTestTags.SCROLL)
            .performScrollToNode(hasTestTag(RegisterTestTags.SUBMIT))
        composeRule.onNodeWithTag(RegisterTestTags.SUBMIT).assertIsDisplayed()
        composeRule.onNodeWithTag(RegisterTestTags.SUBMIT).assertBottomAbove(keyboardTop, RegisterTestTags.SUBMIT)
    }

    /**
     * Abre o teclado: publica `WindowInsets.Type.ime()` na `ComposeView` e
     * devolve a coordenada Y (px, espaço da raiz) da linha superior dele.
     *
     * Tem que ser na `ComposeView` — despachar no `decorView` não chega no
     * listener de insets do Compose e o teste passaria vazio.
     */
    private fun showKeyboard(): Float {
        val root = composeRule.onRoot()
        val rootNode = root.fetchSemanticsNode()
        val rootHeight = rootNode.size.height
        val imeHeight = (rootHeight * IME_FRACTION).toInt()
        composeView()
            .dispatchApplyWindowInsets(
                WindowInsets
                    .Builder()
                    .setInsets(WindowInsets.Type.ime(), AndroidInsets.of(0, 0, 0, imeHeight))
                    .setVisible(WindowInsets.Type.ime(), true)
                    .build(),
            )
        composeRule.waitForIdle()
        return (rootHeight - imeHeight).toFloat()
    }

    private fun composeView() =
        composeRule.activity.window.decorView
            .findViewById<ViewGroup>(android.R.id.content)
            .getChildAt(0)
}

/**
 * Falha se a borda inferior do nó descer até a linha do teclado ou além —
 * foi exatamente isso que escondia o botão de enviar (DEF-27).
 */
private fun SemanticsNodeInteraction.assertBottomAbove(
    limitY: Float,
    tag: String,
) {
    val bottom = fetchSemanticsNode().boundsInRoot.bottom
    check(bottom <= limitY) { "tag '$tag' invadiu o teclado: bottom=$bottom > teclado em $limitY" }
}
