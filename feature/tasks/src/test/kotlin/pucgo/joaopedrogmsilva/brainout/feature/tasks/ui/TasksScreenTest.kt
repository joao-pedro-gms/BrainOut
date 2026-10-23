// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Testes de UI da [TasksScreen].
//
// Rodam em JVM via Robolectric + createComposeRule. Verificam a
// renderização da lista de tarefas e, em especial, o rótulo da tarefa
// sem projeto — que antes era um literal pt-BR no ViewModel e vazava
// em qualquer locale diferente de pt-BR (DEF-50).

package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme

/**
 * Verifica a renderização do conteúdo da aba de Tarefas.
 */
@RunWith(AndroidJUnit4::class)
// pt-rBR garante resolução das strings do `values/` padrão (pt): com o
// locale default do Robolectric (en), `values-en` venceria a resolução.
@Config(sdk = [34], qualifiers = "pt-rBR")
class TasksScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun taskRow(projectName: String?): TaskRow =
        TaskRow(
            task = Task.create(projectId = "p1", title = "Revisar relatório mensal"),
            projectName = projectName,
        )

    private fun setRows(vararg rows: TaskRow) {
        composeRule.setContent {
            BrainOutTheme {
                Surface {
                    TasksContent(state = TasksUiState(rows = rows.toList(), isLoading = false))
                }
            }
        }
    }

    @Test
    fun `tarefa com projeto mostra o nome do projeto`() {
        setRows(taskRow(projectName = "Projeto Demo"))

        composeRule.onNodeWithText("Projeto Demo").assertExists()
    }

    @Test
    fun `tarefa sem projeto mostra o rotulo localizado`() {
        setRows(taskRow(projectName = null))

        composeRule.onNodeWithText("(sem projeto)").assertExists()
    }

    @Test
    fun `a lista expõe a tag de list`() {
        setRows(taskRow(projectName = "Projeto Demo"))

        composeRule.onNodeWithTag(TasksTestTags.LIST).assertExists()
    }

    @Test
    fun `estado de carregamento expõe a tag de loading`() {
        composeRule.setContent {
            BrainOutTheme {
                Surface {
                    TasksContent(state = TasksUiState(rows = emptyList(), isLoading = true))
                }
            }
        }

        composeRule.onNodeWithTag(TasksTestTags.LOADING).assertExists()
    }

    @Test
    fun `estado vazio expõe a tag de empty`() {
        composeRule.setContent {
            BrainOutTheme {
                Surface {
                    TasksContent(state = TasksUiState(rows = emptyList(), isLoading = false))
                }
            }
        }

        composeRule.onNodeWithTag(TasksTestTags.EMPTY).assertExists()
    }
}

/**
 * O mesmo cartão, em locale `en`: o rótulo precisa acompanhar o
 * idioma (DEF-50). Com o literal pt-BR no ViewModel, o app em inglês
 * mostrava "(sem projeto)".
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "en-rUS")
class TasksScreenEnglishTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `tarefa sem projeto mostra o rotulo em ingles`() {
        composeRule.setContent {
            BrainOutTheme {
                Surface {
                    TasksContent(
                        state =
                            TasksUiState(
                                rows =
                                    listOf(
                                        TaskRow(
                                            task = Task.create(projectId = "p1", title = "Monthly report"),
                                            projectName = null,
                                        ),
                                    ),
                                isLoading = false,
                            ),
                    )
                }
            }
        }

        composeRule.onNodeWithText("(no project)").assertExists()
    }
}
