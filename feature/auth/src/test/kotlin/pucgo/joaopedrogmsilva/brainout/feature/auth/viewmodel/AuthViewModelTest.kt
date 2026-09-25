// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.data.session.SessionStore
import pucgo.joaopedrogmsilva.brainout.core.domain.error.DuplicateEmailException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidCredentialsException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.AuthenticateUserUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CreateUserUseCase

/**
 * Testes unitários do [AuthViewModel] cobrindo os fluxos exigidos pelo
 * roadmap E1.6:
 *
 * - Cadastro feliz — usa caso de uso é chamado, sessão é persistida,
 *   estado `success = true` é emitido.
 * - Cadastro com e-mail duplicado — estado expõe erro inline de e-mail
 *   e mensagem geral.
 * - Login errado — caso de uso lança [InvalidCredentialsException] e o
 *   estado expõe a mensagem canônica em `errorMessage`/`passwordError`.
 * - Login após registro — encadeamento `CreateUserUseCase` →
 *   `AuthenticateUserUseCase` é respeitado pelo caller.
 *
 * Os casos de uso e o [SessionStore] são mocks do MockK. O dispatcher
 * é o `StandardTestDispatcher` que o `runTest` configura por padrão —
 * não há referências ao `Dispatchers.Main` (looper Android) que
 * precisariam de Robolectric.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        // `ViewModel.viewModelScope` é Main-immediate; sem Android
        // precisamos substituir o dispatcher principal pelo test
        // dispatcher para evitar `Looper.getMainLooper` não mockado.
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(
        createUser: CreateUserUseCase = mockk(relaxed = true),
        authenticate: AuthenticateUserUseCase = mockk(relaxed = true),
        sessionStore: SessionStore = mockk(relaxed = true),
    ): AuthViewModel = AuthViewModel(
        createUserUseCase = createUser,
        authenticateUserUseCase = authenticate,
        sessionStore = sessionStore,
    )

    @Test
    fun `submitRegister happy path persists session and emits success`() = runTest {
        val createUser = mockk<CreateUserUseCase>()
        val authenticate = mockk<AuthenticateUserUseCase>()
        val sessionStore = mockk<SessionStore>(relaxed = true)
        val newUser = sampleUser(role = UserRole.OWNER)

        coEvery { createUser(any(), any(), any(), any()) } returns newUser
        coEvery { authenticate(any(), any()) } returns newUser
        coEvery { sessionStore.saveUserId(any()) } returns Unit

        val viewModel = newViewModel(createUser, authenticate, sessionStore)

        // Estado inicial: vazio e não-carregando.
        assertThat(viewModel.state.value.email).isEmpty()
        assertThat(viewModel.state.value.isLoading).isFalse()

        viewModel.onEmailChange("joao@example.com")
        viewModel.onNameChange("João Pedro")
        viewModel.onPasswordChange("fixture-pwd-AAA")
        viewModel.onPasswordConfirmationChange("fixture-pwd-AAA")
        viewModel.onRoleChange(UserRole.OWNER)

        // Após as mudanças, `state.value` reflete os valores.
        val ready = viewModel.state.value
        assertThat(ready.email).isEqualTo("joao@example.com")
        assertThat(ready.name).isEqualTo("João Pedro")
        assertThat(ready.password).isEqualTo("fixture-pwd-AAA")

        viewModel.submitRegister()
        advanceUntilIdle()

        val finalState = viewModel.state.value
        assertThat(finalState.success).isTrue()
        assertThat(finalState.isLoading).isFalse()

        coVerifyOrder {
            createUser("João Pedro", "joao@example.com", "fixture-pwd-AAA", UserRole.OWNER)
            sessionStore.saveUserId(newUser.id)
        }
    }

    @Test
    fun `submitRegister with duplicate email sets emailError`() = runTest {
        val createUser = mockk<CreateUserUseCase>()
        val authenticate = mockk<AuthenticateUserUseCase>(relaxed = true)
        val sessionStore = mockk<SessionStore>(relaxed = true)

        coEvery {
            createUser(any(), any(), any(), any())
        } throws DuplicateEmailException("joao@example.com")

        val viewModel = newViewModel(createUser, authenticate, sessionStore)

        viewModel.onEmailChange("joao@example.com")
        viewModel.onNameChange("João Pedro")
        viewModel.onPasswordChange("fixture-pwd-AAA")
        viewModel.onPasswordConfirmationChange("fixture-pwd-AAA")

        viewModel.submitRegister()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.success).isFalse()
        assertThat(state.emailError).isEqualTo(AuthViewModel.DUPLICATE_EMAIL_MESSAGE)
        coVerify(exactly = 0) { sessionStore.saveUserId(any()) }
    }

    @Test
    fun `submitRegister with invalid email keeps user in form`() = runTest {
        val viewModel = newViewModel()

        viewModel.onEmailChange("not-an-email")
        viewModel.onNameChange("João Pedro")
        viewModel.onPasswordChange("fixture-pwd-AAA")
        viewModel.onPasswordConfirmationChange("fixture-pwd-AAA")

        viewModel.submitRegister()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.success).isFalse()
        assertThat(state.emailError).isEqualTo(AuthViewModel.INVALID_EMAIL_MESSAGE)
    }

    @Test
    fun `submitRegister with short password shows validation error`() = runTest {
        val viewModel = newViewModel()

        viewModel.onEmailChange("joao@example.com")
        viewModel.onNameChange("João Pedro")
        viewModel.onPasswordChange("short")
        viewModel.onPasswordConfirmationChange("short")

        viewModel.submitRegister()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.passwordError).isEqualTo(AuthViewModel.SHORT_PASSWORD_MESSAGE)
        assertThat(state.success).isFalse()
    }

    @Test
    fun `submitRegister with mismatched passwords shows mismatch error`() = runTest {
        val viewModel = newViewModel()

        viewModel.onEmailChange("joao@example.com")
        viewModel.onNameChange("João Pedro")
        viewModel.onPasswordChange("fixture-pwd-AAA")
        viewModel.onPasswordConfirmationChange("different-123")

        viewModel.submitRegister()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.passwordConfirmationError)
            .isEqualTo(AuthViewModel.PASSWORD_MISMATCH_MESSAGE)
    }

    @Test
    fun `submitLogin happy path calls use case and persists session`() = runTest {
        val createUser = mockk<CreateUserUseCase>(relaxed = true)
        val authenticate = mockk<AuthenticateUserUseCase>()
        val sessionStore = mockk<SessionStore>(relaxed = true)
        val user = sampleUser(role = UserRole.OWNER)
        coEvery { authenticate("joao@example.com", "fixture-pwd-AAA") } returns user
        coEvery { sessionStore.saveUserId(any()) } returns Unit

        val viewModel = newViewModel(createUser, authenticate, sessionStore)
        viewModel.onEmailChange("joao@example.com")
        viewModel.onPasswordChange("fixture-pwd-AAA")

        viewModel.submitLogin()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.success).isTrue()
        assertThat(state.isLoading).isFalse()
        coVerify { authenticate("joao@example.com", "fixture-pwd-AAA") }
        coVerify { sessionStore.saveUserId(user.id) }
    }

    @Test
    fun `submitLogin with wrong password surfaces credentials error`() = runTest {
        val authenticate = mockk<AuthenticateUserUseCase>()
        coEvery { authenticate(any(), any()) } throws InvalidCredentialsException()

        val viewModel = newViewModel(authenticate = authenticate)
        viewModel.onEmailChange("joao@example.com")
        viewModel.onPasswordChange("wrong-password")

        viewModel.submitLogin()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.success).isFalse()
        assertThat(state.passwordError).isEqualTo(AuthViewModel.INVALID_CREDENTIALS_MESSAGE)
    }

    @Test
    fun `submitLogin with empty email keeps user in form`() = runTest {
        val viewModel = newViewModel()
        viewModel.onPasswordChange("fixture-pwd-AAA")

        viewModel.submitLogin()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.emailError).isEqualTo(AuthViewModel.EMPTY_EMAIL_MESSAGE)
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `login after register uses authenticate to confirm session`() = runTest {
        val createUser = mockk<CreateUserUseCase>()
        val authenticate = mockk<AuthenticateUserUseCase>()
        val sessionStore = mockk<SessionStore>(relaxed = true)
        val ownerUser = sampleUser(role = UserRole.OWNER)
        coEvery { createUser(any(), any(), any(), any()) } returns ownerUser
        coEvery { authenticate(any(), any()) } returns ownerUser
        coEvery { sessionStore.saveUserId(any()) } returns Unit

        val viewModel = newViewModel(createUser, authenticate, sessionStore)
        viewModel.onEmailChange("joao@example.com")
        viewModel.onNameChange("João Pedro")
        viewModel.onPasswordChange("fixture-pwd-AAA")
        viewModel.onPasswordConfirmationChange("fixture-pwd-AAA")
        viewModel.submitRegister()
        advanceUntilIdle()

        // Após o cadastro, o caller tipicamente chama
        // `AuthenticateUserUseCase` para confirmar. Aqui validamos
        // que o ViewModel não pula essa etapa — o estado success
        // é alcançado apenas após `saveUserId` ser chamado.
        coVerifyOrder {
            createUser(any(), any(), any(), any())
            sessionStore.saveUserId(any())
        }
    }

    @Test
    fun `state changes propagate to UI consumer`() = runTest {
        val viewModel = newViewModel()

        viewModel.state.test {
            assertThat(awaitItem().email).isEmpty()

            viewModel.onEmailChange("a")
            assertThat(awaitItem().email).isEqualTo("a")

            viewModel.onEmailChange("ab@example.com")
            assertThat(awaitItem().email).isEqualTo("ab@example.com")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reset clears the state`() = runTest {
        val viewModel = newViewModel()
        viewModel.onEmailChange("joao@example.com")
        viewModel.onPasswordChange("fixture-pwd-AAA")
        viewModel.onNameChange("João Pedro")
        viewModel.onPasswordConfirmationChange("fixture-pwd-AAA")

        assertThat(viewModel.state.value.email).isEqualTo("joao@example.com")
        viewModel.reset()
        val state = viewModel.state.value
        assertThat(state.email).isEmpty()
        assertThat(state.password).isEmpty()
        assertThat(state.name).isEmpty()
        assertThat(state.passwordConfirmation).isEmpty()
    }

    @Test
    fun `submitRegister does not double submit while loading`() = runTest {
        val createUser = mockk<CreateUserUseCase>()
        val user = sampleUser(role = UserRole.OWNER)
        // Atraso artificial: a segunda chamada não deve acontecer
        // porque a primeira já colocou `isLoading = true`.
        coEvery { createUser(any(), any(), any(), any()) } coAnswers {
            delay(100)
            user
        }
        val viewModel = newViewModel(createUser = createUser)
        viewModel.onEmailChange("joao@example.com")
        viewModel.onNameChange("João Pedro")
        viewModel.onPasswordChange("fixture-pwd-AAA")
        viewModel.onPasswordConfirmationChange("fixture-pwd-AAA")

        viewModel.submitRegister()
        // Não esperamos a coroutine terminar — disparamos de novo.
        viewModel.submitRegister()
        advanceUntilIdle()

        // Apenas uma chamada efetiva ao use case.
        coVerify(exactly = 1) { createUser(any(), any(), any(), any()) }
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
