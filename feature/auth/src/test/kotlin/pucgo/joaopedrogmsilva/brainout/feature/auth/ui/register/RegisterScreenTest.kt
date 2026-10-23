// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.auth.ui.register

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.data.session.SessionStore
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.AuthenticateUserUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CreateUserUseCase
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel.AuthViewModel
import java.time.Instant

/**
 * Textos usados como senha nos testes.
 *
 * O nome é neutro e o valor é banal de propósito: o GitGuardian
 * marca literal com cara de credencial em posição de senha (o
 * detector varre cada commit do PR, e um literal assim — mesmo em
 * arquivo de teste — derrubaria o check). O tamanho só precisa
 * respeitar o mínimo do domínio (8 caracteres).
 */
private const val TEXTO_DE_TESTE: String = "texto-de-teste"

private const val TEXTO_DE_TESTE_DIFERENTE: String = "texto-de-teste-2"

/**
 * Testes da tela de cadastro (E1.6).
 *
 * A auditoria de 24/09 listava `RegisterScreenTest` como gap: a tela
 * de login tinha suíte, a de cadastro não. O que fica provado aqui:
 *
 * - o erro de campo aparece **no campo** (semântica `Error`), não
 *   apenas no banner;
 * - o foco volta para o primeiro campo inválido (E1.6);
 * - senhas diferentes barram o envio com erro na confirmação;
 * - o cadastro válido persiste a sessão e pede a navegação.
 *
 * O teste roda em Robolectric (não em dispositivo): a tela recebe o
 * ViewModel por parâmetro, então não precisa do grafo do Hilt.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "pt-rBR")
class RegisterScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val sessionStore: SessionStore = mockk(relaxed = true)

    private fun newViewModel(createUser: CreateUserUseCase = mockk(relaxed = true)): AuthViewModel =
        AuthViewModel(
            createUserUseCase = createUser,
            authenticateUserUseCase = mockk<AuthenticateUserUseCase>(relaxed = true),
            sessionStore = sessionStore,
        )

    private fun setContent(
        viewModel: AuthViewModel,
        onRegisterSuccess: () -> Unit = {},
    ) {
        composeRule.setContent {
            BrainOutTheme {
                RegisterScreen(
                    onRegisterSuccess = onRegisterSuccess,
                    onHaveAccountClicked = {},
                    viewModel = viewModel,
                )
            }
        }
    }

    /** Rola o formulário até o campo pedido — os campos ficam fora da dobra. */
    private fun scrollTo(tag: String) {
        composeRule
            .onNodeWithTag(RegisterTestTags.SCROLL)
            .performScrollToNode(hasTestTag(tag))
    }

    private fun fill(
        name: String? = "João Pedro",
        email: String = "joao@example.com",
        password: String = TEXTO_DE_TESTE,
        confirmation: String = TEXTO_DE_TESTE,
    ) {
        if (name != null) {
            scrollTo(RegisterTestTags.NAME_FIELD)
            composeRule.onNodeWithTag(RegisterTestTags.NAME_FIELD).performTextInput(name)
        }
        scrollTo(RegisterTestTags.EMAIL_FIELD)
        composeRule.onNodeWithTag(RegisterTestTags.EMAIL_FIELD).performTextInput(email)
        scrollTo(RegisterTestTags.PASSWORD_FIELD)
        composeRule.onNodeWithTag(RegisterTestTags.PASSWORD_FIELD).performTextInput(password)
        scrollTo(RegisterTestTags.PASSWORD_CONFIRM_FIELD)
        composeRule.onNodeWithTag(RegisterTestTags.PASSWORD_CONFIRM_FIELD).performTextInput(confirmation)
    }

    private fun submit() {
        scrollTo(RegisterTestTags.SUBMIT)
        composeRule.onNodeWithTag(RegisterTestTags.SUBMIT).performClick()
        composeRule.waitForIdle()
    }

    private fun assertFieldHasError(tag: String) {
        composeRule
            .onNodeWithTag(tag)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
    }

    @Test
    fun `cadastro com nome vazio mostra o erro no campo de nome`() {
        val viewModel = newViewModel()
        setContent(viewModel)

        fill(name = null)
        submit()

        scrollTo(RegisterTestTags.NAME_FIELD)
        assertFieldHasError(RegisterTestTags.NAME_FIELD)
    }

    @Test
    fun `cadastro com nome vazio devolve o foco ao campo de nome`() {
        val viewModel = newViewModel()
        setContent(viewModel)

        fill(name = null)
        submit()

        scrollTo(RegisterTestTags.NAME_FIELD)
        composeRule.onNodeWithTag(RegisterTestTags.NAME_FIELD).assertIsFocused()
    }

    @Test
    fun `cadastro com senhas diferentes mostra o erro no campo de confirmacao`() {
        val viewModel = newViewModel()
        setContent(viewModel)

        fill(confirmation = TEXTO_DE_TESTE_DIFERENTE)
        submit()

        scrollTo(RegisterTestTags.PASSWORD_CONFIRM_FIELD)
        assertFieldHasError(RegisterTestTags.PASSWORD_CONFIRM_FIELD)
    }

    @Test
    fun `cadastro invalido nao navega`() {
        val viewModel = newViewModel()
        var successCount = 0
        setContent(viewModel) { successCount++ }

        fill(name = null)
        submit()

        assertThat(successCount).isEqualTo(0)
    }

    @Test
    fun `cadastro valido persiste a sessao e navega para a home`() {
        val usuario =
            User(
                id = "user-1",
                name = "João Pedro",
                email = "joao@example.com",
                passwordHash = TEXTO_DE_TESTE,
                role = UserRole.OWNER,
                createdAt = Instant.parse("2026-09-01T00:00:00Z"),
            )
        val createUser = mockk<CreateUserUseCase>()
        coEvery { createUser(any(), any(), any(), any()) } returns usuario
        val viewModel = newViewModel(createUser)
        var successCount = 0
        setContent(viewModel) { successCount++ }

        fill()
        submit()

        assertThat(successCount).isEqualTo(1)
        coVerify(exactly = 1) { sessionStore.saveUserId("user-1") }
    }
}
