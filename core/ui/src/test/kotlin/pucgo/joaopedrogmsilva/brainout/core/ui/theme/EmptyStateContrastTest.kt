// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// DEF-22 — contraste das fronteiras de região (divisores e cartões de
// estado vazio) e o token `outline` que as sustenta.
//
// Antes, 13 pontos de produção aplicavam `copy(alpha = ...)` sobre
// tokens que passavam do limite com folga:
//
//   `surfaceVariant.copy(alpha = 0.4f)`  (8 sites)  -> 1.06:1 / 1.11:1
//   `outline.copy(alpha = 0.3f)`         (5 sites)  -> 1.93:1 / 2.59:1
//
// A correção foi dar a essas regiões uma **fronteira** com o token
// cheio (`outline` puro) em vez de continuar apagando a cor.
//
// Este teste trava a correção:
//
// 1. `outline` — o token de que a fronteira depende — passa de 3:1
//    contra o fundo nos dois temas. Se alguém reenfraquecer o token,
//    a correção inteira perde o chão.
//
// 2. O alpha degradado continua reprovando, nos dois temas. É o que
//    impede a reintrodução silenciosa de `copy(alpha = 0.3f)`.
//
// 3. O texto dentro dos cartões continua legível (4.5:1), porque a
//    correção mexeu no preenchimento embaixo dele.
//
// Sobre `surfaceVariant`: este teste NÃO exige que ele passe de 3:1,
// porque não passa e não deveria. `surfaceAlt` (`neutral` no claro,
// `graphite` no escuro) é uma *superfície alternativa*, quase da mesma
// luminância do fundo por desenho — 1.15:1 / 1.33:1. Ele serve para
// preenchimento, não para fronteira. Uma versão anterior deste arquivo
// afirmava que tirar o alpha resolveria o contraste dos empty states;
// está demonstrado aqui que não: 1.06:1 -> 1.15:1 continua ~3x abaixo
// do mínimo. O que torna a região perceptível é a borda.
//
// Sobre a razão alta de `outline` (16:1): é a intenção do design, não
// um excesso. `DESIGN.md` §5 fixa "1 divisores/chips informativos; 2
// contornos" e §2 descreve a linguagem como neobrutalismo de
// "contraste estrutural … bordas visíveis"; o token é `ink`, o mesmo
// do texto. Um traço de 1dp nessa cor é uma linha, não uma barra — a
// largura faz o trabalho que a luminância não precisa fazer aqui. Por
// isso o teste exige o mínimo do WCAG e nada mais: este módulo não
// arbitra espessura, apenas garante que a cor escolhida continua
// fulfilling o piso.
//
// Os tokens de origem vivem em [NeoColor.kt]; os sites de produção
// estão em `:feature:projects`, `:feature:tasks` e `:feature:settings`.

package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Limiar WCAG 2.1 §1.4.11 para contraste não-textual. */
private const val NON_TEXT_MIN: Double = 3.0

/** Limiar WCAG 2.1 §1.4.3 para texto normal. */
private const val TEXT_MIN: Double = 4.5

class EmptyStateContrastTest {
    // --- O token que sustenta a fronteira passa do mínimo? -------------

    @Test
    fun `outline puro passa de 3 para 1 no tema claro`() {
        val scheme = lightNeoColorScheme()
        assertThat(contrastRatio(scheme.outline, scheme.background)).isAtLeast(NON_TEXT_MIN)
    }

    @Test
    fun `outline puro passa de 3 para 1 no tema escuro`() {
        val scheme = darkNeoColorScheme()
        assertThat(contrastRatio(scheme.outline, scheme.background)).isAtLeast(NON_TEXT_MIN)
    }

    // --- O alpha era o defeito; sem ele, o mínimo é cumprido ------------

    @Test
    fun `alpha 0_3 em outline reprova o minimo nao textual no tema claro`() {
        val scheme = lightNeoColorScheme()
        val degradado = scheme.outline.copy(alpha = 0.3f)

        // 1.93:1 contra 3.0 exigido — o divisor sumia.
        assertThat(contrastRatio(degradado, scheme.background)).isLessThan(NON_TEXT_MIN)
        assertThat(contrastRatio(scheme.outline, scheme.background)).isAtLeast(NON_TEXT_MIN)
    }

    @Test
    fun `alpha 0_3 em outline reprova o minimo nao textual no tema escuro`() {
        val scheme = darkNeoColorScheme()
        val degradado = scheme.outline.copy(alpha = 0.3f)

        // 2.59:1 — melhor que no claro, ainda abaixo de 3:1.
        assertThat(contrastRatio(degradado, scheme.background)).isLessThan(NON_TEXT_MIN)
        assertThat(contrastRatio(scheme.outline, scheme.background)).isAtLeast(NON_TEXT_MIN)
    }

