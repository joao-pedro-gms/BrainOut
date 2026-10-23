// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
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
import pucgo.joaopedrogmsilva.brainout.core.data.session.ActiveUserProvider
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ListingPreferences
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ListingPreferencesRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TagRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CreateProjectUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CreateTagUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.DeleteProjectUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.UpdateProjectUseCase
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.feature.projects.R
import java.time.Instant

/**
 * Testes da tela inicial (E2.1/E2.5/E2.6).
 *
 * A auditoria de 24/09 listava `HomeScreen.kt` como gap P1: havia
 * suíte de **componente** (top bar, linha de filtro, diálogo), mas
 * nada provava a tela montada.
 *
 * O que fica provado aqui:
 *
 * - a tela mostra o estado vazio quando não há projeto;
 * - cada projeto vira um card;
 * - tocar num card avisa qual projeto foi aberto;
 * - o FAB abre o diálogo de criar projeto;
 * - enquanto o Room não responde, a tela mostra o carregamento.
 *
 * O teste roda em Robolectric: a tela recebe o ViewModel por
 * parâmetro, então não precisa do grafo do Hilt.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "pt-rBR")
class HomeScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val projectRepository: ProjectRepository = mockk(relaxed = true)
    private val tagRepository: TagRepository = mockk(relaxed = true)
    private val activeUserProvider: ActiveUserProvider = mockk()
    private val listingPreferences: ListingPreferencesRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { activeUserProvider.observeActiveUserId() } returns flowOf("u1")
        coEvery { activeUserProvider.observeActiveUser() } returns flowOf(sampleUser())
        coEvery { listingPreferences.observe(any()) } returns flowOf(ListingPreferences())
        coEvery { tagRepository.observeForOwner(any()) } returns flowOf(emptyList())
        coEvery { tagRepository.observeByProjectIds(any()) } returns flowOf(emptyMap())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleUser(): User =
        User(
            id = "user-1",
            name = "João Pedro",
            email = "joao@example.com",
            passwordHash = "hash-de-teste",
            role = UserRole.OWNER,
            createdAt = Instant.parse("2026-09-18T10:00:00Z"),
        )

    private fun newViewModel(): HomeViewModel =
        HomeViewModel(
            projectRepository = projectRepository,
            tagRepository = tagRepository,
            activeUserProvider = activeUserProvider,
            listingPreferences = listingPreferences,
            createProject = mockk<CreateProjectUseCase>(relaxed = true),
            updateProject = mockk<UpdateProjectUseCase>(relaxed = true),
            deleteProject = mockk<DeleteProjectUseCase>(relaxed = true),
            createTag = mockk<CreateTagUseCase>(relaxed = true),
        )

    private fun setContent(
        viewModel: HomeViewModel = newViewModel(),
        onOpenProject: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            BrainOutTheme {
                HomeScreen(
                    onOpenProject = onOpenProject,
                    viewModel = viewModel,
                )
            }
        }
    }

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `sem projetos a tela mostra o estado vazio`() {
        coEvery { projectRepository.observeSearch(any(), any(), any(), any()) } returns flowOf(emptyList())

        setContent()

        composeRule.onNodeWithText(context().getString(R.string.home_empty_title)).assertExists()
        composeRule.onNodeWithTag(HomeTestTags.PROJECT_CARD).assertDoesNotExist()
    }

    @Test
    fun `cada projeto vira um card`() {
        coEvery { projectRepository.observeSearch(any(), any(), any(), any()) } returns
            flowOf(
                listOf(
                    Project.create(name = "Projeto Um", ownerId = "u1"),
                    Project.create(name = "Projeto Dois", ownerId = "u1"),
                ),
            )

        setContent()

        composeRule.onNodeWithText("Projeto Um").assertExists()
        composeRule.onNodeWithText("Projeto Dois").assertExists()
    }

    @Test
    fun `tocar num card avisa qual projeto foi aberto`() {
        val projeto = Project.create(name = "Projeto Um", ownerId = "u1")
        coEvery { projectRepository.observeSearch(any(), any(), any(), any()) } returns flowOf(listOf(projeto))
        var aberto: String? = null

        setContent { id -> aberto = id }

        composeRule.onNodeWithTag(HomeTestTags.PROJECT_CARD).performClick()

        assertThat(aberto).isEqualTo(projeto.id)
    }

    @Test
    fun `o FAB abre o dialogo de criar projeto`() {
        coEvery { projectRepository.observeSearch(any(), any(), any(), any()) } returns flowOf(emptyList())

        setContent()

        composeRule.onNodeWithTag(HomeTestTags.FAB).performClick()

        composeRule.onNodeWithTag(HomeTestTags.CREATE_PROJECT_DIALOG).assertExists()
    }

    @Test
    fun `enquanto o Room nao responde a tela mostra o carregamento`() {
        // Fluxo que nunca emite: o estado inicial (carregando) persiste.
        coEvery { projectRepository.observeSearch(any(), any(), any(), any()) } returns emptyFlow()

        setContent()

        composeRule.onNodeWithTag(HomeTestTags.LOADING).assertExists()
    }
}
