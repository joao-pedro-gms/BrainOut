// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NeoMotionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `confirmacao nao anima antes de mudar estado e termina em 240ms`() {
        val target = mutableStateOf(Color.White)
        var rendered = Color.Unspecified
        compose.setContent {
            val color = neoConfirmedColor(target.value)
            androidx.compose.material3.Text(color.toString())
            androidx.compose.runtime.SideEffect { rendered = color }
        }
        compose.runOnIdle { assertThat(rendered).isEqualTo(Color.White) }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { target.value = Color.Black }
        compose.waitForIdle()
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(120)
        compose.runOnIdle { assertThat(rendered).isNotEqualTo(Color.Black) }
        compose.mainClock.advanceTimeBy(160)
        compose.runOnIdle { assertThat(rendered).isEqualTo(Color.Black) }
    }
}
