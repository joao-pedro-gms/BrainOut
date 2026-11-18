// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.feature.tasks.navigation.TasksRoutes

@Composable
private fun ShellLayoutContent() {
    val navController = rememberNavController()
    BrainOutTheme {
        BrainOutShell(navController) { modifier ->
            NavHost(navController, BrainOutRoutes.MainGraph, modifier) {
                mainDestinations(
                    projects = { composable(BrainOutRoutes.Home) { Text("Projetos") } },
                    tasks = { composable(TasksRoutes.TASKS) { Text("Tarefas") } },
                    dashboard = { composable(TasksRoutes.DASHBOARD) { Text("Painel") } },
                    settings = { composable(BrainOutRoutes.Settings) { Text("Configurações") } },
                )
            }
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w599dp-h800dp")
class BrainOutShellCompactResponsiveTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `abaixo do breakpoint mostra a barra inferior`() {
        compose.setContent { ShellLayoutContent() }

        val bounds = compose.onNodeWithTag("main_navigation").fetchSemanticsNode().boundsInRoot
        assertThat(bounds.width).isGreaterThan(bounds.height)
        compose.onNodeWithTag("main_tab_${BrainOutRoutes.ProjectsGraph}").assertIsSelected()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w600dp-h800dp")
class BrainOutShellWideResponsiveTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `no breakpoint mostra rail e preserva selecao das abas`() {
        compose.setContent { ShellLayoutContent() }

        val bounds = compose.onNodeWithTag("main_navigation").fetchSemanticsNode().boundsInRoot
        assertThat(bounds.height).isGreaterThan(bounds.width)
        compose.onNodeWithTag("main_tab_${BrainOutRoutes.ProjectsGraph}").assertIsSelected()
        compose.onNodeWithTag("main_tab_${BrainOutRoutes.TasksGraph}").assertExists()
    }
}
