// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Testes das rotas de :feature:tasks — a única feature que expõe
// extensões de grafo (`tasksGraph()` e `dashboardGraph()`).
//
// O grafo é montado de verdade, sem a dependência `navigation-testing`:
// `NavController.createGraph` e o `iterator()` do `NavGraph` são
// públicos, e é o `iterator()` que deixa o teste alcançar os destinos
// aninhados — o `findNode` sozinho só busca um nível.

package pucgo.joaopedrogmsilva.brainout.feature.tasks.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.createGraph
import androidx.navigation.compose.ComposeNavigator
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifica as rotas que a feature expõe ao `:app`.
 *
 * O `NavHost` raiz do `:app` registra `tasksGraph()` e `dashboardGraph()`;
 * se uma delas deixar de declarar o seu destino, ou passar a declarar o
 * destino errado, a aba correspondente some do app sem nenhum aviso — o
 * `NavHost` aceita o grafo e a navegação devolve "destino não encontrado".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class TasksRoutesTest {
    private lateinit var controller: NavHostController

    @Before
    fun setUp() {
        // O `rememberNavController` do navigation-compose registra esses
        // navegadores; fora do Compose, quem monta o controlador é o
        // próprio teste.
        controller =
            NavHostController(ApplicationProvider.getApplicationContext()).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
    }

    @Test
    fun `as duas rotas da feature ficam registradas no grafo`() {
        val routes = tasksGraph().allRoutes()

        assertThat(routes).containsAtLeast(TasksRoutes.TASKS, TasksRoutes.DASHBOARD)
    }

    @Test
    fun `o grafo nao registra a mesma rota duas vezes`() {
        val routes = tasksGraph().allRoutes()

        assertThat(routes).containsNoDuplicates()
    }

    @Test
    fun `a rota inicial do grafo e a aba de tarefas`() {
        val graph = tasksGraph()

        assertThat(graph.findNode(TasksRoutes.TASKS)?.route).isEqualTo(TasksRoutes.TASKS)
    }

    /** O grafo raiz com as duas extensões da feature registradas. */
    private fun tasksGraph(): NavGraph =
        controller.createGraph(startDestination = TasksRoutes.TASKS) {
            tasksGraph(controller)
            dashboardGraph(controller)
        }
}

/**
 * Todas as rotas do grafo, incluindo as de dentro de grafos aninhados.
 *
 * O `NavGraph` implementa `Iterable<NavDestination>`: o `iterator()` é
 * público e devolve os filhos diretos, e um filho que é `NavGraph` tem os
 * seus. O `findNode` não serve aqui — ele busca só um nível.
 */
private fun NavDestination.allRoutes(): List<String> = buildList {
    route?.let { add(it) }
    if (this@allRoutes is NavGraph) {
        this@allRoutes.forEach { addAll(it.allRoutes()) }
    }
}
