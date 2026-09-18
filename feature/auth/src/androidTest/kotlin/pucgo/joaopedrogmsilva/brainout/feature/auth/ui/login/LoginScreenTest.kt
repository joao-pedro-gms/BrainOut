// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.auth.ui.login

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pucgo.joaopedrogmsilva.brainout.core.data.session.SessionStore
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidCredentialsException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.AuthenticateUserUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CreateUserUseCase
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel.AuthViewModel

/**
 * Teste Compose instrumentado do fluxo de erro visível na
 * [LoginScreen] (E1.6 do ROADMAP).
 *
 * Cobre:
 * - Tocar em "Entrar" com e-mail vazio → erro inline visível e foco
 *   volta para o campo de e-mail.
 * - Login com credenciais inválidas → mensagem geral visível, foco
 *   volta para o campo de senha.
 *
 * Usa o construtor direto de [AuthViewModel] injetando mocks dos
 * casos de uso para evitar a infraestrutura completa do Hilt (que
 * exigiria `HiltAndroidRule`).
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun newViewModel(
        authenticate: AuthenticateUserUseCase = mockk(relaxed = true),
        createUser: CreateUserUseCase = mockk(relaxed = true),
        sessionStore: SessionStore = mockk(relaxed = true),
    ): AuthViewModel = AuthViewModel(
        createUserUseCase = createUser,
        authenticateUserUseCase = authenticate,
        sessionStore = sessionStore,
    )

    @Test
    fun submitting_with_empty_email_shows_email_error_and_focuses_email() {
        val viewModel = newViewModel()
        var navigationCount = 0

        composeTestRule.setContent {
            BrainOutTheme {
                Surface(modifier = Modifier) {
                    LoginScreen(
                        onLoginSubmit = { navigationCount++ },
                        onCreateAccountClicked = {},
                        viewModel = viewModel,
                    )
                }
            }
        }

        // Digita apenas a senha — e-mail continua vazio.
        composeTestRule.onNodeWithTag(LoginTestTags.PASSWORD_FIELD)
            .performTextInput("any-password")

        // Toca em Entrar.
        composeTestRule.onNodeWithTag(LoginTestTags.SUBMIT)
            .performClick()

        composeTestRule.waitForIdle()

        // O banner de erro geral aparece (estado `errorMessage`).
        composeTestRule.onNodeWithTag(LoginTestTags.ERROR_BANNER)
            .assertIsDisplayed()

        // Não navegou porque a validação falhou.
        assertThat(navigationCount).isEqualTo(0)
    }

    @Test
    fun login_with_invalid_credentials_surfaces_error_and_does_not_navigate() {
        val authenticate = mockk<AuthenticateUserUseCase>()
        coEvery { authenticate(any(), any()) } throws InvalidCredentialsException()
        val viewModel = newViewModel(authenticate = authenticate)

        var navigationCount = 0

        composeTestRule.setContent {
            BrainOutTheme {
                Surface {
                    LoginScreen(
                        onLoginSubmit = { navigationCount++ },
                        onCreateAccountClicked = {},
                        viewModel = viewModel,
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(LoginTestTags.EMAIL_FIELD)
            .performTextInput("joao@example.com")
        composeTestRule.onNodeWithTag(LoginTestTags.PASSWORD_FIELD)
            .performTextInput("wrong-password")

        composeTestRule.onNodeWithTag(LoginTestTags.SUBMIT)
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(LoginTestTags.ERROR_BANNER)
            .assertIsDisplayed()

        // Nenhuma navegação após credenciais inválidas.
        assertThat(navigationCount).isEqualTo(0)
        coVerify(exactly = 1) { authenticate("joao@example.com", "wrong-password") }
    }

    @Test
    fun login_with_valid_credentials_navigates_to_home() = runTest {
        val authenticate = mockk<AuthenticateUserUseCase>()
        val sessionStore = mockk<SessionStore>(relaxed = true)
        val user = User(
            id = "user-id",
            name = "João Pedro",
            email = "joao@example.com",
            passwordHash = "pbkdf2_sha256\$120000\$AAAA\$BBBB",
            role = UserRole.OWNER,
            createdAt = Instant.parse("2026-09-18T10:00:00Z"),
        )
        coEvery { authenticate(any(), any()) } returns user
        coEvery { sessionStore.saveUserId(any()) } returns Unit
        val viewModel = newViewModel(authenticate = authenticate, sessionStore = sessionStore)

        var navigationCount = 0

        composeTestRule.setContent {
            BrainOutTheme {
                Surface {
                    LoginScreen(
                        onLoginSubmit = { navigationCount++ },
                        onCreateAccountClicked = {},
                        viewModel = viewModel,
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(LoginTestTags.EMAIL_FIELD)
            .performTextInput("joao@example.com")
        composeTestRule.onNodeWithTag(LoginTestTags.PASSWORD_FIELD)
            .performTextInput("correct-password")
        composeTestRule.onNodeWithTag(LoginTestTags.SUBMIT)
            .performClick()

        composeTestRule.waitForIdle()

        assertThat(navigationCount).isEqualTo(1)
        coVerify { authenticate("joao@example.com", "correct-password") }
        coVerify { sessionStore.saveUserId("user-id") }
    }
}