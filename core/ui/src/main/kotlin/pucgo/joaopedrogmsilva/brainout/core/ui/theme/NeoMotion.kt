// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

/** Animações finitas Compose respeitam MotionDurationScale, inclusive escala zero. */
object NeoMotion {
    const val press = 80
    const val release = 120
    const val selection = 160
    const val navigation = 220
    const val confirmation = 240
}

/** A face se move, mas o alvo de toque e o layout permanecem fixos. */
@Composable
fun Modifier.neoPressFeedback(source: MutableInteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val offset by animateFloatAsState(
        targetValue = if (pressed) 2f else 0f,
        animationSpec = tween(if (pressed) NeoMotion.press else NeoMotion.release),
        label = "pressFace",
    )
    return graphicsLayer {
        translationX = offset * density
        translationY = offset * density
    }
}

/** O alvo vem do estado persistido observado; a UI não antecipa sucesso. */
@Composable
fun neoConfirmedColor(target: Color): Color {
    val color by animateColorAsState(target, tween(NeoMotion.confirmation), label = "confirmedState")
    return color
}
