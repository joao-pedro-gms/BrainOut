// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Testes de UI da [TasksScreen].
//
// Rodam em JVM via Robolectric + createComposeRule. Verificam a
// renderização da lista de tarefas e, em especial, o rótulo da tarefa
// sem projeto — que antes era um literal pt-BR no ViewModel e vazava
// em qualquer locale diferente de pt-BR (DEF-50).

package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import androidx.compose.material3.Surface
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoWindowProvider

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
    fun `estado de carregamento anuncia o que esta acontecendo`() {
        composeRule.setContent {
            BrainOutTheme {
                Surface {
                    TasksContent(state = TasksUiState(rows = emptyList(), isLoading = true))
                }
            }
        }

        // O indicador é visual: um spinner nu não diz nada ao TalkBack.
        composeRule
            .onNodeWithTag(TasksTestTags.LOADING)
            .assert(hasContentDescription("Carregando tarefas"))
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

    @Test
    fun `estado de carregamento anuncia em ingles`() {
        composeRule.setContent {
            BrainOutTheme {
                Surface {
                    TasksContent(state = TasksUiState(rows = emptyList(), isLoading = true))
                }
            }
        }

        composeRule
            .onNodeWithTag(TasksTestTags.LOADING)
            .assert(hasContentDescription("Loading tasks"))
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w1280dp-h800dp")
class TasksTabletGridTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `lista de tarefas usa grade em largura de tablet`() {
        composeRule.setContent {
            BrainOutTheme {
                NeoWindowProvider {
                    Surface {
                        TasksContent(
                            state =
                                TasksUiState(
                                    rows =
                                        listOf(
                                            TaskRow(
                                                task = Task.create(projectId = "p1", title = "Task one"),
                                                projectName = "Project",
                                            ),
                                            TaskRow(
                                                task = Task.create(projectId = "p1", title = "Task two"),
                                                projectName = "Project",
                                            ),
                                        ),
                                    isLoading = false,
                                ),
                        )
                    }
                }
            }
        }

        val first = composeRule.onNodeWithText("Task one").fetchSemanticsNode().boundsInRoot
        val second = composeRule.onNodeWithText("Task two").fetchSemanticsNode().boundsInRoot
        assertTrue("tarefas na primeira linha devem compartilhar a altura", first.top == second.top)
        assertTrue("a segunda coluna deve ficar à direita", second.left > first.left)
    }
}

@RunWith(AndroidJUnit4::class)
class TasksGridBreakpointTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(sdk = [34], qualifiers = "w600dp-h800dp")
    fun `a grade comeca no breakpoint de 600 dp`() {
        setTwoTaskContent()

        val first = composeRule.onNodeWithText("Task one").fetchSemanticsNode().boundsInRoot
        val second = composeRule.onNodeWithText("Task two").fetchSemanticsNode().boundsInRoot
        assertTrue("tarefas devem compartilhar a linha a partir de 600 dp", first.top == second.top)
        assertTrue("a segunda tarefa deve estar na coluna à direita", second.left > first.left)
    }

    @Test
    @Config(sdk = [34], qualifiers = "w599dp-h800dp")
    fun `a lista continua em uma coluna abaixo do breakpoint`() {
        setTwoTaskContent()

        val first = composeRule.onNodeWithText("Task one").fetchSemanticsNode().boundsInRoot
        val second = composeRule.onNodeWithText("Task two").fetchSemanticsNode().boundsInRoot
        assertTrue("tarefas devem ficar em linhas distintas abaixo de 600 dp", second.top > first.top)
    }

    private fun setTwoTaskContent() {
        composeRule.setContent {
            BrainOutTheme {
                NeoWindowProvider {
                    Surface {
                        TasksContent(
                            state =
                                TasksUiState(
                                    rows =
                                        listOf(
                                            TaskRow(
                                                task = Task.create(projectId = "p1", title = "Task one"),
                                                projectName = "Project",
                                            ),
                                            TaskRow(
                                                task = Task.create(projectId = "p1", title = "Task two"),
                                                projectName = "Project",
                                            ),
                                        ),
                                    isLoading = false,
                                ),
                        )
                    }
                }
            }
        }
    }
}
