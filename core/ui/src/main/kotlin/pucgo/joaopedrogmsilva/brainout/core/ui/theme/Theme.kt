// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = BrainOutPrimary,
    onPrimary = BrainOutOnPrimary,
    primaryContainer = BrainOutPrimaryContainer,
    onPrimaryContainer = BrainOutOnPrimaryContainer,
    secondary = BrainOutSecondary,
    onSecondary = BrainOutOnSecondary,
    secondaryContainer = BrainOutSecondaryContainer,
    onSecondaryContainer = BrainOutOnSecondaryContainer,
    tertiary = BrainOutTertiary,
    onTertiary = BrainOutOnTertiary,
    tertiaryContainer = BrainOutTertiaryContainer,
    onTertiaryContainer = BrainOutOnTertiaryContainer,
    error = BrainOutError,
    onError = BrainOutOnError,
    errorContainer = BrainOutErrorContainer,
    onErrorContainer = BrainOutOnErrorContainer,
    background = BrainOutBackground,
    onBackground = BrainOutOnBackground,
    surface = BrainOutSurface,
    onSurface = BrainOutOnSurface,
    surfaceVariant = BrainOutSurfaceVariant,
    onSurfaceVariant = BrainOutOnSurfaceVariant,
    outline = BrainOutOutline
)

private val DarkColors = darkColorScheme(
    primary = BrainOutPrimaryContainer,
    onPrimary = BrainOutOnPrimaryContainer,
    secondary = BrainOutSecondaryContainer,
    onSecondary = BrainOutOnSecondaryContainer,
    tertiary = BrainOutTertiaryContainer,
    onTertiary = BrainOutOnTertiaryContainer
)

/**
 * Tema Compose raiz do BrainOut.
 *
 * Por padrão NÃO habilitamos `dynamicColor` (Material You) para garantir
 * identidade visual estável entre devices — a especificação E1.3 pede
 * tokens consistentes em PRs.
 *
 * Para ligar dynamic color em builds internos, basta passar
 * `dynamicColor = true`.
 */
@Composable
fun BrainOutTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BrainOutTypography,
        content = content
    )
}
