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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.data.session.ActiveUserProvider
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole

/**
 * Testes do [HomeViewModel] (E1.7).
 *
 * Verifica:
 * - Conversão de [User] (domínio) para [HomeUiState].
 * - Distinção entre Owner e Member no estado exposto.
 * - `onMemberFabClicked` / `dismissUpgradeDialog` alternam o flag do
 *   diálogo de upgrade corretamente.
 * - Sessão órfã (id presente mas `User` ausente) leva a `SignedOut`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `SignedIn owner surfaces owner role`() = runTest {
        val activeUser = mockk<ActiveUserProvider>()
        val userFlow = MutableStateFlow<User?>(sampleUser(role = UserRole.OWNER))
        coEvery { activeUser.observeActiveUser() } returns userFlow
        val viewModel = HomeViewModel(activeUser)
        advanceUntilIdle()

        viewModel.state.test {
            // Pode vir Loading inicialmente; consumimos até SignedIn.
            var emitted = awaitItem()
            while (emitted is HomeUiState.Loading) {
                emitted = awaitItem()
            }
            assertThat(emitted).isInstanceOf(HomeUiState.SignedIn::class.java)
            val signedIn = emitted as HomeUiState.SignedIn
            assertThat(signedIn.displayName).isEqualTo("João Pedro")
            assertThat(signedIn.role).isEqualTo(HomeUserRole.Owner)
            assertThat(signedIn.initials).isEqualTo("JP")
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { activeUser.observeActiveUser() }
    }

    @Test
    fun `SignedIn member surfaces member role`() = runTest {
        val activeUser = mockk<ActiveUserProvider>()
        val userFlow = MutableStateFlow<User?>(sampleUser(role = UserRole.MEMBER))
        coEvery { activeUser.observeActiveUser() } returns userFlow
        val viewModel = HomeViewModel(activeUser)
        advanceUntilIdle()

        viewModel.state.test {
            var emitted = awaitItem()
            while (emitted is HomeUiState.Loading) {
                emitted = awaitItem()
            }
            assertThat(emitted).isInstanceOf(HomeUiState.SignedIn::class.java)
            val signedIn = emitted as HomeUiState.SignedIn
            assertThat(signedIn.role).isEqualTo(HomeUserRole.Member)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `null user surfaces SignedOut`() = runTest {
        val activeUser = mockk<ActiveUserProvider>()
        val userFlow = MutableStateFlow<User?>(null)
        coEvery { activeUser.observeActiveUser() } returns userFlow
        val viewModel = HomeViewModel(activeUser)
        advanceUntilIdle()

        viewModel.state.test {
            var emitted = awaitItem()
            while (emitted is HomeUiState.Loading) {
                emitted = awaitItem()
            }
            assertThat(emitted).isEqualTo(HomeUiState.SignedOut)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onMemberFabClicked opens upgrade dialog`() = runTest {
        val activeUser = mockk<ActiveUserProvider>()
        val userFlow = MutableStateFlow<User?>(sampleUser(role = UserRole.MEMBER))
        coEvery { activeUser.observeActiveUser() } returns userFlow
        val viewModel = HomeViewModel(activeUser)
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

    private fun sampleUser(role: UserRole): User = User(
        id = "user-id-${role.name}",
        name = "João Pedro",
        email = "joao@example.com",
        passwordHash = "pbkdf2_sha256\$120000\$AAAA\$BBBB",
        role = role,
        createdAt = Instant.parse("2026-09-18T10:00:00Z"),
    )
}
