// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.components

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme

/**
 * O botão de voltar da identidade Neo.
 *
 * O rótulo acessível é o ponto: ele é o que o TalkBack anuncia, e a
 * receita estava copiada em duas features — cada uma com a sua cópia da
 * string "Voltar".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "pt-rBR")
class NeoBackButtonTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `o botao anuncia o que ele faz`() {
        compose.setContent {
            BrainOutTheme { NeoBackButton(onClick = {}) }
        }

        compose.onNode(hasContentDescription("Voltar")).assertExists()
    }

    @Test
    fun `o botao e acionavel`() {
        var clicks = 0
        compose.setContent {
            BrainOutTheme { NeoBackButton(onClick = { clicks++ }) }
        }

        compose.onNode(hasContentDescription("Voltar")).assertHasClickAction().performClick()
        compose.runOnIdle { assertThat(clicks).isEqualTo(1) }
    }
}
