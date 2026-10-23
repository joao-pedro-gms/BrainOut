// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// A tela de abertura mostra a marca do app — o mesmo glifo que é o ícone
// do launcher (`brand/brainout-icon.svg`).
//
// O teste existe para prender o vínculo entre ícone e app: se a arte
// dentro do app divergir do ícone do launcher, a identidade se quebra em
// silêncio — e ninguém olha o ícone do launcher todo dia.

package pucgo.joaopedrogmsilva.brainout.feature.auth.ui.splash

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme

/**
 * Cobertura da tela de abertura: a marca do app precisa estar na tela, ao
 * lado do nome e do CTA.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "pt-rBR")
class SplashScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `a tela de abertura mostra a marca do app`() {
        composeRule.setContent {
            BrainOutTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SplashScreen(onStartClicked = {})
                }
            }
        }

        composeRule.onNodeWithTag(SplashScreenTestTags.LOGO).assertIsDisplayed()
    }
}
