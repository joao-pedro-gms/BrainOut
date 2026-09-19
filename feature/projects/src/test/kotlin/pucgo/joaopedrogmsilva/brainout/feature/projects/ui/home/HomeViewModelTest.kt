// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.data.session.ActiveUserProvider
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TagRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CreateProjectUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CreateTagUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.DeleteProjectUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.UpdateProjectUseCase

/**
 * Testes do [HomeViewModel].
 *
 * Verifica (E1.7):
 * - Conversão de [User] (domínio) para [HomeUserState].
 * - Distinção entre Owner e Member no estado exposto.
 * - `onMemberFabClicked` / `dismissUpgradeDialog` alternam o flag do
 *   diálogo de upgrade corretamente.
 * - Sessão órfã (id presente mas `User` ausente) leva a `SignedOut`.
 *
 * Verifica (E2.1):
 * - `createProject` dispara o use case com `ownerId` derivado de
 *   [ActiveUserProvider.observeActiveUserId].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val projectRepository: ProjectRepository = mockk(relaxed = true)
    private val tagRepository: TagRepository = mockk(relaxed = true)
    private val activeUserProvider: ActiveUserProvider = mockk()
    private val createProject: CreateProjectUseCase = mockk()
    private val updateProject: UpdateProjectUseCase = mockk()
    private val deleteProject: DeleteProjectUseCase = mockk()
    private val createTag: CreateTagUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { projectRepository.observeAllForOwner(any()) } returns flowOf(emptyList())
        coEvery { tagRepository.observeForOwner(any()) } returns flowOf(emptyList())
        coEvery { activeUserProvider.observeActiveUserId() } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(): HomeViewModel = HomeViewModel(
        projectRepository = projectRepository,
        tagRepository = tagRepository,
        activeUserProvider = activeUserProvider,
        createProject = createProject,
        updateProject = updateProject,
        deleteProject = deleteProject,
        createTag = createTag,
    )

    @Test
    fun `SignedIn owner surfaces owner role`() = runTest {
        val userFlow = MutableStateFlow<User?>(sampleUser(role = UserRole.OWNER))
        coEvery { activeUserProvider.observeActiveUser() } returns userFlow
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.userState.test {
            // Pode vir Loading inicialmente; consumimos até SignedIn.
            var emitted = awaitItem()
            while (emitted is HomeUserState.Loading) {
                emitted = awaitItem()
            }
            assertThat(emitted).isInstanceOf(HomeUserState.SignedIn::class.java)
            val signedIn = emitted as HomeUserState.SignedIn
            assertThat(signedIn.displayName).isEqualTo("João Pedro")
            assertThat(signedIn.role).isEqualTo(HomeUserRole.Owner)
            assertThat(signedIn.initials).isEqualTo("JP")
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { activeUserProvider.observeActiveUser() }
    }

    @Test
    fun `SignedIn member surfaces member role`() = runTest {
        val userFlow = MutableStateFlow<User?>(sampleUser(role = UserRole.MEMBER))
        coEvery { activeUserProvider.observeActiveUser() } returns userFlow
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.userState.test {
            var emitted = awaitItem()
            while (emitted is HomeUserState.Loading) {
                emitted = awaitItem()
            }
            assertThat(emitted).isInstanceOf(HomeUserState.SignedIn::class.java)
            val signedIn = emitted as HomeUserState.SignedIn
            assertThat(signedIn.role).isEqualTo(HomeUserRole.Member)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `null user surfaces SignedOut`() = runTest {
        val userFlow = MutableStateFlow<User?>(null)
        coEvery { activeUserProvider.observeActiveUser() } returns userFlow
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.userState.test {
            var emitted = awaitItem()
            while (emitted is HomeUserState.Loading) {
                emitted = awaitItem()
            }
            assertThat(emitted).isEqualTo(HomeUserState.SignedOut)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onMemberFabClicked opens upgrade dialog`() = runTest {
        val userFlow = MutableStateFlow<User?>(sampleUser(role = UserRole.MEMBER))
        coEvery { activeUserProvider.observeActiveUser() } returns userFlow
        val viewModel = newViewModel()
        advanceUntilIdle()

        assertThat(viewModel.upgradeDialogVisible.value).isFalse()
        viewModel.onMemberFabClicked()
        assertThat(viewModel.upgradeDialogVisible.value).isTrue()
        viewModel.dismissUpgradeDialog()
        assertThat(viewModel.upgradeDialogVisible.value).isFalse()
    }

    @Test
    fun `computeInitials produces sensible monograms`() {
        assertThat(computeInitials("João Pedro")).isEqualTo("JP")
        assertThat(computeInitials("madonna")).isEqualTo("MA")
        assertThat(computeInitials("Maria de Lourdes Silva")).isEqualTo("MS")
        assertThat(computeInitials("")).isEqualTo("?")
        assertThat(computeInitials("   ")).isEqualTo("?")
    }

    @Test
    fun `createProject dispara use case com ownerId correto`() = runTest {
        val newProject = Project.create(name = "Novo", ownerId = "u1")
        coEvery { activeUserProvider.observeActiveUserId() } returns flowOf("u1")
        coEvery { activeUserProvider.observeActiveUser() } returns flowOf(sampleUser(role = UserRole.OWNER))
        coEvery { createProject.invoke(any(), any(), any(), any()) } returns newProject

        val viewModel = newViewModel()
        viewModel.createProject("Novo", null, emptyList())
        advanceUntilIdle()

        coVerify {
            createProject.invoke(name = "Novo", ownerId = "u1", description = null, tagIds = emptyList())
        }
    }

    private fun sampleUser(role: UserRole): User = User(
        id = "user-id-${role.name}",
        name = "João Pedro",
        email = "joao@example.com",
        passwordHash = "pbkdf2_sha256\$120000\$AAAA\$BBBB",
        role = role,
        createdAt = Instant.parse("2026-09-18T10:00:00Z"),
    )
}
