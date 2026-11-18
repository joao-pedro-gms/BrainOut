// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.auth.ui.splash

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "pt-rBR-land")
class SplashResponsiveLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `cta da splash continua alcancavel em paisagem`() {
        composeRule.setContent {
            BrainOutTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SplashScreen(onStartClicked = {})
                }
            }
        }

        composeRule
            .onNodeWithTag(SplashScreenTestTags.CONTENT)
            .performScrollToNode(hasText("Começar"))
        composeRule.onNodeWithText("Começar").assertIsDisplayed()
    }
}
