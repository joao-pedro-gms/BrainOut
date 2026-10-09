// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// D-01 / AC-0.1..0.3 — a classificação da janela é função pura e dos tokens.
//
// Teste de tabela de `NeoWindowInfo.fromSize()`. Os tamanhos de entrada são
// as **bordas** dos breakpoints: 0, o token - 1dp, o token exato, e o token
// seguinte - 1dp. E os tokens lidos em cada asserção vêm de `NeoLayout`, nunca
// de um literal — se alguém mover `mediumMinWidth`, este teste acompanha; se
// as asserções divergirem do token, o próprio token tem o comentário certo e
// `NeoTokenParityTest` é quem julga o número.
//
// Sem Robolectric de propósito: `fromSize` é pura, não toca `Context`,
// `Configuration` nem Android. É essa pureza que a torna testável por tabela,
// e é por isso que o projeto não adotou
// `calculateWindowSizeClass(activity)` — que exigiria uma Activity para cada
// caso da tabela.

package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

class NeoWindowInfoTest {
    // --- AC-0.1: tabela de largura ----------------------------------------

    @Test
    fun `largura classifica pelos breakpoints de NeoLayout`() {
        // Entradas: 0, token - 1dp, o token, e o token seguinte - 1dp.
        val cases =
            listOf(
                Triple(NeoWidth.COMPACT, 0.dp, "abaixo de tudo"),
                Triple(NeoWidth.COMPACT, NeoLayout.mediumMinWidth - 1.dp, "1dp antes da média"),
                Triple(NeoWidth.MEDIUM, NeoLayout.mediumMinWidth, "no limite da média"),
                Triple(NeoWidth.MEDIUM, NeoLayout.expandedMinWidth - 1.dp, "1dp antes da expandida"),
                Triple(NeoWidth.EXPANDED, NeoLayout.expandedMinWidth, "no limite da expandida"),
                Triple(NeoWidth.EXPANDED, NeoLayout.largeMinWidth - 1.dp, "1dp antes da grande"),
                Triple(NeoWidth.LARGE, NeoLayout.largeMinWidth, "no limite da grande"),
            )

        cases.forEach { (expected, widthDp, label) ->
            val info = NeoWindowInfo.fromSize(widthDp, TALL_HEIGHT)

            assertWithMessage("largura em $widthDp ($label)").that(info.width).isEqualTo(expected)
            // fromSize não classifica: ele mede. O número tem de chegar intacto.
            assertWithMessage("widthDp preservado em $widthDp ($label)").that(info.widthDp).isEqualTo(widthDp)
        }
    }

    @Test
    fun `os breakpoints da tabela sao as bordas dos tokens de NeoLayout`() {
        // Trava o que a tabela acima pressupõe. Se esta ordem mudar, a tabela
        // deixa de estar medindo "borda de token" e a prova perde o sentido.
        val tokens = listOf(NeoLayout.mediumMinWidth, NeoLayout.expandedMinWidth, NeoLayout.largeMinWidth)

        assertThat(tokens).isInStrictOrder()
        assertThat(tokens.map { it.value }).containsExactly(600f, 840f, 1200f).inOrder()
    }

    @Test
    fun `uma janela media ou maior conta como larga`() {
        val compact = NeoWindowInfo.fromSize(NeoLayout.mediumMinWidth - 1.dp, TALL_HEIGHT)
        val medium = NeoWindowInfo.fromSize(NeoLayout.mediumMinWidth, TALL_HEIGHT)
        val expanded = NeoWindowInfo.fromSize(NeoLayout.expandedMinWidth, TALL_HEIGHT)
        val large = NeoWindowInfo.fromSize(NeoLayout.largeMinWidth, TALL_HEIGHT)

        assertThat(compact.isWide).isFalse()
        assertThat(medium.isWide).isTrue()
        assertThat(expanded.isWide).isTrue()
        assertThat(large.isWide).isTrue()
    }

    @Test
    fun `a margem horizontal segue o bucket de largura`() {
        val compact = NeoWindowInfo.fromSize(0.dp, TALL_HEIGHT)
        val medium = NeoWindowInfo.fromSize(NeoLayout.mediumMinWidth, TALL_HEIGHT)
        val expanded = NeoWindowInfo.fromSize(NeoLayout.expandedMinWidth, TALL_HEIGHT)
        val large = NeoWindowInfo.fromSize(NeoLayout.largeMinWidth, TALL_HEIGHT)

        assertThat(compact.margin).isEqualTo(NeoLayout.compactMargin)
        assertThat(medium.margin).isEqualTo(NeoLayout.mediumMargin)
        assertThat(expanded.margin).isEqualTo(NeoLayout.expandedMargin)
        // ≥1200dp continua na margem expandida: o design não define uma
        // quinta margem, e o número extra é `contentMaxWidth` (D-03).
        assertThat(large.margin).isEqualTo(NeoLayout.expandedMargin)
    }

