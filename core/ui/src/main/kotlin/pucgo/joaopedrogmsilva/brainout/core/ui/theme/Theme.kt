// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

internal val BrainOutLightColors = lightColorScheme(

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

/**
 * `BrainOutDarkColors` espelha os 25 pares de `BrainOutLightColors` (marco E4.4 —
 * acessibilidade AA). A versão original da E1.3 cobria apenas 6
 * tokens, deixando surface/error/outline do tema escuro iguais aos
 * do claro — o que quebraria o contraste no modo escuro.
 *
 * Cada par `on*`/fundo é validado pelo `ContrastRatioTest` em
 * `core/ui/src/test/kotlin/...` (>= 4.5:1 texto / >= 3.0:1 gráfico).
 */
internal val BrainOutDarkColors = darkColorScheme(
    primary = BrainOutDarkPrimary,
    onPrimary = BrainOutDarkOnPrimary,
    primaryContainer = BrainOutDarkPrimaryContainer,
    onPrimaryContainer = BrainOutDarkOnPrimaryContainer,
    secondary = BrainOutDarkSecondary,
    onSecondary = BrainOutDarkOnSecondary,
    secondaryContainer = BrainOutDarkSecondaryContainer,
    onSecondaryContainer = BrainOutDarkOnSecondaryContainer,
    tertiary = BrainOutDarkTertiary,
    onTertiary = BrainOutDarkOnTertiary,
    tertiaryContainer = BrainOutDarkTertiaryContainer,
    onTertiaryContainer = BrainOutDarkOnTertiaryContainer,
    error = BrainOutDarkError,
    onError = BrainOutDarkOnError,
    errorContainer = BrainOutDarkErrorContainer,
    onErrorContainer = BrainOutDarkOnErrorContainer,
    background = BrainOutDarkBackground,
    onBackground = BrainOutDarkOnBackground,
    surface = BrainOutDarkSurface,
    onSurface = BrainOutDarkOnSurface,
    surfaceVariant = BrainOutDarkSurfaceVariant,
    onSurfaceVariant = BrainOutDarkOnSurfaceVariant,
    outline = BrainOutDarkOutline

)

/**
 * Resolve o `ColorScheme` BrainOut estático (sem dynamic color).
 *
 * Função pura e determinística — recebe `darkTheme: Boolean` e retorna
 * a paleta correspondente. Existe desacoplada de `BrainOutTheme`
 * justamente para ser testável em Robolectric sem montar um Composition
 * tree: o teste unitário (E4.5) afirma que o esquema claro/escuro é
 * o `BrainOutLightColors`/`BrainOutDarkColors` segundo a flag,
 * validando o critério "alternância segue a configuração do sistema".
 */
fun resolveBrainOutStaticColorScheme(darkTheme: Boolean): ColorScheme =
    if (darkTheme) BrainOutDarkColors else BrainOutLightColors

/**
 * Tema Compose raiz do BrainOut.
 *
 * Por padrão NÃO habilitamos `dynamicColor` (Material You) para garantir
 * identidade visual estável entre devices — a especificação E1.3 pede
 * tokens consistentes em PRs.
 *
 * Para ligar dynamic color em builds internos, basta passar
 * `dynamicColor = true`.
 *
 * Alternância de tema (E4.5): segue a configuração do sistema via
 * `isSystemInDarkTheme()`; só aceita override explícito quando o
 * caller passa `darkTheme = ...` (ex.: previews, testes Compose).
 * Nenhum toggle manual é obrigatório — se uma versão futura
 * adicionar override em Configurações, deve persistir a escolha em
 * DataStore com default "seguir sistema".
 */
@Composable
fun BrainOutTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        resolveBrainOutStaticColorScheme(darkTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BrainOutTypography,
        shapes = BrainOutShapes,
        content = content
    )
}
