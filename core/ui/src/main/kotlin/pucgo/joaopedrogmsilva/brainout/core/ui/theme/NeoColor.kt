// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Tokens de cor do redesign neobrutalista (paleta A, «Bloco de ação»).

package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Primitivos da paleta A. São os únicos valores de cor literais do sistema
 * novo: todo papel semântico abaixo resolve para um destes. Valores
 * canônicos em `docs/design/tokens.json` (bloco `primitives`).
 *
 * Ficam em `object` interno porque nada fora de `:core:ui` deve usar cor
 * primitiva direto — a UI consome papéis ([NeoColors]) ou o
 * `MaterialTheme.colorScheme`.
 */
internal object NeoPrimitive {
    val ink = Color(0xFF181818)
    val paper = Color(0xFFF4F4F0)
    val white = Color(0xFFFFFFFF)
    val neutral = Color(0xFFE5E5DE)
    val muted = Color(0xFF54544C)
    val yellow = Color(0xFFF9D94A)
    val violet = Color(0xFFB5A1F5)
    val mint = Color(0xFFA8E6CF)
    val blue = Color(0xFFA5D8FF)
    val orange = Color(0xFFFFBA8C)
    val night = Color(0xFF191919)
    val charcoal = Color(0xFF242424)
    val graphite = Color(0xFF303030)
    val mutedDark = Color(0xFFC6C6BE)
    val shadowDark = Color(0xFFB7B7AE)
    val ochre = Color(0xFF544500)
    val purple = Color(0xFF51368F)
    val purpleLight = Color(0xFFC8B5FF)
    val purpleDeep = Color(0xFF38294F)
    val purplePaper = Color(0xFFE7DAFF)
    val green = Color(0xFF165C47)
    val greenPaper = Color(0xFFDCF4E8)
    val greenDeep = Color(0xFF203F34)
    val greenLight = Color(0xFFBBF1DA)
    val blueInk = Color(0xFF174F7B)
    val bluePaper = Color(0xFFDCEEFF)
    val blueDeep = Color(0xFF20374A)
    val amberInk = Color(0xFF665000)
    val amberPaper = Color(0xFFFFF0B8)
    val amberDeep = Color(0xFF453B1E)
    val amberLight = Color(0xFFFFE681)
    val red = Color(0xFFA51D2D)
    val redInk = Color(0xFF741C1C)
    val redPaper = Color(0xFFFFE0E0)
    val redDeep = Color(0xFF4A2424)
    val redLight = Color(0xFFFFB4B4)
    val black = Color(0xFF000000)
}

/**
 * Papéis semânticos de cor do tema claro e escuro.
 *
 * Imutável de propósito: o valor de cada papel é fixo por tema e trocar de
 * tema troca o objeto inteiro. Nada aqui é `var`, nada é derivado por
 * opacidade global — texto desabilitado, por exemplo, tem primitivo próprio
 * (`disabled.text`), porque reduzir alfa derruba o contraste abaixo do
 * mínimo exigido (`DESIGN.md` §6).
 */
