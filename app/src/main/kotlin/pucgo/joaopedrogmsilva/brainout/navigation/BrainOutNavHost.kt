// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// NavHost raiz do BrainOut — declara todas as telas e o fluxo entre elas
// (Splash → Login → Home → ProjectDetail / Settings → Login).
//
// Marco E1.3 do ROADMAP. Marcos E1.6, E1.7 e E1.8 do ROADMAP adicionam
// formulários funcionais, gate do FAB por papel e persistência da
// sessão em `DataStore`.

package pucgo.joaopedrogmsilva.brainout.navigation

import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import pucgo.joaopedrogmsilva.brainout.core.data.session.ActiveUserProvider
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoMotion
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.login.LoginScreen
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.register.RegisterScreen
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.splash.SplashScreen
import pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home.HomeScreen
import pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail.ProjectDetailScreen
import pucgo.joaopedrogmsilva.brainout.feature.settings.ui.BackupSection
import pucgo.joaopedrogmsilva.brainout.feature.settings.ui.SettingsScreen
import pucgo.joaopedrogmsilva.brainout.feature.tasks.navigation.dashboardGraph
import pucgo.joaopedrogmsilva.brainout.feature.tasks.navigation.tasksGraph
import pucgo.joaopedrogmsilva.brainout.notifications.NotificationPermissionRequest
import pucgo.joaopedrogmsilva.brainout.notifications.NotificationPermissionStore

private const val NAVIGATION_SLIDE_DIVISOR = 12

private const val TAG = "BrainOut:Nav"

/**
 * Componente que registra todos os destinos do `BrainOutNavHost`.
 *
 * O [navController] é gerenciado no escopo da `MainActivity` e sobrevive à
 * recomposição — as telas individuais recebem callbacks `lambda` em vez de
 * acessarem o controller diretamente. Isso facilita a testabilidade de cada
 * tela isoladamente e evita dependência circular.
 *
 * O [activeUserProvider] é usado pelo handler de "Sair" para encerrar
 * a sessão persistida (E1.8) — chama `signOut()` antes de navegar
 * para `Login` com a pilha limpa.
 */
@Composable
fun BrainOutNavHost(
    navController: NavHostController,
    activeUserProvider: ActiveUserProvider,
    notificationPermissionStore: NotificationPermissionStore,
    startDestination: String = BrainOutRoutes.Splash,
) {
    BrainOutShell(navController) { modifier ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier,
            enterTransition = {
                fadeIn(tween(NeoMotion.navigation)) +
                    slideInHorizontally(tween(NeoMotion.navigation)) { it / NAVIGATION_SLIDE_DIVISOR }
            },
            exitTransition = { fadeOut(tween(NeoMotion.navigation)) },
            popEnterTransition = { fadeIn(tween(NeoMotion.navigation)) },
            popExitTransition = {
                fadeOut(tween(NeoMotion.navigation)) +
                    slideOutHorizontally(tween(NeoMotion.navigation)) { it / NAVIGATION_SLIDE_DIVISOR }
            },
        ) {
            addSplashRoute(navController)
            addLoginRoute(navController)
            addRegisterRoute(navController)
            mainDestinations(
                projects = {
                    addHomeRoute(navController, notificationPermissionStore)
                    addProjectDetailRoute(navController)
                },
                tasks = { tasksGraph(navController) },
                dashboard = { dashboardGraph(navController) },
                settings = { addSettingsRoute(navController, activeUserProvider) },
            )
        }
    }
}

private fun NavGraphBuilder.addSplashRoute(navController: NavHostController) {
    composable(route = BrainOutRoutes.Splash) {
        SplashScreen(
            onStartClicked = {
                Log.d(TAG, "Navegando: Splash -> Login")
                navController.navigate(BrainOutRoutes.Login) {
                    popUpTo(BrainOutRoutes.Splash) { inclusive = true }
                }
            },
        )
    }
}

private fun NavGraphBuilder.addLoginRoute(navController: NavHostController) {
    composable(route = BrainOutRoutes.Login) {
        LoginScreen(
            onLoginSuccess = {
                Log.d(TAG, "Navegando: Login -> Home")
                navController.navigate(BrainOutRoutes.MainGraph) {
                    popUpTo(0) { inclusive = true }
                }
            },
            onCreateAccountClicked = {
                Log.d(TAG, "Navegando: Login -> Register")
                navController.navigate(BrainOutRoutes.Register)
            },
        )
    }
}

private fun NavGraphBuilder.addRegisterRoute(navController: NavHostController) {
    composable(route = BrainOutRoutes.Register) {
        RegisterScreen(
            onRegisterSuccess = {
                Log.d(TAG, "Navegando: Register -> Home")
                navController.navigate(BrainOutRoutes.MainGraph) {
                    popUpTo(0) { inclusive = true }
                }
            },
            onHaveAccountClicked = {
                Log.d(TAG, "Navegando: Register -> PopBackStack")
                navController.popBackStack()
            },
        )
    }
}

private fun NavGraphBuilder.addHomeRoute(
    navController: NavHostController,
    notificationPermissionStore: NotificationPermissionStore,
) {
    composable(route = BrainOutRoutes.Home) {
        HomeScreen(
            onOpenProject = { projectId ->
                Log.d(TAG, "Navegando: Home -> ProjectDetail($projectId)")
                navController.navigate(BrainOutRoutes.projectDetail(projectId))
            },
        )
        // DEF-45 — o pedido da permissão vive **dentro** da Home: na
        // raiz da Activity ele disparava na primeira composição da
        // árvore, que em instalação limpa é a tela de splash — o
        // diálogo do sistema aparecia antes do login.
        NotificationPermissionRequest(permissionStore = notificationPermissionStore)
    }
}

private fun NavGraphBuilder.addProjectDetailRoute(navController: NavHostController) {
    composable(
        route = BrainOutRoutes.ProjectDetailPattern,
        arguments =
            listOf(
                navArgument(BrainOutRoutes.ProjectIdArg) {
                    type = NavType.StringType
                },
            ),
    ) { backStackEntry ->
        val projectId =
            backStackEntry.arguments
                ?.getString(BrainOutRoutes.ProjectIdArg)
                .orEmpty()
        ProjectDetailScreen(
            projectId = projectId,
            onBackClicked = {
                Log.d(TAG, "Navegando: ProjectDetail -> PopBackStack")
                navController.popBackStack()
            },
        )
    }
}

private fun NavGraphBuilder.addSettingsRoute(
    navController: NavHostController,
    activeUserProvider: ActiveUserProvider,
) {
    composable(route = BrainOutRoutes.Settings) {
        val lifecycleOwner = LocalLifecycleOwner.current
        SettingsScreen(
            onSignOut = {
                Log.d(TAG, "Navegando: Settings -> SignOut (Limpando sessão)")
                // Limpa a sessão no DataStore (E1.8) num escopo que
                // sobrevive à saída de Settings e navega apenas após
                // a escrita concluir — evita sessão órfã no próximo
                // lançamento do app.
                lifecycleOwner.lifecycleScope.launch {
                    activeUserProvider.signOut()
                    navController.clearMainBackStacks()
                    navController.navigate(BrainOutRoutes.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            },
            backupContent = {
                BackupSection(
                    onRestoreCompleted = {
                        navController.navigate(BrainOutRoutes.MainGraph) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            },
        )
    }
}
