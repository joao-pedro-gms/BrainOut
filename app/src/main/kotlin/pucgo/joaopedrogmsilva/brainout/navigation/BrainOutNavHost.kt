// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// NavHost raiz do BrainOut — declara todas as telas e o fluxo entre elas
// (Splash → Login → Home → ProjectDetail / Settings → Login).
//
// Marco E1.3 do ROADMAP. Formulários com validação (E1.6) e CRUDs reais
// (E2.x) virão em tasks subsequentes.

package pucgo.joaopedrogmsilva.brainout.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.login.LoginScreen
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.register.RegisterScreen
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.splash.SplashScreen
import pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail.ProjectDetailScreen
import pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home.HomeScreen
import pucgo.joaopedrogmsilva.brainout.feature.settings.ui.SettingsActionType
import pucgo.joaopedrogmsilva.brainout.feature.settings.ui.SettingsScreen

/**
 * Componente que registra todos os destinos do `BrainOutNavHost`.
 *
 * O [navController] é gerenciado no escopo da `MainActivity` e sobrevive à
 * recomposição — as telas individuais recebem callbacks `lambda` em vez de
 * acessarem o controller diretamente. Isso facilita a testabilidade de cada
 * tela isoladamente e evita dependência circular.
 */
@Composable
fun BrainOutNavHost(
    navController: NavHostController,
    startDestination: String = BrainOutRoutes.Splash
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        addSplashRoute(navController)
        addLoginRoute(navController)
        addRegisterRoute(navController)
        addHomeRoute(navController)
        addProjectDetailRoute(navController)
        addSettingsRoute(navController)
    }
}

private fun androidx.navigation.NavGraphBuilder.addSplashRoute(
    navController: NavHostController
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
    navController: NavHostController
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
    navController: NavHostController
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
    navController: NavHostController
) {
    composable(route = BrainOutRoutes.Home) {
        HomeScreen(
            onOpenProject = { projectId ->
                navController.navigate(BrainOutRoutes.projectDetail(projectId))
            },
            onOpenSettings = {
                navController.navigate(BrainOutRoutes.Settings)
            }
        )
    }
}

private fun androidx.navigation.NavGraphBuilder.addProjectDetailRoute(
    navController: NavHostController
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
    navController: NavHostController
) {
    composable(route = BrainOutRoutes.Settings) {
        SettingsScreen(
            onOptionClicked = { action ->
                when (action) {
                    SettingsActionType.SignOut -> {
                        // Limpa toda a pilha de navegação para que voltar
                        // da tela de Login não retorne para Settings/Home.
                        // `popUpTo(0) { inclusive = true }` é o idiom
                        // recomendado pela documentação do Navigation
                        // Compose para esvaziar o backstack completo.
                        navController.navigate(BrainOutRoutes.Login) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                    SettingsActionType.Profile,
                    SettingsActionType.Notifications,
                    SettingsActionType.Theme -> {
                        // Sub-telas não fazem parte do E1.3 — placeholder.
                    }
                }
            }
        )
    }
}
