// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Tokens de forma (E4.5).
 *
 * Definidos em `:core:ui/theme` como único ponto de verdade; features
 * consomem `MaterialTheme.shapes` em vez de `RoundedCornerShape`
 * literais. Os valores seguem o Material 3 default com leve
 * arredondamento, suficiente para destacar botões e cards sem
 * descaracterizar componentes planos (splash, telas cheias).
 */
val BrainOutShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
