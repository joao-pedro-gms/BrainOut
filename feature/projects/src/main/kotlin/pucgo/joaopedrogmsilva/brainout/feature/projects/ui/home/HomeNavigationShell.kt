// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Shell de navegação da Home (D-02): `NavigationSuiteScaffold` deixa o
// AndroidX escolher entre `NavigationBar` (abaixo de 600dp) e
// `NavigationRail` (a partir de 600dp), em vez de a Home decidir isso à
// mão — que era o defeito: o antigo `HomeBottomBar` renderizava
// `NavigationBar` em qualquer largura, e o token
// `NeoSizes.navMinHeight` (80dp) nunca era usado.
//
// **Navegação não muda.** Os 4 destinos continuam sendo `onClick`: só
// [HomeTab.Projects] é aba interna da Home; [HomeTab.Tasks],
// [HomeTab.Dashboard] e [HomeTab.Settings] disparam o callback externo
// que o `BrainOutNavHost` converte em `navigate(...)`. Trocar o
// componente não troca o destino.
//
// **Sobre "não aninhar dois Scaffolds" (D-02):** vale registrar o que a
// API real é. `NavigationSuiteScaffold` 1.3.1 é um `Surface` + `Layout`
// próprio (`NavigationSuiteScaffoldLayout`) que posiciona a suíte em
// baixo (bar) ou à esquerda (rail) — ele **não** tem `topBar`,
// `floatingActionButton` nem `snackbarHost`. A composição correta é o
// `Scaffold` da Home *dentro* do suite: o suite cuida da navegação e do
// espaço dela, o `Scaffold` cuida da top bar, do FAB e do padding.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScope
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource

/**
 * Casca de navegação da Home: escolhe a suíte pelo tamanho da janela e
 * delega o corpo ao [content].
 *
 * @param currentTab aba ativa da Home.
 * @param onSelectTab disparado ao tocar em qualquer um dos 4 destinos.
 * @param content corpo da tela — um `Scaffold` que cuida do `topBar`,
 *  do FAB e do `innerPadding`. O [NavigationSuiteScaffold] não passa
 *  padding nenhum: ele só reserva o espaço da barra/rail e consome os
 *  insets próprios do componente, então o `Scaffold` interno continua
 *  o dono do padding.
 */
@OptIn(
    ExperimentalMaterial3AdaptiveNavigationSuiteApi::class,
    ExperimentalMaterial3AdaptiveApi::class,
)
@Composable
internal fun HomeNavigationShell(
    currentTab: HomeTab,
    onSelectTab: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = { homeNavigationItems(currentTab, onSelectTab) },
        // `calculateFromAdaptiveInfo` é a decisão documentada do
        // AndroidX: bar abaixo de 600dp, rail a partir de 600dp, e bar
        // de novo quando a altura é compacta (paisagem) — altura vale
        // mais que largura, porque um rail espremido em 400dp de altura
        // rouba altura do conteúdo.
        layoutType =
            NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(
                currentWindowAdaptiveInfo(),
            ),
        // D-02 — o visual neobrutalista fixa `surface` no componente de
        // navegação. `NavigationSuiteDefaults.colors()` usaria
        // `NavigationBarDefaults.containerColor` (= `surfaceContainer`),
        // um cinza tonal que muda a identidade da barra.
        navigationSuiteColors =
            NavigationSuiteDefaults.colors(
                navigationBarContainerColor = MaterialTheme.colorScheme.surface,
                navigationRailContainerColor = MaterialTheme.colorScheme.surface,
            ),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        content()
    }
}

/**
 * Os 4 destinos de [HomeTab] como itens da suíte.
 *
 * `NavigationSuiteScope.item` é apenas um coletor: esta função também
 * não é `@Composable`, porque o `navigationSuiteItems` do
 * [NavigationSuiteScaffold] é um lambda **não**-composable que grava as
 * lambdas ([androidx.compose.material3.Icon] e [Text]) para serem
 * chamadas depois, já dentro da barra, do rail ou do drawer que a
 * suíte escolher. Quem decide bar/rail/drawer é o suite, então o mesmo
 * código serve para os três.
 */
@OptIn(ExperimentalMaterial3AdaptiveNavigationSuiteApi::class)
internal fun NavigationSuiteScope.homeNavigationItems(
    currentTab: HomeTab,
    onSelectTab: (HomeTab) -> Unit,
) {
    HomeTab.entries.forEach { tab ->
        item(
            selected = tab == currentTab,
            onClick = { onSelectTab(tab) },
            icon = {
                Icon(
                    imageVector = tab.icon(),
                    contentDescription = null,
                )
            },
            label = {
                Text(
                    text = stringResource(id = tab.labelRes),
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            alwaysShowLabel = true,
            // AC-1.4 — os itens precisam continuar alcançáveis por
            // test tag. Antes da Fase 1 nenhum destino tinha tag (nem o
            // `NavigationBarItem` antigo), então a navegação não era
            // verificável sem device; a tag é o que a torna testável.
            modifier = Modifier.testTag(tab.testTag()),
        )
    }
}

/**
 * Test tag do destino — uma por aba.
 *
 * Vive ao lado de [HomeTab] porque é a chave que liga a navegação ao
 * contrato de testes (AC-1.4): o nome deriva de [HomeTab.name], então
 * uma aba nova não pode quebrar o teste que a procura.
 */
internal fun HomeTab.testTag(): String = "${HomeTestTags.NAV_SUITE_PREFIX}_${name.lowercase()}"

internal fun HomeTab.icon(): ImageVector =
    when (this) {
        HomeTab.Projects -> Icons.Outlined.Folder
        HomeTab.Tasks -> Icons.AutoMirrored.Outlined.Assignment
        HomeTab.Dashboard -> Icons.Outlined.BarChart
        HomeTab.Settings -> Icons.Outlined.Settings
    }
