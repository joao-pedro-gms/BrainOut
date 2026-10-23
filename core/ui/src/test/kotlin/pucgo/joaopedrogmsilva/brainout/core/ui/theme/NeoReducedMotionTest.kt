// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NeoReducedMotionTest {
    @get:Rule val compose =
        createComposeRule(
            effectContext =
                object : MotionDurationScale {
                    override val scaleFactor: Float = 0f
                },
        )

    @Test fun `escala zero mostra estado final sem aguardar duracao da animacao`() {
        val target = mutableStateOf(Color.White)
        var rendered = Color.Unspecified
        compose.setContent {
            val color = neoConfirmedColor(target.value)
            androidx.compose.material3.Text(color.toString())
            androidx.compose.runtime.SideEffect { rendered = color }
        }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { target.value = Color.Black }
        compose.waitForIdle()
        repeat(3) { compose.mainClock.advanceTimeByFrame() }
        compose.runOnIdle { assertThat(rendered).isEqualTo(Color.Black) }
    }
}
