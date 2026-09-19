// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Rotas do módulo :feature:tasks — tela de listagem global de tarefas
// (tab "Tarefas" da HomeScreen).

package pucgo.joaopedrogmsilva.brainout.feature.tasks.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import pucgo.joaopedrogmsilva.brainout.feature.tasks.ui.TasksScreen

/**
 * Constantes de rota da aba "Tarefas" da HomeScreen.
 *
 * Mantida como `object` para evitar typos no momento da navegação e
 * servir de ponto único de verdade quando outros módulos precisarem
 * referenciar o destino (ex.: testes do NavHost em `:app`).
 */
object TasksRoutes {
    /** Rota simples — a aba "Tarefas" não recebe argumentos. */
    const val TASKS: String = "tasks"
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
