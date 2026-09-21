// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta principal do BrainOut.
 *
 * Os tons são derivados das paletas padrão do Material 3 (cor semente
 * roxa) para garantir identidade visual consistente enquanto o design
 * system próprio não é finalizado.
 *
 * Tokens centralizados em `:core:ui/theme` (E4.5): as paletas claras e
 * escuras ficam neste único ponto de verdade. Features consomem apenas
 * `MaterialTheme.colorScheme` / `BrainOutShapes` / `BrainOutTypography`;
 * nenhuma cor deve aparecer em código de feature.
 */

// --- Light tokens ---
internal val BrainOutPrimary = Color(0xFF6750A4)
internal val BrainOutOnPrimary = Color(0xFFFFFFFF)
internal val BrainOutPrimaryContainer = Color(0xFFEADDFF)
internal val BrainOutOnPrimaryContainer = Color(0xFF21005D)

internal val BrainOutSecondary = Color(0xFF625B71)
internal val BrainOutOnSecondary = Color(0xFFFFFFFF)
internal val BrainOutSecondaryContainer = Color(0xFFE8DEF8)
internal val BrainOutOnSecondaryContainer = Color(0xFF1D192B)

internal val BrainOutTertiary = Color(0xFF7D5260)
internal val BrainOutOnTertiary = Color(0xFFFFFFFF)
internal val BrainOutTertiaryContainer = Color(0xFFFFD8E4)
internal val BrainOutOnTertiaryContainer = Color(0xFF31111D)

internal val BrainOutError = Color(0xFFB3261E)
internal val BrainOutOnError = Color(0xFFFFFFFF)
internal val BrainOutErrorContainer = Color(0xFFF9DEDC)
internal val BrainOutOnErrorContainer = Color(0xFF410E0B)

internal val BrainOutBackground = Color(0xFFFFFBFE)
internal val BrainOutOnBackground = Color(0xFF1C1B1F)
internal val BrainOutSurface = Color(0xFFFFFBFE)
internal val BrainOutOnSurface = Color(0xFF1C1B1F)

internal val BrainOutSurfaceVariant = Color(0xFFE7E0EC)
internal val BrainOutOnSurfaceVariant = Color(0xFF49454F)
internal val BrainOutOutline = Color(0xFF79747E)

// Tokens do tema escuro. Derivados da paleta Material 3 (mesma cor
// semente roxa) seguindo o padrão do M3 Color Builder. Garantimos
// contraste AA (>= 4.5:1 texto, >= 3.0:1 gráfico) entre cada par
// `on*` / superfície — coberto por `ContrastRatioTest` (marco E4.4).
internal val BrainOutDarkPrimary = Color(0xFFD0BCFF)
internal val BrainOutDarkOnPrimary = Color(0xFF381E72)
internal val BrainOutDarkPrimaryContainer = Color(0xFF4F378B)
internal val BrainOutDarkOnPrimaryContainer = Color(0xFFEADDFF)

internal val BrainOutDarkSecondary = Color(0xFFCCC2DC)
internal val BrainOutDarkOnSecondary = Color(0xFF332D41)
internal val BrainOutDarkSecondaryContainer = Color(0xFF4A4458)
internal val BrainOutDarkOnSecondaryContainer = Color(0xFFE8DEF8)

internal val BrainOutDarkTertiary = Color(0xFFEFB8C8)
internal val BrainOutDarkOnTertiary = Color(0xFF492532)
internal val BrainOutDarkTertiaryContainer = Color(0xFF633B48)
internal val BrainOutDarkOnTertiaryContainer = Color(0xFFFFD8E4)

internal val BrainOutDarkError = Color(0xFFF2B8B5)
internal val BrainOutDarkOnError = Color(0xFF601410)
internal val BrainOutDarkErrorContainer = Color(0xFF8C1D18)
internal val BrainOutDarkOnErrorContainer = Color(0xFFF9DEDC)

internal val BrainOutDarkBackground = Color(0xFF1C1B1F)
internal val BrainOutDarkOnBackground = Color(0xFFE6E1E5)
internal val BrainOutDarkSurface = Color(0xFF1C1B1F)
internal val BrainOutDarkOnSurface = Color(0xFFE6E1E5)

internal val BrainOutDarkSurfaceVariant = Color(0xFF49454F)
internal val BrainOutDarkOnSurfaceVariant = Color(0xFFCAC4D0)
internal val BrainOutDarkOutline = Color(0xFF938F99)

