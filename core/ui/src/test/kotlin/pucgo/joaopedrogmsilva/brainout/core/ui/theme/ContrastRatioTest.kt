// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Marco E4.4 do ROADMAP.md — acessibilidade AA.
// Testes automatizados do contraste WCAG 2.1 dos tokens do tema Neo
// (paleta A) em ambos os modos (claro/escuro).
//
// O algoritmo sRGB + luminância relativa + razão de contraste vem
// de https://www.w3.org/TR/WCAG21/#dfn-relative-luminance e
// https://www.w3.org/TR/WCAG21/#dfn-contrast-ratio. Aplicamos:
//  - texto normal: razão >= 4.5:1 (nível AA);
//  - texto grande / gráfico (ícone, contorno, divider): >= 3.0:1
//    (nível AA para "Non-text Contrast", WCAG 1.4.11).
//
// O alvo é o `ColorScheme` que o app **renderiza** (`toMaterialColorScheme`),
// não a paleta crua: é o esquema mapeado que o Material 3 consome.
//
// Estes testes rodam em JVM puro (sem Robolectric) porque os tokens
// são `Color` e o algoritmo é matemática — não precisamos do ciclo de
// vida do Android para validar a razão.

package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Limiares WCAG 2.1 AA.
 *
 *  - [TEXT_RATIO_MIN] 4.5:1 para texto normal (corpo, labels).
 *  - [GRAPHIC_RATIO_MIN] 3.0:1 para componentes gráficos (ícones
 *    funcionais, contornos, divisores, indicadores de foco).
 */
private const val TEXT_RATIO_MIN: Double = 4.5
private const val GRAPHIC_RATIO_MIN: Double = 3.0

class ContrastRatioTest {
    private val light: ColorScheme = NeoColors.Light.toMaterialColorScheme()
    private val dark: ColorScheme = NeoColors.Dark.toMaterialColorScheme()

    // --- Light theme ----------------------------------------------------

    @Test
    fun light_onPrimary_on_primary_meets_aa() {
        assertRatio(
            fg = light.onPrimary,
            bg = light.primary,
            min = TEXT_RATIO_MIN,
            label = "light onPrimary/primary",
        )
    }

    @Test
    fun light_onPrimaryContainer_on_primaryContainer_meets_aa() {
        assertRatio(
            fg = light.onPrimaryContainer,
            bg = light.primaryContainer,
            min = TEXT_RATIO_MIN,
            label = "light onPrimaryContainer/primaryContainer",
        )
    }

    @Test
    fun light_onSecondary_on_secondary_meets_aa() {
        assertRatio(
            fg = light.onSecondary,
            bg = light.secondary,
            min = TEXT_RATIO_MIN,
            label = "light onSecondary/secondary",
        )
    }

    @Test
    fun light_onSecondaryContainer_on_secondaryContainer_meets_aa() {
        assertRatio(
            fg = light.onSecondaryContainer,
            bg = light.secondaryContainer,
            min = TEXT_RATIO_MIN,
            label = "light onSecondaryContainer/secondaryContainer",
        )
    }

    @Test
    fun light_onTertiary_on_tertiary_meets_aa() {
        assertRatio(
            fg = light.onTertiary,
            bg = light.tertiary,
            min = TEXT_RATIO_MIN,
            label = "light onTertiary/tertiary",
        )
    }

    @Test
    fun light_onTertiaryContainer_on_tertiaryContainer_meets_aa() {
        assertRatio(
            fg = light.onTertiaryContainer,
            bg = light.tertiaryContainer,
            min = TEXT_RATIO_MIN,
            label = "light onTertiaryContainer/tertiaryContainer",
        )
    }

    @Test
    fun light_onError_on_error_meets_aa() {
        assertRatio(
            fg = light.onError,
            bg = light.error,
            min = TEXT_RATIO_MIN,
            label = "light onError/error",
        )
    }

    @Test
    fun light_onErrorContainer_on_errorContainer_meets_aa() {
        assertRatio(
            fg = light.onErrorContainer,
            bg = light.errorContainer,
            min = TEXT_RATIO_MIN,
            label = "light onErrorContainer/errorContainer",
        )
    }

    @Test
    fun light_onBackground_on_background_meets_aa() {
        assertRatio(
            fg = light.onBackground,
            bg = light.background,
            min = TEXT_RATIO_MIN,
            label = "light onBackground/background",
        )
    }

    @Test
    fun light_onSurface_on_surface_meets_aa() {
        assertRatio(
            fg = light.onSurface,
            bg = light.surface,
            min = TEXT_RATIO_MIN,
            label = "light onSurface/surface",
        )
    }

    @Test
    fun light_onSurfaceVariant_on_surfaceVariant_meets_aa() {
        assertRatio(
            fg = light.onSurfaceVariant,
            bg = light.surfaceVariant,
            min = TEXT_RATIO_MIN,
            label = "light onSurfaceVariant/surfaceVariant",
        )
    }

