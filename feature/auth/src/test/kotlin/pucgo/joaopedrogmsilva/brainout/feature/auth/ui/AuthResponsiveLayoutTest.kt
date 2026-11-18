// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.auth.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoLayout
import pucgo.joaopedrogmsilva.brainout.feature.auth.R
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.login.LoginBody
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.login.LoginCallbacks
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.login.LoginTestTags
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.register.RegisterBody
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.register.RegisterCallbacks
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.register.RegisterTestTags
import pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel.AuthUiState

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w800dp-h400dp")
class AuthResponsiveLayoutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `cadastro em paisagem baixa permite rolar ate o envio`() {
        composeRule.setContent {
            BrainOutTheme {
                AuthScreenScaffold(R.string.register_title) { innerPadding ->
                    RegisterBody(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        uiState = AuthUiState(),
                        callbacks = registerCallbacks(),
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag(RegisterTestTags.SCROLL)
            .performScrollToNode(hasTestTag(RegisterTestTags.SUBMIT))
        composeRule.onNodeWithTag(RegisterTestTags.SUBMIT).assertIsDisplayed()
    }

    @Test
    fun `formulario de login respeita a largura maxima de token`() {
        composeRule.setContent {
            BrainOutTheme {
                AuthScreenScaffold(R.string.login_title) { innerPadding ->
                    LoginBody(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        uiState = AuthUiState(),
                        callbacks = loginCallbacks(),
                    )
                }
            }
        }

        val field = composeRule.onNodeWithTag(LoginTestTags.USERNAME_FIELD).fetchSemanticsNode()
        val maxWidthPx = with(composeRule.density) { NeoLayout.formMaxWidth.toPx() }
        val rootWidthPx =
            composeRule
                .onRoot()
                .fetchSemanticsNode()
                .size.width
                .toFloat()
        assert(field.size.width <= maxWidthPx) {
            "campo de login excedeu NeoLayout.formMaxWidth: ${field.size.width}px > ${maxWidthPx}px"
        }
        assert(field.boundsInRoot.left > 0f && field.boundsInRoot.right < rootWidthPx) {
            "formulário de login não ficou centralizado na janela larga"
        }
    }

    @Test
    fun `cadastro com fonte ampliada continua permitindo rolar ate o envio`() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                BrainOutTheme {
                    AuthScreenScaffold(R.string.register_title) { innerPadding ->
                        RegisterBody(
                            modifier = Modifier.fillMaxSize().padding(innerPadding),
                            uiState = AuthUiState(),
                            callbacks = registerCallbacks(),
                        )
                    }
                }
            }
        }

        composeRule
            .onNodeWithTag(RegisterTestTags.SCROLL)
            .performScrollToNode(hasTestTag(RegisterTestTags.SUBMIT))
        composeRule.onNodeWithTag(RegisterTestTags.SUBMIT).assertIsDisplayed()
    }

    private fun loginCallbacks() =
        LoginCallbacks(
            onUsernameChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onCreateAccountClicked = {},
        )

    private fun registerCallbacks() =
        RegisterCallbacks(
            onUsernameChange = {},
            onPasswordChange = {},
            onPasswordConfirmationChange = {},
            onSubmit = {},
            onHaveAccountClicked = {},
        )
}
