// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.ChangeTaskStatusUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CheckDeadlineUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CreateTaskUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.DeleteProjectUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.DeleteTaskUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.UpdateTaskUseCase
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

/**
 * Testes da tela de detalhe do projeto (E2.2).
 *
 * Havia suíte do ViewModel (`ProjectDetailViewModelTest`) e de
 * componente (banner de erro, diálogo de nova tarefa), mas a tela
 * montada não era exercitada — a auditoria de 24/09 listava
 * `ProjectDetailScreen.kt` como gap P1.
 *
 * O que fica provado aqui:
 *
 * - sem tarefas, a tela mostra o card vazio;
 * - cada tarefa aparece na lista;
 * - o FAB abre o diálogo de nova tarefa;
 * - enquanto o Room não responde, a tela mostra o carregamento;
 * - o menu da tarefa move a tarefa de estado (a transição chega ao
 *   ViewModel, com o status de destino certo).
 *
 * O teste roda em Robolectric: a tela recebe o ViewModel por
 * parâmetro, então não precisa do grafo do Hilt.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "pt-rBR")
class ProjectDetailScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val projectId: String = "p1"
    private lateinit var taskRepository: TaskRepository
    private lateinit var changeStatus: ChangeTaskStatusUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        taskRepository = mockk(relaxed = true)
        changeStatus = mockk(relaxed = true)
        coEvery { taskRepository.observeForProject(projectId) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun viewModel(): ProjectDetailViewModel =
        ProjectDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf(ProjectDetailViewModel.PROJECT_ID_ARG to projectId)),
            taskRepository = taskRepository,
            createTask = mockk<CreateTaskUseCase>(relaxed = true),
            updateTask = mockk<UpdateTaskUseCase>(relaxed = true),
            changeStatus = changeStatus,
            deleteProject = mockk<DeleteProjectUseCase>(relaxed = true),
            deleteTask = mockk<DeleteTaskUseCase>(relaxed = true),
            checkDeadline = mockk<CheckDeadlineUseCase>(relaxed = true),
        )

    private fun setContent(
        viewModel: ProjectDetailViewModel = viewModel(),
        onBackClicked: () -> Unit = {},
    ) {
        composeRule.setContent {
            BrainOutTheme {
                ProjectDetailScreen(
                    projectId = projectId,
                    onBackClicked = onBackClicked,
                    viewModel = viewModel,
                )
            }
        }
    }

    @Test
    fun `sem tarefas a tela mostra o card vazio`() {
        setContent()

        composeRule.onNodeWithText(context().getString(R.string.project_detail_empty_title)).assertExists()
    }

    @Test
    fun `cada tarefa aparece na lista`() {
        coEvery { taskRepository.observeForProject(projectId) } returns
            flowOf(
                listOf(
                    Task.create(projectId = projectId, title = "Tarefa Um"),
                    Task.create(projectId = projectId, title = "Tarefa Dois"),
                ),
            )

        setContent()

        composeRule.onNodeWithText("Tarefa Um").assertExists()
        composeRule.onNodeWithText("Tarefa Dois").assertExists()
    }

    @Test
    fun `o FAB abre o dialogo de nova tarefa`() {
        setContent()

        composeRule.onNodeWithTag(ProjectDetailTestTags.NEW_TASK_FAB).performClick()

        composeRule.onNodeWithTag(ProjectDetailTestTags.NEW_TASK_DIALOG).assertExists()
    }

    @Test
    fun `enquanto o Room nao responde a tela mostra o carregamento`() {
        // Fluxo que nunca emite: o estado inicial (carregando) persiste.
        coEvery { taskRepository.observeForProject(projectId) } returns emptyFlow()

        setContent()

        composeRule.onNodeWithTag(ProjectDetailTestTags.LOADING).assertExists()
    }

    @Test
    fun `o menu da tarefa move a tarefa para concluida`() {
        val tarefa = Task.create(projectId = projectId, title = "Tarefa Um", status = TaskStatus.DOING)
        coEvery { taskRepository.observeForProject(projectId) } returns flowOf(listOf(tarefa))

        setContent()

        composeRule
            .onNodeWithContentDescription(context().getString(R.string.project_detail_task_menu_more))
            .performClick()
        composeRule.onNodeWithTag(ProjectDetailTestTags.TASK_ITEM_MENU_MOVE_DONE).performClick()

        coVerify(exactly = 1) { changeStatus.invoke(tarefa.id, TaskStatus.DONE) }
    }
}
