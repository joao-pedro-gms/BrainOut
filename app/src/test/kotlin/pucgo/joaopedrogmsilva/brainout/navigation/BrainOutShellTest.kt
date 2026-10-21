// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.navigation

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.feature.tasks.navigation.TasksRoutes

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "pt-rBR")
class BrainOutShellTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var nav: NavHostController

    private fun render(start: String = BrainOutRoutes.MainGraph) {
        compose.setContent {
            nav = rememberNavController()
            BrainOutTheme {
                BrainOutShell(nav) { modifier ->
                    NavHost(nav, start, modifier) {
                        composable(BrainOutRoutes.Login) { Text("Login") }
                        composable(BrainOutRoutes.Register) { Text("Cadastro") }
                        composable(BrainOutRoutes.Splash) { Text("Abertura") }
                        mainDestinations(
                            projects = {
                                composable(BrainOutRoutes.Home) { Text("Lista de projetos") }
                                composable(
                                    BrainOutRoutes.ProjectDetailPattern,
                                    arguments =
                                        listOf(
                                            navArgument(BrainOutRoutes.ProjectIdArg) {
                                                type =
                                                    NavType.StringType
                                            },
                                        ),
                                ) {
                                    var query by rememberSaveable { mutableStateOf("") }
                                    OutlinedTextField(query, { query = it }, Modifier.testTag("detail_query"))
                                }
                            },
                            tasks = { composable(TasksRoutes.TASKS) { Text("Lista de tarefas") } },
                            dashboard = { composable(TasksRoutes.DASHBOARD) { Text("Resumo") } },
                            settings = { composable(BrainOutRoutes.Settings) { Text("Preferências") } },
                        )
                    }
                }
            }
        }
    }

    @Test fun `barra acompanha os quatro destinos e seleciona projetos no detalhe`() {
        render()
        mainTabs.forEach { route ->
            compose.onNodeWithTag("main_tab_$route").performClick().assertIsSelected()
            compose.onNodeWithTag("main_navigation").assertExists()
        }
        compose.onNodeWithTag("main_tab_${BrainOutRoutes.ProjectsGraph}").performClick()
        compose.runOnIdle { nav.navigate(BrainOutRoutes.projectDetail("p1")) }
        compose.onNodeWithTag("detail_query").assertExists()
        compose.onNodeWithTag("main_tab_${BrainOutRoutes.ProjectsGraph}").assertIsSelected()
    }

    @Test fun `alternar abas restaura detalhe e formulario sem duplicar destino`() {
        render()
        compose.runOnIdle { nav.navigate(BrainOutRoutes.projectDetail("p1")) }
        compose.onNodeWithTag("detail_query").performTextInput("Contexto salvo")
        // IME fica fechado neste teste JVM; visibilidade real é verificada no emulador.
        compose.onNodeWithTag("main_tab_${BrainOutRoutes.TasksGraph}").performClick()
        compose.onNodeWithTag("main_tab_${BrainOutRoutes.ProjectsGraph}").performClick()
        compose.onNodeWithTag("detail_query").assertTextEquals("Contexto salvo")
        lateinit var entryId: String
        compose.runOnIdle { entryId = nav.currentBackStackEntry!!.id }
        compose.onNodeWithTag("main_tab_${BrainOutRoutes.ProjectsGraph}").performClick()
        compose.runOnIdle { assertThat(nav.currentBackStackEntry!!.id).isEqualTo(entryId) }
        compose.runOnIdle { nav.popBackStack() }
        compose.onNodeWithText("Lista de projetos").assertExists()
    }

    @Test fun `autenticacao nao mostra abas e entrada posterior preserva navegacao`() {
        render(BrainOutRoutes.Login)
        compose.onNodeWithTag("main_navigation").assertDoesNotExist()
        compose.runOnIdle { nav.navigate(BrainOutRoutes.MainGraph) { popUpTo(0) { inclusive = true } } }
        compose.onNodeWithTag("main_tab_${BrainOutRoutes.TasksGraph}").performClick()
        compose.onNodeWithTag("main_tab_${BrainOutRoutes.ProjectsGraph}").performClick()
        compose.onNodeWithText("Lista de projetos").assertExists()
    }

    @Test fun `encerrar sessao remove tambem historico de abas estacionadas`() {
        render()
        compose.runOnIdle { nav.navigate(BrainOutRoutes.projectDetail("p1")) }
        compose.onNodeWithTag("main_tab_${BrainOutRoutes.SettingsGraph}").performClick()
        compose.runOnIdle {
            nav.clearMainBackStacks()
            nav.navigate(BrainOutRoutes.Login) { popUpTo(0) { inclusive = true } }
        }
        compose.onNodeWithTag("main_navigation").assertDoesNotExist()
        compose.runOnIdle { nav.navigate(BrainOutRoutes.MainGraph) { popUpTo(0) { inclusive = true } } }
        compose.onNodeWithText("Lista de projetos").assertExists()
        compose.onNodeWithTag("detail_query").assertDoesNotExist()
    }
}