    @Test
    fun light_outline_on_background_meets_graphic_aa() {
        // outline é componente gráfico (divider, borda de campo); usa
        // o limiar gráfico (3.0:1) e não o de texto.
        assertRatio(
            fg = light.outline,
            bg = light.background,
            min = GRAPHIC_RATIO_MIN,
            label = "light outline/background (graphic)",
        )
    }

    // --- Dark theme -----------------------------------------------------

    @Test
    fun dark_onPrimary_on_primary_meets_aa() {
        assertRatio(
            fg = dark.onPrimary,
            bg = dark.primary,
            min = TEXT_RATIO_MIN,
            label = "dark onPrimary/primary",
        )
    }

    @Test
    fun dark_onPrimaryContainer_on_primaryContainer_meets_aa() {
        assertRatio(
            fg = dark.onPrimaryContainer,
            bg = dark.primaryContainer,
            min = TEXT_RATIO_MIN,
            label = "dark onPrimaryContainer/primaryContainer",
        )
    }

    @Test
    fun dark_onSecondary_on_secondary_meets_aa() {
        assertRatio(
            fg = dark.onSecondary,
            bg = dark.secondary,
            min = TEXT_RATIO_MIN,
            label = "dark onSecondary/secondary",
        )
    }

    @Test
    fun dark_onSecondaryContainer_on_secondaryContainer_meets_aa() {
        assertRatio(
            fg = dark.onSecondaryContainer,
            bg = dark.secondaryContainer,
            min = TEXT_RATIO_MIN,
            label = "dark onSecondaryContainer/secondaryContainer",
        )
    }

    @Test
    fun dark_onTertiary_on_tertiary_meets_aa() {
        assertRatio(
            fg = dark.onTertiary,
            bg = dark.tertiary,
            min = TEXT_RATIO_MIN,
            label = "dark onTertiary/tertiary",
        )
    }

    @Test
    fun dark_onTertiaryContainer_on_tertiaryContainer_meets_aa() {
        assertRatio(
            fg = dark.onTertiaryContainer,
            bg = dark.tertiaryContainer,
            min = TEXT_RATIO_MIN,
            label = "dark onTertiaryContainer/tertiaryContainer",
        )
    }

    @Test
    fun dark_onError_on_error_meets_aa() {
        assertRatio(
            fg = dark.onError,
            bg = dark.error,
            min = TEXT_RATIO_MIN,
            label = "dark onError/error",
        )
    }

    @Test
    fun dark_onErrorContainer_on_errorContainer_meets_aa() {
        assertRatio(
            fg = dark.onErrorContainer,
            bg = dark.errorContainer,
            min = TEXT_RATIO_MIN,
            label = "dark onErrorContainer/errorContainer",
        )
    }

    @Test
    fun dark_onBackground_on_background_meets_aa() {
        assertRatio(
            fg = dark.onBackground,
            bg = dark.background,
            min = TEXT_RATIO_MIN,
            label = "dark onBackground/background",
        )
    }

    @Test
    fun dark_onSurface_on_surface_meets_aa() {
        assertRatio(
            fg = dark.onSurface,
            bg = dark.surface,
            min = TEXT_RATIO_MIN,
            label = "dark onSurface/surface",
        )
    }

    @Test
    fun dark_onSurfaceVariant_on_surfaceVariant_meets_aa() {
        assertRatio(
            fg = dark.onSurfaceVariant,
            bg = dark.surfaceVariant,
            min = TEXT_RATIO_MIN,
            label = "dark onSurfaceVariant/surfaceVariant",
        )
    }

    @Test
    fun dark_outline_on_background_meets_graphic_aa() {
        assertRatio(
            fg = dark.outline,
            bg = dark.background,
            min = GRAPHIC_RATIO_MIN,
            label = "dark outline/background (graphic)",
        )
    }

    // --- helpers --------------------------------------------------------

    /**
     * Calcula a razão de contraste WCAG 2.1 entre [fg] (cor do texto)
     * e [bg] (cor de fundo) e exige que seja >= [min] (4.5 texto, 3.0
     * gráfico). A fórmula é (L1 + 0.05) / (L2 + 0.05), onde L1 é a
     * luminância do tom mais claro e L2 a do mais escuro. A
     * luminância relativa vem do canal sRGB linearizado conforme
     * WCAG (`Color.luminance()` da Compose já implementa).
     */
    private fun assertRatio(
        fg: Color,
        bg: Color,
        min: Double,
        label: String,
    ) {
        val ratio = contrastRatio(fg, bg)
        try {
            assertThat(ratio).isAtLeast(min)
        } catch (e: AssertionError) {
            throw AssertionError(
                "$label — ratio=$ratio (esperado >= $min)",
                e,
            )
        }
    }

    /** Implementação direta de `Color.contrastRatio` da Compose (privado). */
    private fun contrastRatio(
        fg: Color,
        bg: Color,
    ): Double {
        val fgL = fg.luminance()
        val bgL = bg.luminance()
        val lighter = maxOf(fgL, bgL)
        val darker = minOf(fgL, bgL)
        return (lighter + 0.05) / (darker + 0.05)
    }
}
