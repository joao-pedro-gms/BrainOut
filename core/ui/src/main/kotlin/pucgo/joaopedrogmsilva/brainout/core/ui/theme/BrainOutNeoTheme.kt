// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Ativação do design system neobrutalista (NB-06/NB-32): o tema que o app
// renderiza passa a ser construído a partir de NeoColors/NeoTypography.

package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Os quatro valores que definem uma instância do tema Neo, resolvidos
 * juntos.
 *
 * Existe para que a ligação entre a especificação e a árvore de composição
 * seja verificável sem montar Compose: [BrainOutNeoTheme] injeta
 * exatamente este objeto, e o teste do tema compara a mesma instância
 * que o app usa. Se o wrapper passar a derivar um valor por fora, o teste
 * reprova — que é o ponto.
 *
 * [colorScheme] vem de [NeoColors.toMaterialColorScheme], que mapeia os
 * papéis semânticos nos slots do Material 3; [typography] vem dos quinze
 * estilos de [NeoTypography]; [shapes] vem de [NeoRadii].
 */
data class NeoThemeSpec(
    val neoColors: NeoColors,
    val colorScheme: ColorScheme,
    val typography: Typography,
    val shapes: Shapes,
)

/**
 * Tema Neo claro, completo e construído uma única vez.
 *
 * Singleton de propósito: o `ColorScheme` do Material é um objeto
 * immutable reaproveitado por toda a árvore, e comparar a identidade da
 * instância só significa alguma coisa se a resolução devolver sempre o
 * mesmo objeto em vez de um `ColorScheme` novo a cada recomposição.
 */
val NeoLightTheme: NeoThemeSpec =
    NeoThemeSpec(
        neoColors = NeoColors.Light,
        colorScheme = NeoColors.Light.toMaterialColorScheme(dark = false),
        typography = NeoMaterialTypography,
        shapes = NeoShapes,
    )

/** Tema Neo escuro, completo e construído uma única vez. */
val NeoDarkTheme: NeoThemeSpec =
    NeoThemeSpec(
        neoColors = NeoColors.Dark,
        colorScheme = NeoColors.Dark.toMaterialColorScheme(dark = true),
        typography = NeoMaterialTypography,
        shapes = NeoShapes,
    )

/**
 * Resolve o tema Neo a partir da única flag de modo escuro.
 *
 * Função pura, determinística e sem `isSystemInDarkTheme()`: quem decide o
 * modo é o shell (hoje `MainActivity`, que ainda delega ao tema); aqui
 * só há o mapeamento `true → escuro`, o mesmo desenho de
 * `resolveBrainOutStaticColorScheme` que `ThemeSelectionTest` já cobre
 * para a paleta legada.
 *
 * Nenhum token é resolvido "no meio": a paleta vem inteira de
 * `NeoColors.Light`/`NeoColors.Dark`, a tipografia inteira de
 * `NeoTypography` e os raios inteiros de `NeoRadii`. Nenhum valor literal
 * de cor, sp ou dp é decidido aqui.
 */
fun resolveNeoThemeSpec(darkTheme: Boolean): NeoThemeSpec = if (darkTheme) NeoDarkTheme else NeoLightTheme

/**
 * Tema raiz na identidade Neo (paleta A, `DESIGN.md` §3).
 *
 * Faz três coisas ao mesmo tempo, que é o que torna a identidade
 * verificável de ponta a ponta:
 *
 *  1. monta o `ColorScheme` a partir de [NeoColors] (via
 *     [toMaterialColorScheme]), então nenhum canto da árvore recebe o
 *     roxo padrão do Material (`#6750A4`) nem `surfaceTint` residual;
 *  2. publica [LocalNeoColors] para os componentes Neo lerem papéis
 *     (`action.background`, `text.muted`, `warning.container`) sem passar
 *     a paleta por parâmetro e sem duplicar `Color` literal;
 *  3. entrega [NeoMaterialTypography] e [NeoShapes], o que faz as cinco
 *     fontes empacotadas em `res/font` e os raios 4/4/8/8/16 dp
 *     chegarem à tela.
 *
 * `darkTheme` não tem default aqui: quem chama é o shell e decide
 * explicitamente. Isso mantém aberta a preferência manual de tema (NB-06,
 * item 2) sem recalcular `isSystemInDarkTheme()` dentro do subtree — hoje
 * o valor vem do `BrainOutTheme` legado, que o chama.
 */
@Composable
fun BrainOutNeoTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val spec = resolveNeoThemeSpec(darkTheme)

    CompositionLocalProvider(LocalNeoColors provides spec.neoColors) {
        MaterialTheme(
            colorScheme = spec.colorScheme,
            typography = spec.typography,
            shapes = spec.shapes,
            content = content,
        )
    }
}