    @Test
    fun `as margens por bucket sao as declaradas em NeoLayout`() {
        assertThat(NeoLayout.compactMargin).isEqualTo(16.dp)
        assertThat(NeoLayout.mediumMargin).isEqualTo(24.dp)
        assertThat(NeoLayout.expandedMargin).isEqualTo(32.dp)
    }

    // --- AC-0.2: o corte de altura é exclusivo -----------------------------

    @Test
    fun `altura abaixo do token de compactHeightMaxExclusive forca coluna unica`() {
        val below = NeoWindowInfo.fromSize(WIDE_WIDTH, NeoLayout.compactHeightMaxExclusive - 1.dp)

        assertThat(below.forceSingleColumn).isTrue()
        assertThat(below.height).isEqualTo(NeoHeight.COMPACT)
    }

    @Test
    fun `altura exatamente no token ja libera coluna unica`() {
        val at = NeoWindowInfo.fromSize(WIDE_WIDTH, NeoLayout.compactHeightMaxExclusive)

        assertThat(at.forceSingleColumn).isFalse()
        assertThat(at.height).isNotEqualTo(NeoHeight.COMPACT)
    }

    @Test
    fun `o corte de altura nao depende da largura da janela`() {
        // Paisagem num celular estreito e num tablet largo: a altura manda na
        // regra, a largura não. É o que DESIGN.md §5 quer ("com altura <480dp,
        // priorizar rolagem e coluna única"), independente do aparelho.
        val narrow = NeoWindowInfo.fromSize(NeoLayout.mediumMinWidth - 1.dp, 400.dp)
        val wide = NeoWindowInfo.fromSize(NeoLayout.largeMinWidth, 400.dp)

        assertThat(narrow.forceSingleColumn).isTrue()
        assertThat(wide.forceSingleColumn).isTrue()
    }

    // --- AC-0.3: default seguro do CompositionLocal ------------------------

    @Test
    fun `o default do LocalNeoWindowInfo e a janela mais apertada`() {
        // A leitura da CompositionLocal em si (com e sem provider) é de
        // `NeoWindowInfoCompositionLocalTest` — montar composição exige
        // Robolectric. Aqui fica o valor, que é puro.
        val default = DefaultNeoWindowInfo

        assertThat(default.width).isEqualTo(NeoWidth.COMPACT)
        assertThat(default.height).isEqualTo(NeoHeight.COMPACT)
        assertThat(default.isWide).isFalse()
        assertThat(default.margin).isEqualTo(NeoLayout.compactMargin)
        assertThat(default.forceSingleColumn).isTrue()
    }

    // --- pureza ------------------------------------------------------------

    @Test
    fun `fromSize e pura e determinista`() {
        val first = NeoWindowInfo.fromSize(NeoLayout.expandedMinWidth, NeoLayout.compactHeightMaxExclusive)
        val second = NeoWindowInfo.fromSize(NeoLayout.expandedMinWidth, NeoLayout.compactHeightMaxExclusive)

        assertThat(first).isEqualTo(second)
    }

    @Test
    fun `width e height sao consistentes com os tamanhos medidos`() {
        listOf(0.dp, 320.dp, NeoLayout.mediumMinWidth, 1000.dp, NeoLayout.largeMinWidth).forEach { widthDp ->
            listOf(240.dp, 480.dp, 900.dp).forEach { heightDp ->
                val info = NeoWindowInfo.fromSize(widthDp, heightDp)

                assertThat(info.width).isEqualTo(neoWidthOf(widthDp))
                assertThat(info.height).isEqualTo(neoHeightOf(heightDp))
                assertThat(info.widthDp).isEqualTo(widthDp)
                assertThat(info.heightDp).isEqualTo(heightDp)
            }
        }
    }

    private companion object {
        /** Altura folgada: isola o teste de largura do corte de coluna única. */
        val TALL_HEIGHT: Dp = NeoLayout.largeMinWidth

        /** Largura folgada: isola o teste de altura da classificação de largura. */
        val WIDE_WIDTH: Dp = NeoLayout.largeMinWidth
    }
}
