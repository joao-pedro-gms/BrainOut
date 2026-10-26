// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.data.session.SessionStore
import pucgo.joaopedrogmsilva.brainout.core.domain.error.DuplicateUsernameException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidCredentialsException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.AuthenticateUserUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CreateUserUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val create = mockk<CreateUserUseCase>()
    private val authenticate = mockk<AuthenticateUserUseCase>()
    private val session = mockk<SessionStore>(relaxed = true)

    private fun viewModel() = AuthViewModel(create, authenticate, session)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cadastro persiste sessao e emite navegacao`() =
        runTest {
            val user = User.create("Joao", "hash")
            coEvery { create("Joao", "fixture-input-long") } returns user
            val vm = viewModel()
            vm.onUsernameChange("Joao")
            vm.onPasswordChange("fixture-input-long")
            vm.onPasswordConfirmationChange("fixture-input-long")
            vm.events.test {
                vm.submitRegister()
                advanceUntilIdle()
                assertThat(awaitItem()).isEqualTo(AuthEvent.NavigateHome)
            }
            coVerify(exactly = 1) { session.saveUserId(user.id) }
            assertThat(vm.state.value.isLoading).isFalse()
        }

    @Test
    fun `nome ocupado gera erro tipado no campo e permite tentar novamente`() =
        runTest {
            coEvery { create(any(), any()) } throws DuplicateUsernameException("Joao")
            val vm = viewModel()
            vm.onUsernameChange("Joao")
            vm.onPasswordChange("fixture-input-long")
            vm.onPasswordConfirmationChange("fixture-input-long")
            vm.submitRegister()
            advanceUntilIdle()
            assertThat(vm.state.value.usernameError).isEqualTo(AuthErrorType.DuplicateUsername)
            vm.onUsernameChange("joao")
            assertThat(vm.state.value.usernameError).isNull()
            coVerify(exactly = 0) { session.saveUserId(any()) }
        }

    @Test
    fun `confirmacao diferente impede cadastro`() =
        runTest {
            val vm = viewModel()
            vm.onUsernameChange("Joao")
            vm.onPasswordChange("fixture-input-long")
            vm.onPasswordConfirmationChange("different-input")
            vm.submitRegister()
            assertThat(vm.state.value.passwordConfirmationError).isEqualTo(AuthErrorType.PasswordMismatch)
            coVerify(exactly = 0) { create(any(), any()) }
        }

    @Test
    fun `login preserva caixa e falha generica nao abre sessao`() =
        runTest {
            coEvery { authenticate("joao", "fixture-input-long") } throws InvalidCredentialsException()
            val vm = viewModel()
            vm.onUsernameChange("joao")
            vm.onPasswordChange("fixture-input-long")
            vm.submitLogin()
            advanceUntilIdle()
            assertThat(vm.state.value.passwordError).isEqualTo(AuthErrorType.InvalidCredentials)
            coVerify(exactly = 0) { session.saveUserId(any()) }
        }

    @Test
    fun `cadastro simultaneo e bloqueado durante envio`() =
        runTest {
            coEvery { create(any(), any()) } returns User.create("Joao", "hash")
            val vm = viewModel()
            vm.onUsernameChange("Joao")
            vm.onPasswordChange("fixture-input-long")
            vm.onPasswordConfirmationChange("fixture-input-long")
            vm.submitRegister()
            vm.submitRegister()
            advanceUntilIdle()
            coVerify(exactly = 1) { create(any(), any()) }
        }

    @Test
    fun `falha inesperada retorna formulario editavel`() =
        runTest {
            coEvery { authenticate(any(), any()) } throws IllegalStateException("fixture failure")
            val vm = viewModel()
            vm.onUsernameChange("Joao")
            vm.onPasswordChange("fixture-input-long")
            vm.submitLogin()
            advanceUntilIdle()
            assertThat(vm.state.value.errorMessage).isEqualTo(AuthErrorType.Unexpected)
            assertThat(vm.state.value.isLoading).isFalse()
        }
}