data class NeoColors(
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val borderDefault: Color,
    val borderOnAccent: Color,
    val focus: Color,
    val shadow: Color,
    val actionBackground: Color,
    val actionText: Color,
    val brandBackground: Color,
    val brandText: Color,
    val link: Color,
    val onLink: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val secondaryText: Color,
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val successText: Color,
    val info: Color,
    val infoContainer: Color,
    val infoText: Color,
    val warning: Color,
    val warningContainer: Color,
    val warningText: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val errorText: Color,
    val disabledBackground: Color,
    val disabledText: Color,
    val inverseBackground: Color,
    val inverseText: Color,
    val inversePrimary: Color,
    val scrim: Color,
) {
    companion object {
        /** Tema claro. Papéis conforme `themes.light` de `tokens.json`. */
        val Light =
            NeoColors(
                background = NeoPrimitive.paper,
                surface = NeoPrimitive.white,
                surfaceAlt = NeoPrimitive.neutral,
                textPrimary = NeoPrimitive.ink,
                textMuted = NeoPrimitive.muted,
                borderDefault = NeoPrimitive.ink,
                borderOnAccent = NeoPrimitive.ink,
                focus = NeoPrimitive.ink,
                shadow = NeoPrimitive.ink,
                actionBackground = NeoPrimitive.yellow,
                actionText = NeoPrimitive.ink,
                brandBackground = NeoPrimitive.violet,
                brandText = NeoPrimitive.ink,
                link = NeoPrimitive.ochre,
                onLink = NeoPrimitive.white,
                secondary = NeoPrimitive.purple,
                onSecondary = NeoPrimitive.white,
                secondaryContainer = NeoPrimitive.violet,
                secondaryText = NeoPrimitive.ink,
                success = NeoPrimitive.green,
                onSuccess = NeoPrimitive.white,
                successContainer = NeoPrimitive.greenPaper,
                successText = NeoPrimitive.green,
                info = NeoPrimitive.blueInk,
                infoContainer = NeoPrimitive.bluePaper,
                infoText = NeoPrimitive.blueInk,
                warning = NeoPrimitive.amberInk,
                warningContainer = NeoPrimitive.amberPaper,
                warningText = NeoPrimitive.amberInk,
                error = NeoPrimitive.red,
                onError = NeoPrimitive.white,
                errorContainer = NeoPrimitive.redPaper,
                errorText = NeoPrimitive.redInk,
                disabledBackground = NeoPrimitive.neutral,
                disabledText = NeoPrimitive.muted,
                inverseBackground = NeoPrimitive.ink,
                inverseText = NeoPrimitive.paper,
                inversePrimary = NeoPrimitive.yellow,
                scrim = NeoPrimitive.black,
            )

        /** Tema escuro. Papéis conforme `themes.dark` de `tokens.json`. */
        val Dark =
            NeoColors(
                background = NeoPrimitive.night,
                surface = NeoPrimitive.charcoal,
                surfaceAlt = NeoPrimitive.graphite,
                textPrimary = NeoPrimitive.paper,
                textMuted = NeoPrimitive.mutedDark,
                borderDefault = NeoPrimitive.paper,
                borderOnAccent = NeoPrimitive.ink,
                focus = NeoPrimitive.paper,
                shadow = NeoPrimitive.shadowDark,
                actionBackground = NeoPrimitive.yellow,
                actionText = NeoPrimitive.ink,
                brandBackground = NeoPrimitive.violet,
                brandText = NeoPrimitive.ink,
                link = NeoPrimitive.yellow,
                onLink = NeoPrimitive.ink,
                secondary = NeoPrimitive.purpleLight,
                onSecondary = NeoPrimitive.ink,
                secondaryContainer = NeoPrimitive.purpleDeep,
                secondaryText = NeoPrimitive.purplePaper,
                success = NeoPrimitive.mint,
                onSuccess = NeoPrimitive.ink,
                successContainer = NeoPrimitive.greenDeep,
                successText = NeoPrimitive.greenLight,
                info = NeoPrimitive.blue,
                infoContainer = NeoPrimitive.blueDeep,
                infoText = NeoPrimitive.bluePaper,
                warning = NeoPrimitive.amberLight,
                warningContainer = NeoPrimitive.amberDeep,
                warningText = NeoPrimitive.amberLight,
                error = NeoPrimitive.redLight,
                onError = NeoPrimitive.ink,
                errorContainer = NeoPrimitive.redDeep,
                errorText = NeoPrimitive.redLight,
                disabledBackground = NeoPrimitive.graphite,
                disabledText = NeoPrimitive.mutedDark,
                inverseBackground = NeoPrimitive.paper,
                inverseText = NeoPrimitive.ink,
                inversePrimary = NeoPrimitive.ochre,
                scrim = NeoPrimitive.black,
            )
    }
}

/**
 * Composição local dos papéis novos.
 *
 * Permite que componentes de `:core:ui` leiam `LocalNeoColors.current` sem
 * receber o objeto por parâmetro, sem quebrar o `MaterialTheme` que as
 * rotas legadas ainda usam. O default é o tema claro para que previews e
 * testes não precisem montar o wrapper (NB-06).
 */
val LocalNeoColors = staticCompositionLocalOf { NeoColors.Light }

/**
 * Mapeia os papéis novos para o `ColorScheme` do Material 3.
 *
 * Ponto deliberado do desenho: `primary` **não** é o amarelo. No claro, o
 * amarelo como `primary` faria labels e links do Material perderem
 * contraste; por isso `primary = link` e o botão principal do BrainOut usa
 * explicitamente os papéis de ação. Os níveis de superfície, o `scrim` e os
 * inversos também são mapeados, para não sobrar roxo padrão do Material em
 * nenhum canto da árvore.
 */
internal fun NeoColors.toMaterialColorScheme(dark: Boolean): ColorScheme =
    if (dark) {
        darkColorScheme(
            primary = link,
            onPrimary = onLink,
            primaryContainer = actionBackground,
            onPrimaryContainer = actionText,
            inversePrimary = inversePrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = secondaryText,
            tertiary = success,
            onTertiary = onSuccess,
            tertiaryContainer = successContainer,
            onTertiaryContainer = successText,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = errorText,
            background = background,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceAlt,
            onSurfaceVariant = textMuted,
            surfaceTint = link,
            inverseSurface = inverseBackground,
            inverseOnSurface = inverseText,
            outline = borderDefault,
            outlineVariant = borderDefault,
            scrim = scrim,
            surfaceBright = surface,
            surfaceDim = surfaceAlt,
            surfaceContainerLowest = background,
            surfaceContainerLow = surface,
            surfaceContainer = surface,
            surfaceContainerHigh = surfaceAlt,
            surfaceContainerHighest = surfaceAlt,
        )
    } else {
        lightColorScheme(
            primary = link,
            onPrimary = onLink,
            primaryContainer = actionBackground,
            onPrimaryContainer = actionText,
            inversePrimary = inversePrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = secondaryText,
            tertiary = success,
            onTertiary = onSuccess,
            tertiaryContainer = successContainer,
            onTertiaryContainer = successText,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = errorText,
            background = background,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceAlt,
            onSurfaceVariant = textMuted,
            surfaceTint = link,
            inverseSurface = inverseBackground,
            inverseOnSurface = inverseText,
            outline = borderDefault,
            outlineVariant = borderDefault,
            scrim = scrim,
            surfaceBright = surface,
            surfaceDim = surfaceAlt,
            surfaceContainerLowest = background,
            surfaceContainerLow = surface,
            surfaceContainer = surface,
            surfaceContainerHigh = surfaceAlt,
            surfaceContainerHighest = surfaceAlt,
        )
    }
