// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Rotas do módulo :feature:tasks — tela de listagem global de tarefas
// (tab "Tarefas" da HomeScreen) e Painel/Dashboard (E2.7).

package pucgo.joaopedrogmsilva.brainout.feature.tasks.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import pucgo.joaopedrogmsilva.brainout.feature.tasks.ui.DashboardScreen
import pucgo.joaopedrogmsilva.brainout.feature.tasks.ui.TasksScreen

/**
 * Constantes de rota das abas "Tarefas" e "Painel" da HomeScreen.
 *
 * Mantida como `object` para evitar typos no momento da navegação e
 * servir de ponto único de verdade quando outros módulos precisarem
 * referenciar o destino (ex.: testes do NavHost em `:app`).
 */
object TasksRoutes {
    /** Rota simples — a aba "Tarefas" não recebe argumentos. */
    const val TASKS: String = "tasks"

    /** Rota do Painel (Dashboard — E2.7). Também sem argumentos. */
    const val DASHBOARD: String = "dashboard"
}

/**
 * Declara o destino [TasksScreen] no [NavGraphBuilder] do host.
 *
 * Mantida como extensão para que o `:app` (ou o `ProjectsRoutes` que
 * já monta Home e ProjectDetail) possa chamá-la com `tasksGraph()`
 * sem precisar conhecer detalhes do composable.
 */
fun NavGraphBuilder.tasksGraph() {
    composable(TasksRoutes.TASKS) {
        TasksScreen()
    }
}

/**
 * Declara o destino [DashboardScreen] no [NavGraphBuilder] do host
 * (E2.7 do ROADMAP — visão consolidada acessível a partir da Home).
 */
fun NavGraphBuilder.dashboardGraph() {
    composable(TasksRoutes.DASHBOARD) {
        DashboardScreen()
    }
}
