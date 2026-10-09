// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// D-01 — fundação de janela: a largura/altura **disponíveis** classificadas
// pelos tokens de `NeoLayout`. É o único ponto do app que responde "sou largo?",
// e ele responde pelos tokens do design, não pelos do AndroidX.
//
// Este arquivo **acrescenta**; não altera token nenhum. `NeoLayout` (em
// `NeoTokens.kt`) continua sendo a fonte de verdade dos números — por isso
// aqui não existe literal de dp: tudo é derivado dos tokens.

package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Faixa de largura da janela, pelos breakpoints de [NeoLayout].
 *
 * `LARGE` existe separada de `EXPANDED` porque `DESIGN.md` §5 dá à faixa
 * ≥1200dp uma regra própria (conteúdo de trabalho limitado a
 * [NeoLayout.contentMaxWidth]) — mais espaço não implica cartão gigante.
 * Colapsar as duas em um bucket só perderia essa distinção.
 */
enum class NeoWidth {
    COMPACT,
    MEDIUM,
    EXPANDED,
    LARGE,
}

/**
 * Faixa de altura útil.
 *
 * O design só fixa **um** limiar de altura — [NeoLayout.compactHeightMaxExclusive]
 * (`DESIGN.md` §5: "com altura <480dp, priorizar rolagem e coluna única em
 * formulários"). Os dois degraus acima dele reusam a rampa de largura de
 * [NeoLayout] para que a classificação de altura não introduza um segundo
 * conjunto de números: a fonte de verdade dos breakpoints continua sendo uma
 * só. O comportamento que o design manda observar — [NeoWindowInfo.forceSingleColumn]
 * — depende exclusivamente do token de 480dp, então essa escolha não aparece
 * em nenhuma tela.
 */
enum class NeoHeight {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

/**
 * A janela disponível, já classificada.
 *
 * @param width faixa de largura derivada de [widthDp].
 * @param height faixa de altura derivada de [heightDp].
 * @param widthDp largura **medida**, não o bucket: quem decide layout
 * precisa do número (para um cap, um `GridCells.Adaptive`, uma coluna), não
 * só do rótulo.
 * @param heightDp altura medida, pelo mesmo motivo.
 */
@Immutable
data class NeoWindowInfo(
    val width: NeoWidth,
    val height: NeoHeight,
    val widthDp: Dp,
    val heightDp: Dp,
) {
    /** Verdadeiro a partir de [NeoLayout.mediumMinWidth] — a faixa do rail. */
    val isWide: Boolean get() = width != NeoWidth.COMPACT

    /**
     * Margem horizontal do conteúdo, pelo bucket de largura
     * (`DESIGN.md` §5: 16dp compacto, 24dp médio, 32dp expandido).
     */
    val margin: Dp
        get() =
            when (width) {
                NeoWidth.COMPACT -> NeoLayout.compactMargin
                NeoWidth.MEDIUM -> NeoLayout.mediumMargin
                NeoWidth.EXPANDED, NeoWidth.LARGE -> NeoLayout.expandedMargin
            }

    /**
     * Altura abaixo de [NeoLayout.compactHeightMaxExclusive] força coluna
     * única: com pouca altura, o alvo é rolagem e o campo de formulário
     * visível, não a composição lado a lado (`DESIGN.md` §5).
     *
     * O token é **exclusivo** por nome e por uso: 480dp já comporta duas
     * colunas.
     */
    val forceSingleColumn: Boolean get() = heightDp < NeoLayout.compactHeightMaxExclusive

    companion object
}

/**
 * Classifica [widthDp] pelos breakpoints de [NeoLayout].
 *
 * Os limites são `>=`: o valor do token já marca a entrada na faixa
 * (`DESIGN.md` §5 usa "600–839dp" para a faixa média).
 */
fun neoWidthOf(widthDp: Dp): NeoWidth =
    when {
        widthDp < NeoLayout.mediumMinWidth -> NeoWidth.COMPACT
        widthDp < NeoLayout.expandedMinWidth -> NeoWidth.MEDIUM
        widthDp < NeoLayout.largeMinWidth -> NeoWidth.EXPANDED
        else -> NeoWidth.LARGE
    }

/**
 * Classifica [heightDp]. O corte de [NeoLayout.compactHeightMaxExclusive] é o
 * único limiar que o design fixa; acima dele os degraus seguem a mesma rampa
 * de [neoWidthOf], para não existir uma segunda tabela de breakpoints.
 */
fun neoHeightOf(heightDp: Dp): NeoHeight =
    when {
        heightDp < NeoLayout.compactHeightMaxExclusive -> NeoHeight.COMPACT
        heightDp < NeoLayout.mediumMinWidth -> NeoHeight.MEDIUM
        else -> NeoHeight.EXPANDED
    }

/**
 * Converte um tamanho medido em [NeoWindowInfo].
 *
 * Função **pura** — sem `Activity`, sem `Context`, sem injeção, sem Compose
 * runtime: o mesmo par de entradas devolve sempre o mesmo resultado. É essa
 * propriedade que permite testar os breakpoints por `qualifiers` do
 * Robolectric (e o teste de tabela de `NeoWindowInfoTest` é ela que exercita).
 * `calculateWindowSizeClass(activity)` não teria como ser testada aqui.
 *
 * Declarada como extensão do companion para o nome de chamada ficar
 * `NeoWindowInfo.fromSize(...)` — mas continua sendo função de topo: o
 * companion existe só como receiver nominal.
 */
fun NeoWindowInfo.Companion.fromSize(
    width: Dp,
    height: Dp,
): NeoWindowInfo =
    NeoWindowInfo(
        width = neoWidthOf(width),
        height = neoHeightOf(height),
        widthDp = width,
        heightDp = height,
    )

/**
 * Valor de [LocalNeoWindowInfo] quando ninguém fornece a informação.
 *
 * A menor janela possível: bucket compacto, margem compacta e coluna única
 * forçada. Um `@Preview` ou um teste que não monte o shell não quebra, e o
 * que ele renderiza é a geometria mais apertada — a que nenhuma tela pode
 * atravessar.
 */
val DefaultNeoWindowInfo: NeoWindowInfo = NeoWindowInfo.fromSize(width = 0.dp, height = 0.dp)

/**
 * A janela disponível, para leitura em qualquer ponto da composição.
 *
 * Injetada uma vez pelo shell (`MainActivity.setContent`), a partir da
 * `Configuration`, e não lida de um `BoxWithConstraints` — a medida é sempre
 * da **janela**, nunca do aparelho nem do constraints de um nó.
 */
val LocalNeoWindowInfo: ProvidableCompositionLocal<NeoWindowInfo> =
    staticCompositionLocalOf { DefaultNeoWindowInfo }
