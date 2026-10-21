// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import pucgo.joaopedrogmsilva.brainout.core.ui.R
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoNavigationBar
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoNavigationItem
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoWindowProvider
import pucgo.joaopedrogmsilva.brainout.feature.tasks.navigation.TasksRoutes

internal val mainTabs =
    listOf(
        BrainOutRoutes.ProjectsGraph,
        BrainOutRoutes.TasksGraph,
        BrainOutRoutes.DashboardGraph,
        BrainOutRoutes.SettingsGraph,
    )

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun BrainOutShell(
    navController: NavHostController,
    content: @Composable (Modifier) -> Unit,
) {
    val entry by navController.currentBackStackEntryAsState()
    val selected =
        entry
            ?.destination
            ?.hierarchy
            ?.firstOrNull { it.route in mainTabs }
            ?.route
    val keyboardVisible = WindowInsets.isImeVisible
    NeoWindowProvider {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (selected != null && !keyboardVisible) {
                    NeoNavigationBar(
                        items = mainNavigationItems(),
                        selectedId = selected,
                        onSelect = { navController.selectMainTab(it) },
                    )
                }
            },
        ) { padding ->
            content(Modifier.padding(padding).consumeWindowInsets(padding))
        }
    }
}

@Composable
private fun mainNavigationItems(): List<NeoNavigationItem> =
    listOf(
        NeoNavigationItem(
            BrainOutRoutes.ProjectsGraph,
            stringResource(R.string.neo_tab_projects),
            R.drawable.neo_ic_folder,
        ),
        NeoNavigationItem(
            BrainOutRoutes.TasksGraph,
            stringResource(R.string.neo_tab_tasks),
            R.drawable.neo_ic_assignment,
        ),
        NeoNavigationItem(
            BrainOutRoutes.DashboardGraph,
            stringResource(R.string.neo_tab_dashboard),
            R.drawable.neo_ic_bar_chart,
        ),
        NeoNavigationItem(
            BrainOutRoutes.SettingsGraph,
            stringResource(R.string.neo_tab_settings),
            R.drawable.neo_ic_settings,
        ),
    )

internal fun NavHostController.selectMainTab(route: String) {
    require(route in mainTabs)
    if (currentDestination?.hierarchy?.any { it.route == route } == true) return
    navigate(route) {
        popUpTo(BrainOutRoutes.Home) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Remove inclusive os estados estacionados de abas de uma sessão anterior. */
internal fun NavHostController.clearMainBackStacks() {
    mainTabs.forEach { clearBackStack(it) }
}

/** Topologia compartilhada pelos destinos reais e testes do shell. */
internal fun androidx.navigation.NavGraphBuilder.mainDestinations(
    projects: androidx.navigation.NavGraphBuilder.() -> Unit,
    tasks: androidx.navigation.NavGraphBuilder.() -> Unit,
    dashboard: androidx.navigation.NavGraphBuilder.() -> Unit,
    settings: androidx.navigation.NavGraphBuilder.() -> Unit,
) {
    navigation(startDestination = BrainOutRoutes.ProjectsGraph, route = BrainOutRoutes.MainGraph) {
        navigation(startDestination = BrainOutRoutes.Home, route = BrainOutRoutes.ProjectsGraph, builder = projects)
        navigation(startDestination = TasksRoutes.TASKS, route = BrainOutRoutes.TasksGraph, builder = tasks)
        navigation(startDestination = TasksRoutes.DASHBOARD, route = BrainOutRoutes.DashboardGraph, builder = dashboard)
        navigation(startDestination = BrainOutRoutes.Settings, route = BrainOutRoutes.SettingsGraph, builder = settings)
    }
}