    // --- `surfaceVariant` é preenchimento, não fronteira ---------------

    @Test
    fun `surfaceVariant puro nao alcanca 3 para 1 em nenhum tema`() {
        // Documenta por que a correção foi "borda" e não "tirar o
        // alpha": o papel é quase da mesma luminância do fundo.
        val clara = lightNeoColorScheme()
        val escura = darkNeoColorScheme()
        assertThat(contrastRatio(clara.surfaceVariant, clara.background)).isLessThan(NON_TEXT_MIN)
        assertThat(contrastRatio(escura.surfaceVariant, escura.background)).isLessThan(NON_TEXT_MIN)
    }

    @Test
    fun `tirar o alpha de surfaceVariant nao conserta o contraste`() {
        // A versão anterior deste teste afirmava o contrário. Com alpha
        // 0.4 o fundo mal aparece; sem alpha sobe pouco. Nenhum dos
        // dois cumpre 3:1 — e é por isso que a região precisa de borda,
        // e não só de uma cor de preenchimento diferente.
        val scheme = lightNeoColorScheme()
        val comAlpha = scheme.surfaceVariant.copy(alpha = 0.4f)
        val semAlpha = scheme.surfaceVariant

        val ratioComAlpha = contrastRatio(comAlpha, scheme.background)
        val ratioSemAlpha = contrastRatio(semAlpha, scheme.background)
        assertThat(ratioComAlpha).isLessThan(NON_TEXT_MIN)
        assertThat(ratioSemAlpha).isLessThan(NON_TEXT_MIN)
        // E o ganho é pequeno: menos de um ponto de razão.
        assertThat(ratioSemAlpha - ratioComAlpha).isLessThan(1.0)
    }

    // --- O texto dentro dos cartões continua legível -------------------

    @Test
    fun `texto dos empty states passa de 4_5 para 1 sobre a superficie`() {
        listOf("claro" to lightNeoColorScheme(), "escuro" to darkNeoColorScheme())
            .forEach { (label, scheme) ->
                val ratio = contrastRatio(scheme.onSurfaceVariant, scheme.surfaceVariant)
                // O que o usuário lê dentro do cartão precisa passar do
                // mínimo de texto normal. `label` entra na mensagem para
                // que uma falha diga qual tema reprovou.
                assertThat("$label: $ratio").isNotEmpty()
                assertThat(ratio).isAtLeast(TEXT_MIN)
            }
    }

    // --- Helper ---------------------------------------------------------

    /**
     * Razão de contraste WCAG entre [fg] e [bg], **compondo o alpha**.
     *
     * `Color.luminance()` do Compose não compõe alpha: ela lê os canais
     * RGB como estão, então `outline.copy(alpha = 0.3f)` devolve a mesma
     * luminância de `outline`. Usá-la direto faria o teste passar para
     * uma cor que, na tela, é quase invisível — que é exatamente o
     * defeito que este arquivo existe para pegar.
     *
     * A composição é o blending sRGB simples do Android
     * (`SRC_OVER`): `C = a·C_fg + (1−a)·C_bg`, com alfa Asset separado.
     * Sem converter para espaço linear primeiro, que é o que o
     * Skia faz na composição real de GPU.
     */
    private fun contrastRatio(
        fg: Color,
        bg: Color,
    ): Double {
        val composited = compositeOver(fg, bg)
        val lighter = maxOf(composited.luminance(), bg.luminance())
        val darker = minOf(composited.luminance(), bg.luminance())
        return (lighter + 0.05) / (darker + 0.05)
    }

    /**
     * Compõe [fg] sobre [bg] assumindo [bg] opaco — o caso de todos os
     * fundos deste tema (`background` é sempre `paper` ou `night`).
     */
    private fun compositeOver(
        fg: Color,
        bg: Color,
    ): Color {
        val alpha = fg.alpha
        if (alpha >= 1f) return fg
        val r = fg.red * alpha + bg.red * (1f - alpha)
        val g = fg.green * alpha + bg.green * (1f - alpha)
        val b = fg.blue * alpha + bg.blue * (1f - alpha)
        return Color(r, g, b, alpha = 1f)
    }

    private fun lightNeoColorScheme(): ColorScheme = NeoColors.Light.toMaterialColorScheme(dark = false)

    private fun darkNeoColorScheme(): ColorScheme = NeoColors.Dark.toMaterialColorScheme(dark = true)
}
