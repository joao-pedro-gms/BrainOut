// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Tokens de geometria, tamanho, sombra e scrim do redesign neobrutalista.

package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Espaçamentos da paleta A. 4 dp é a unidade de ritmo: os valores são
 * múltiplos dela, exceto o zero.
 *
 * Aplicação prevista (`DESIGN.md` §5): 8 entre ícone e rótulo, 12 entre
 * controles próximos, 16 de padding de ficha, 24 entre seções e 32 entre
 * grandes grupos. Não existe token "quase": se um layout precisa de 6 dp,
 * ele precisa de um motivo — o valor não entra na escala por conveniência.
 */
object NeoSpacing {
    val none: Dp = 0.dp
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val mdPlus: Dp = 20.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 40.dp
    val xxxl: Dp = 48.dp
    val huge: Dp = 64.dp

    /** Escala completa, na ordem, para testes e para a galeria. */
    val scale: List<Dp> = listOf(none, xxs, xs, sm, md, mdPlus, lg, xl, xxl, xxxl, huge)
}

/**
 * Raios. Barras e gráficos têm canto reto (0); chips e campos usam 4;
 * botões, cards e menus usam 8; diálogos e sheets usam 16. Círculo é
 * reservado a avatar e radio.
 */
object NeoRadii {
    val none: Dp = 0.dp
    val chip: Dp = 4.dp
    val field: Dp = 4.dp
    val button: Dp = 8.dp
    val card: Dp = 8.dp
    val menu: Dp = 8.dp
    val modal: Dp = 16.dp
}

/** Larguras de borda: 1 em divisores e chips informativos, 2 em contornos, 3 no foco. */
object NeoBorders {
    val subtle: Dp = 1.dp
    val default: Dp = 2.dp
    val focus: Dp = 3.dp
    val focusGap: Dp = 2.dp
}

/**
 * Sombra rígida: impressão deslocada, **nunca** iluminação. Blur e spread
 * são sempre zero — é isso que separa esta identidade de uma sombra
 * Material. Reservar até o offset embaixo/à direita da caixa para o
 * desenho, senão a face cobre o volume (ou o volume é cortado por um
 * `clip` de pai).
 */
data class NeoShadow(val x: Dp, val y: Dp, val blur: Dp, val spread: Dp) {
    /** Face deslocada durante o press: some o offset, mantendo o desenho. */
    fun pressed(): NeoShadow = copy(x = NeoSpacing.none, y = NeoSpacing.none)
}

object NeoShadows {
    val none = NeoShadow(0.dp, 0.dp, 0.dp, 0.dp)
    val small = NeoShadow(2.dp, 2.dp, 0.dp, 0.dp)
    val medium = NeoShadow(4.dp, 4.dp, 0.dp, 0.dp)
    val display = NeoShadow(6.dp, 6.dp, 0.dp, 0.dp)

    /** Valor de deslocamento da face no press, conforme `motion.pressFaceOffset`. */
    val pressFaceOffset = 2.dp
}

/**
 * Tamanhos mínimos de controle, todos vindos da matriz de acessibilidade:
 * 48 dp de alvo, 52 dp de botão, 56 dp de campo e de FAB, 64 dp de linha de
 * referência (que pode crescer, nunca encolher).
 */
object NeoSizes {
    val touchMin: Dp = 48.dp
    val touchGap: Dp = 8.dp
    val buttonMinHeight: Dp = 52.dp
    val fieldMinHeight: Dp = 56.dp
    val fabMinHeight: Dp = 56.dp
    val rowMinHeight: Dp = 64.dp
    val icon: Dp = 24.dp
    val iconMeta: Dp = 20.dp
    val iconEmpty: Dp = 32.dp
    val badgeMinHeight: Dp = 28.dp
    val navMinHeight: Dp = 80.dp
}

/**
 * Breakpoints e margens de layout. A medida é a **janela disponível**, não
 * o modelo do aparelho: qualquer aparelho pode ser compacto em paisagem com
 * teclado aberto.
 */
object NeoLayout {
    val mediumMinWidth: Dp = 600.dp
    val expandedMinWidth: Dp = 840.dp
    val largeMinWidth: Dp = 1200.dp
    val compactHeightMaxExclusive: Dp = 480.dp
    val compactMargin: Dp = 16.dp
    val mediumMargin: Dp = 24.dp
    val expandedMargin: Dp = 32.dp
    val formMaxWidth: Dp = 480.dp
    val contentMaxWidth: Dp = 1200.dp
}

/** Scrim de modal: preto a 56 %, calculado sobre o popup, não sobre a tela. */
object NeoScrim {
    const val alpha: Float = 0.56f
}
