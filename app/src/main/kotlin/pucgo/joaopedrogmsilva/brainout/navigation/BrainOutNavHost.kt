// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// NavHost raiz do BrainOut — declara todas as telas e o fluxo entre elas
// (Splash → Login → Home → ProjectDetail / Settings → Login).
//
// Marco E1.3 do ROADMAP. Marcos E1.6, E1.7 e E1.8 do ROADMAP adicionam
// formulários funcionais, gate do FAB por papel e persistência da
// sessão em `DataStore`.

package pucgo.joaopedrogmsilva.brainout.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import pucgo.joaopedrogmsilva.brainout.core.data.session.ActiveUserProvider
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.login.LoginScreen
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.register.RegisterScreen
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.splash.SplashScreen
import pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail.ProjectDetailScreen
import pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home.HomeScreen
import pucgo.joaopedrogmsilva.brainout.feature.settings.ui.SettingsActionType
import pucgo.joaopedrogmsilva.brainout.feature.settings.ui.SettingsScreen
import pucgo.joaopedrogmsilva.brainout.feature.tasks.navigation.TasksRoutes
import pucgo.joaopedrogmsilva.brainout.feature.tasks.navigation.dashboardGraph
import pucgo.joaopedrogmsilva.brainout.feature.tasks.navigation.tasksGraph

/**
 * Componente que registra todos os destinos do `BrainOutNavHost`.
 *
 * O [navController] é gerenciado no escopo da `MainActivity` e sobrevive à
 * recomposição — as telas individuais recebem callbacks `lambda` em vez de
 * acessarem o controller diretamente. Isso facilita a testabilidade de cada
 * tela isoladamente e evita dependência circular.
 *
 * O [activeUserProvider] é usado pelo handler de `SettingsActionType.SignOut`
 * para encerrar a sessão persistida (E1.8) — chama `signOut()` antes de
 * navegar para `Login` com a pilha limpa.
 */
@Composable
fun BrainOutNavHost(
    navController: NavHostController,
    activeUserProvider: ActiveUserProvider,
    startDestination: String = BrainOutRoutes.Splash,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        addSplashRoute(navController)
        addLoginRoute(navController)
        addRegisterRoute(navController)
        addHomeRoute(navController)
        addProjectDetailRoute(navController)
        addSettingsRoute(navController, activeUserProvider)
        // Marco E2.6 do ROADMAP — registra a tela "Tarefas" como
        // destino de navegação global (aba "Tarefas" da bottom bar).
        tasksGraph()
        // Marco E2.7 do ROADMAP — registra o Painel (Dashboard) como
        // destino de navegação global (aba "Painel" da bottom bar).
        dashboardGraph()
    }
}

private fun androidx.navigation.NavGraphBuilder.addSplashRoute(
    navController: NavHostController,
) {
    composable(route = BrainOutRoutes.Splash) {
        SplashScreen(
            onStartClicked = {
                navController.navigate(BrainOutRoutes.Login) {
                    popUpTo(BrainOutRoutes.Splash) { inclusive = true }
                }
            }
        )
    }
}

private fun androidx.navigation.NavGraphBuilder.addLoginRoute(
    navController: NavHostController,
) {
    composable(route = BrainOutRoutes.Login) {
        LoginScreen(
            onLoginSubmit = {
                navController.navigate(BrainOutRoutes.Home) {
                    popUpTo(BrainOutRoutes.Login) { inclusive = true }
                }
            },
            onCreateAccountClicked = {
                navController.navigate(BrainOutRoutes.Register)
            }
        )
    }
}

private fun androidx.navigation.NavGraphBuilder.addRegisterRoute(
    navController: NavHostController,
) {
    composable(route = BrainOutRoutes.Register) {
        RegisterScreen(
            onRegisterSubmit = {
                navController.navigate(BrainOutRoutes.Home) {
                    popUpTo(BrainOutRoutes.Login) { inclusive = true }
                }
            },
            onHaveAccountClicked = {
                navController.popBackStack()
            }
        )
    }
}

private fun androidx.navigation.NavGraphBuilder.addHomeRoute(
    navController: NavHostController,
) {
    composable(route = BrainOutRoutes.Home) {
        HomeScreen(
            onOpenProject = { projectId ->
                navController.navigate(BrainOutRoutes.projectDetail(projectId))
            },
            onOpenSettings = {
                navController.navigate(BrainOutRoutes.Settings)
            },
            onOpenTasks = {
                navController.navigate(TasksRoutes.TASKS)
            },
            onOpenDashboard = {
                navController.navigate(TasksRoutes.DASHBOARD)
            }
        )
    }
}

private fun androidx.navigation.NavGraphBuilder.addProjectDetailRoute(
    navController: NavHostController,
) {
    composable(
        route = BrainOutRoutes.ProjectDetailPattern,
        arguments = listOf(
            navArgument(BrainOutRoutes.ProjectIdArg) {
                type = NavType.StringType
            }
        )
    ) { backStackEntry ->
        val projectId = backStackEntry.arguments
            ?.getString(BrainOutRoutes.ProjectIdArg)
            .orEmpty()
        ProjectDetailScreen(
            projectId = projectId,
            onBackClicked = { navController.popBackStack() }
        )
    }
}

private fun androidx.navigation.NavGraphBuilder.addSettingsRoute(
    navController: NavHostController,
    activeUserProvider: ActiveUserProvider,
) {
    composable(route = BrainOutRoutes.Settings) {
        val lifecycleOwner = LocalLifecycleOwner.current
        SettingsScreen(
            onOptionClicked = { action ->
                when (action) {
                    SettingsActionType.SignOut -> {
                        // Limpa a sessão no DataStore (E1.8) num escopo que
                        // sobrevive à saída de Settings e navega apenas após
                        // a escrita concluir — evita sessão órfã no próximo
                        lifecycleOwner.lifecycleScope.launch {
                            activeUserProvider.signOut()
                            navController.navigate(BrainOutRoutes.Login) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                    SettingsActionType.Profile,
                    SettingsActionType.Notifications,
                    SettingsActionType.Theme -> {
                        // Sub-telas não fazem parte do E1.6 — placeholder.
                    }
                }
            }
        )
    }
}
