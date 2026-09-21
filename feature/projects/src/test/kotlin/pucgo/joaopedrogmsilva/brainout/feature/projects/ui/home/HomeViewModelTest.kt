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
import kotlinx.coroutines.flow.flow
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

    // === E2.8 — estados de erro e retry ==========================================

    /**
     * Verifica a transição do estado de UI quando o Flow emite
     * uma lista inicial (E2.8). Como `MutableStateFlow` emite
     * imediatamente em `subscribe`, o "loading inicial" só é
     * observável via `uiState.value` antes da primeira coleta
     * (cenário típico com Room real, onde a query é assíncrona).
     *
     * Aqui verificamos que, uma vez que o Flow emite a primeira
     * lista, `isLoading` é `false` e o estado contém a lista —
     * ou seja, a pipeline reage corretamente à primeira
     * emissão do upstream (parâmetro fundamental do spinner).
     */
    @Test
    fun `E2 8 uiState recebe primeira emissao do Flow com isLoading false`() = runTest {
        val projectsFlow = MutableStateFlow<List<Project>>(emptyList())
        val tagsFlow = MutableStateFlow<List<pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag>>(emptyList())
        coEvery { activeUserProvider.observeActiveUserId() } returns flowOf("u1")
        coEvery { activeUserProvider.observeActiveUser() } returns flowOf(sampleUser(role = UserRole.OWNER))
        coEvery { projectRepository.observeAllForOwner("u1") } returns projectsFlow
        coEvery { tagRepository.observeForOwner("u1") } returns tagsFlow

        val viewModel = newViewModel()

        // Estado inicial antes da primeira emissão do `combine`
        // (cacheado como `initialValue`): `isLoading = true`. Isso
        // é o que a UI usa para renderizar o spinner enquanto o
        // Room não emite.
        assertThat(viewModel.uiState.value.isLoading).isTrue()

        viewModel.uiState.test {
            // Primeira emissão do `combine`: lista vazia, loading
            // concluído.
            advanceUntilIdle()
            var state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.projects).isEmpty()

            // Emite a primeira lista não-vazia.
            val now = Instant.parse("2026-09-19T00:00:00Z")
            projectsFlow.value = listOf(
                Project.create(name = "P1", ownerId = "u1", now = now),
            )
            advanceUntilIdle()
            state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.projects).hasSize(1)

            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Simula falha do Room no Flow de projetos: o `.catch` no
     * ViewModel converte a exceção em `errorMessage` canônico
     * (`ERROR_LOAD_FAILED`) e força `isLoading = false`.
     */
    @Test
    fun `E2 8 erro do Flow de projetos popula errorMessage e sai do loading`() = runTest {
        coEvery { activeUserProvider.observeActiveUserId() } returns flowOf("u1")
        coEvery { activeUserProvider.observeActiveUser() } returns flowOf(sampleUser(role = UserRole.OWNER))
        coEvery { projectRepository.observeAllForOwner("u1") } returns flow {
            throw IllegalStateException("boom")
        }
        coEvery { tagRepository.observeForOwner("u1") } returns flowOf(emptyList())

        val viewModel = newViewModel()

        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertThat(state.errorMessage).isEqualTo(HomeViewModel.ERROR_LOAD_FAILED)
            assertThat(state.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(viewModel.errorMessage.value).isEqualTo(HomeViewModel.ERROR_LOAD_FAILED)
    }

    /**
     * Após uma falha, `retry()` deve re-assinar o Flow. Aqui
     * trocamos o mock do repositório para devolver uma lista
     * vazia (sucesso) e verificamos que o `errorMessage` some.
     *
     * E2.8 — usamos um `subscriptionCount` explícito via `test`
     * para forçar a coleta do `WhileSubscribed` antes de ler
     * `errorMessage.value`. Sem um assinante, o upstream nem
     * chega a iniciar (e o `catch` nunca dispara).
     */
    @Test
    fun `E2 8 retry re-assina o Flow apos erro e limpa errorMessage`() = runTest {
        val projectsFlow = MutableStateFlow<List<Project>>(emptyList())
        coEvery { activeUserProvider.observeActiveUserId() } returns flowOf("u1")
        coEvery { activeUserProvider.observeActiveUser() } returns flowOf(sampleUser(role = UserRole.OWNER))
        coEvery { tagRepository.observeForOwner("u1") } returns flowOf(emptyList())
        // Inicialmente falha; após retry, o mock é reconfigurado abaixo.
        coEvery { projectRepository.observeAllForOwner("u1") } returns flow {
            throw IllegalStateException("boom")
        }

        val viewModel = newViewModel()

        // Força a inscrição no `stateIn` para que o upstream
        // comece a emitir e o `catch` seja exercitado.
        viewModel.uiState.test {
            advanceUntilIdle()
            // Após a falha, `errorMessage` deve estar populado.
            assertThat(viewModel.errorMessage.value).isEqualTo(HomeViewModel.ERROR_LOAD_FAILED)

            // Reconfigura o mock para emitir lista vazia (sucesso).
            coEvery { projectRepository.observeAllForOwner("u1") } returns projectsFlow

            viewModel.retry()
            advanceUntilIdle()

            // O retry re-assina o Flow e o sucesso limpa o erro.
            assertThat(viewModel.errorMessage.value).isNull()
            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.errorMessage).isNull()

            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Falha em `createProject` (uso de I/O do Room) deve ser
     * capturada e convertida em `ERROR_ACTION_FAILED` em
     * `errorMessage` — sem crash, sem stack trace exposto.
     */
    @Test
    fun `E2 8 falha em createProject popula errorMessage de acao`() = runTest {
        coEvery { activeUserProvider.observeActiveUserId() } returns flowOf("u1")
        coEvery { activeUserProvider.observeActiveUser() } returns flowOf(sampleUser(role = UserRole.OWNER))
        coEvery { createProject.invoke(any(), any(), any(), any()) } throws
            IllegalStateException("disk full")

        val viewModel = newViewModel()
        viewModel.createProject("x", null, emptyList())
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isEqualTo(HomeViewModel.ERROR_ACTION_FAILED)
    }

    @Test
    fun `E2 8 clearError zera errorMessage`() = runTest {
        coEvery { activeUserProvider.observeActiveUserId() } returns flowOf("u1")
        coEvery { activeUserProvider.observeActiveUser() } returns flowOf(sampleUser(role = UserRole.OWNER))
        coEvery { createProject.invoke(any(), any(), any(), any()) } throws
            IllegalStateException("boom")

        val viewModel = newViewModel()
        viewModel.createProject("x", null, emptyList())
        advanceUntilIdle()
        assertThat(viewModel.errorMessage.value).isNotNull()

        viewModel.clearError()
        assertThat(viewModel.errorMessage.value).isNull()
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
