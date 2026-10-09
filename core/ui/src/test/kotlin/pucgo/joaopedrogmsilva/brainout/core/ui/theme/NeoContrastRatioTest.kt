// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// WB-04: contraste calculado dos pares declarados na especificação.

package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Contraste dos pares reais da paleta A, nos dois temas.
 *
 * A fórmula WCAG é reimplementada aqui de propósito. O `DESIGN.md` publica a
 * razão de cada par; se o número publicado estiver errado, um teste que
 * reutilizasse a implementação do documento passaria em falso. Aqui o
 * cálculo é independente e o critério é o valor completo, sem
 * arredondamento para aprovação — 4,49:1 reprova, mesmo "parecendo" 4,5.
 */
class NeoContrastRatioTest {
    private fun linearize(channel: Float): Double {
        val c = channel.toDouble()
        return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double =
        0.2126 * linearize(color.red) +
            0.7152 * linearize(color.green) +
            0.0722 * linearize(color.blue)

    private fun contrast(
        foreground: Color,
        background: Color,
    ): Double {
        val a = luminance(foreground)
        val b = luminance(background)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    private fun assertPairs(
        colors: NeoColors,
        themeName: String,
    ) {
        declaredContrastPairs.forEach { pair ->
            val ratio =
                contrast(
                    foreground = neoRole(colors, pair.foreground),
                    background = neoRole(colors, pair.background),
                )
            assertThat(ratio).isAtLeast(pair.minimum)
            // Guarda também a razão em 2 casas para o relatório de falha ser
            // legível: o valor completo é o critério, o arredondado é leitura.
            assertThat(ratio.roundToInt()).isAtLeast(pair.minimum.roundToInt())
            assertThat(themeName).isNotEmpty()
        }
    }

    @Test
    fun `todos os pares declarados passam no tema claro`() {
        assertPairs(NeoColors.Light, "light")
    }

    @Test
    fun `todos os pares declarados passam no tema escuro`() {
        assertPairs(NeoColors.Dark, "dark")
    }

    @Test
    fun `os dois temas tem a mesma quantidade de pares verificados`() {
        // Proteção contra "conserto" por remoção: se alguém apagar um par da
        // lista compartilhada, a contagem muda e este teste avisa.
        assertThat(declaredContrastPairs).hasSize(29)
        assertThat(declaredContrastPairs.count { it.minimum == 4.5 }).isEqualTo(23)
        assertThat(declaredContrastPairs.count { it.minimum == 3.0 }).isEqualTo(6)
    }

    @Test
    fun `texto principal e muted mantem leitura nos dois temas`() {
        val light = contrast(NeoColors.Light.textPrimary, NeoColors.Light.background)
        val dark = contrast(NeoColors.Dark.textPrimary, NeoColors.Dark.background)
        val mutedLight = contrast(NeoColors.Light.textMuted, NeoColors.Light.background)
        val mutedDark = contrast(NeoColors.Dark.textMuted, NeoColors.Dark.background)

        assertThat(light).isAtLeast(15.0)
        assertThat(dark).isAtLeast(15.0)
        assertThat(mutedLight).isAtLeast(6.0)
        assertThat(mutedDark).isAtLeast(6.0)
    }

    @Test
    fun `papel de acao mantem texto ink legivel sobre amarelo`() {
        // O botão principal é amarelo com texto ink nos dois temas: é o par
        // que mais aparece em tela e o que inspirou não usar amarelo em
        // `primary` (labels/links do Material perderiam contraste).
        assertThat(contrast(NeoColors.Light.actionText, NeoColors.Light.actionBackground))
            .isAtLeast(12.0)
        assertThat(contrast(NeoColors.Dark.actionText, NeoColors.Dark.actionBackground))
            .isAtLeast(12.0)
    }
}
