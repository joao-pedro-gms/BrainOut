// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// NB-06: wrapper Material aditivo do redesign neobrutalista.

package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Estado escuro **já resolvido** pelo app (plano §3.2.2, NB-06).
 *
 * A raiz (`MainActivity`) lê a preferência persistida
 * (`ThemePreferencesRepository`), consulta a configuração do sistema
 * **uma única vez** e publica o resultado aqui. Os subtrees novos leem
 * este local em vez de chamar `isSystemInDarkTheme()` — se cada rota
 * recalcular, a escolha manual se perde (sistema escuro + escolha
 * manual clara voltaria a ficar escuro por rota).
 *
 * Default claro, no mesmo espírito de [LocalNeoColors]: previews e
 * testes que não montam a raiz do app enxergam o tema claro sem
 * precisar providenciar a composição inteira.
 */
val LocalResolvedDarkTheme = staticCompositionLocalOf { false }

/**
 * Escolhe o conjunto de papéis novos para o modo resolvido.
 *
 * Função pura e determinística — existe desacoplada de
 * [BrainOutNeoTheme] para ser testável sem montar uma Composition
 * (o módulo `:core:ui` ainda não tem infra de Compose UI test; ver
 * `ThemeSelectionTest`).
 */
internal fun resolveNeoColors(darkTheme: Boolean): NeoColors =
    if (darkTheme) NeoColors.Dark else NeoColors.Light

/**
 * Tipografia Material do wrapper neo: exatamente os quinze estilos do
 * contrato `DESIGN.md` §4 (NB-05), sem nenhum slot no default do
 * Material — `MaterialTheme.typography.bodyLarge` dentro de uma rota
 * migrada já é Public Sans 16/24.
 */
internal val NeoMaterialTypography: Typography = Typography(
    displayLarge = NeoTypography.displayLarge,
    displayMedium = NeoTypography.displayMedium,
    displaySmall = NeoTypography.displaySmall,
    headlineLarge = NeoTypography.headlineLarge,
    headlineMedium = NeoTypography.headlineMedium,
    headlineSmall = NeoTypography.headlineSmall,
    titleLarge = NeoTypography.titleLarge,
    titleMedium = NeoTypography.titleMedium,
    titleSmall = NeoTypography.titleSmall,
    bodyLarge = NeoTypography.bodyLarge,
    bodyMedium = NeoTypography.bodyMedium,
    bodySmall = NeoTypography.bodySmall,
    labelLarge = NeoTypography.labelLarge,
    labelMedium = NeoTypography.labelMedium,
    labelSmall = NeoTypography.labelSmall,
)

/**
 * Shapes Material do wrapper neo, pelos raios do `DESIGN.md` §5:
 * extraSmall 4 (chips/campos), small 4, medium 8 (botões), large 8
 * (cards/menus), extraLarge 16 (diálogos/sheets). Os valores vêm de
 * [NeoRadii] — não há raio literal aqui.
 */
internal val NeoMaterialShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(NeoRadii.chip),
    small = RoundedCornerShape(NeoRadii.chip),
    medium = RoundedCornerShape(NeoRadii.button),
    large = RoundedCornerShape(NeoRadii.card),
    extraLarge = RoundedCornerShape(NeoRadii.modal),
)

/**
 * Tema Material 3 do redesign neobrutalista — **aditivo** (NB-06).
 *
 * O app mantém um único ponto de decisão de tema: a raiz resolve o
 * modo (System/Light/Dark persistido, com precedência) e publica o
 * resultado em [LocalResolvedDarkTheme]. Este wrapper consome esse
 * valor, publica os papéis neo em [LocalNeoColors] e monta o
 * `MaterialTheme` com os tokens novos (cor, tipografia e shapes).
 *
 * Nada aqui consulta `isSystemInDarkTheme()` — quem chamar sem o
 * parâmetro herda o modo resolvido da raiz.
 *
 * **Aditivo, sem ativação global:** só as rotas listadas em
 * `migratedNeoRoutes` (raiz do app) envolvem o conteúdo neste tema;
 * as demais continuam dentro de [BrainOutTheme] (paleta roxa
 * original) — nenhuma tela é redesenhada antecipadamente. NB-32 faz
 * a ativação global e a remoção dos dois wrappers.
 *
 * @param darkTheme estado escuro efetivo, já resolvido pelo app.
 *  O default lê [LocalResolvedDarkTheme]; previews/testes podem
 *  passar o valor explicitamente.
 * @param content conteúdo da rota migrada.
 */
@Composable
fun BrainOutNeoTheme(
    darkTheme: Boolean = LocalResolvedDarkTheme.current,
    content: @Composable () -> Unit,
) {
    val colors = resolveNeoColors(darkTheme)

    CompositionLocalProvider(LocalNeoColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(darkTheme),
            typography = NeoMaterialTypography,
            shapes = NeoMaterialShapes,
            content = content,
        )
    }
}
