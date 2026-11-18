// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.settings.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoLayout
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoWindowProvider

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w1280dp-h800dp")
class SettingsTabletLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `conteudo de configuracoes fica limitado e centralizado em tablet`() {
        composeRule.setContent {
            NeoWindowProvider {
                BrainOutTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        SettingsScreen(onSignOut = {})
                    }
                }
            }
        }

        val row =
            composeRule
                .onNodeWithTag("settings_option_signout")
                .performScrollTo()
                .fetchSemanticsNode()
        val maxWidthPx = with(composeRule.density) { NeoLayout.contentMaxWidth.toPx() }
        val rootWidthPx =
            composeRule
                .onRoot()
                .fetchSemanticsNode()
                .size.width
                .toFloat()

        assert(row.size.width <= maxWidthPx) {
            "conteúdo de configurações excedeu NeoLayout.contentMaxWidth: ${row.size.width}px > ${maxWidthPx}px"
        }
        assert(row.boundsInRoot.left > 0f && row.boundsInRoot.right < rootWidthPx) {
            "conteúdo de configurações não ficou centralizado na janela larga"
        }
    }

    @Test
    fun `linhas de configuracoes continuam legiveis com fonte ampliada`() {
        composeRule.setContent {
            val density = LocalDensity.current
            val largeFontDensity = remember(density) { Density(density.density, fontScale = 1.5f) }
            CompositionLocalProvider(LocalDensity provides largeFontDensity) {
                NeoWindowProvider {
                    BrainOutTheme {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            SettingsScreen(onSignOut = {})
                        }
                    }
                }
            }
        }

        composeRule
            .onNodeWithTag("settings_option_signout")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
