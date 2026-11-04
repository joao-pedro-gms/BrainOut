// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Tela de Cadastro — formulário funcional com validação (E1.6).
// Owner/Member são selecionados via RadioButtons e mapeados para
// UserRole do domínio.

package pucgo.joaopedrogmsilva.brainout.feature.auth.ui.register

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSpacing
import pucgo.joaopedrogmsilva.brainout.feature.auth.R
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.AuthErrorBanner
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.AuthFormScaffold
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.AuthScreenScaffold
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.AuthSecondaryButton
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.AuthSubmitButton
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.AuthSubtitle
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.FocusFirstInvalidField
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.login.PasswordField
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.login.PasswordFieldConfig
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.login.UsernameField
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.login.UsernameFieldConfig
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.rememberAuthUiState
import pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel.AuthErrorType
import pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel.AuthUiState
import pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel.AuthViewModel

/**
 * Tela de cadastro — formulário funcional (E1.6).
 *
 * @param onRegisterSuccess disparado após cadastro bem-sucedido
 *  (consumido pelo `NavHost` para navegar para `home`).
 * @param onHaveAccountClicked disparado quando o usuário toca em
 *  "Já tenho conta" — volta para a rota `login`.
 * @param viewModel injetado pelo Hilt; pode ser substituído por um
 *  fake nos `@Preview`/testes.
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onHaveAccountClicked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState = rememberAuthUiState(viewModel, onRegisterSuccess)

    AuthScreenScaffold(titleRes = R.string.register_title, modifier = modifier) { innerPadding ->
        RegisterBody(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            uiState = uiState,
            callbacks =
                RegisterCallbacks(
                    onUsernameChange = viewModel::onUsernameChange,
                    onPasswordChange = viewModel::onPasswordChange,
                    onPasswordConfirmationChange = viewModel::onPasswordConfirmationChange,
                    onSubmit = { viewModel.submitRegister() },
                    onHaveAccountClicked = onHaveAccountClicked,
                ),
        )
    }
}

/** Identificadores usados por testes Compose. */
object RegisterTestTags {
    const val SCROLL: String = "register_scroll"
    const val USERNAME_FIELD: String = "register_username"
    const val PASSWORD_FIELD: String = "register_password"
    const val PASSWORD_CONFIRM_FIELD: String = "register_password_confirm"
    const val PASSWORD_TOGGLE: String = "register_password_toggle"
    const val PASSWORD_CONFIRM_TOGGLE: String = "register_password_confirm_toggle"
    const val SUBMIT: String = "register_submit"
    const val LOADING: String = "register_loading"
    const val ERROR_BANNER: String = "register_error"
    const val HAVE_ACCOUNT: String = "register_have_account"
}

/**
 * Callbacks do [RegisterBody] agrupados em um único parâmetro —
 * mantém a assinatura abaixo do limite de `LongParameterList` do
 * detekt.
 */
internal data class RegisterCallbacks(
    val onUsernameChange: (String) -> Unit,
    val onPasswordChange: (String) -> Unit,
    val onPasswordConfirmationChange: (String) -> Unit,
    val onSubmit: () -> Unit,
    val onHaveAccountClicked: () -> Unit,
)

/**
 * Conteúdo da tela de cadastro.
 *
 * [Modifier.imePadding] (DEF-27) vem **antes** do `verticalScroll` para que
 * a altura do teclado entre nas restrições da área rolável: em edge-to-edge
 * o IME não redimensiona a janela, então sem isso o último campo e o botão
 * de envio ficariam atrás do teclado em paisagem ou em tela baixa.
 */
@Composable
internal fun RegisterBody(
    callbacks: RegisterCallbacks,
    uiState: AuthUiState,
    modifier: Modifier = Modifier,
) {
    val usernameFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val passwordConfirmFocus = remember { FocusRequester() }

    AuthFormScaffold(scrollTestTag = RegisterTestTags.SCROLL, modifier = modifier) {
        AuthSubtitle(textRes = R.string.register_subtitle)

        RegisterIdentityFields(
            uiState = uiState,
            callbacks = callbacks,
            usernameFocus = usernameFocus,
            passwordFocus = passwordFocus,
        )

        RegisterPasswordFields(
            uiState = uiState,
            callbacks = callbacks,
            passwordFocus = passwordFocus,
            passwordConfirmFocus = passwordConfirmFocus,
        )

        RegisterActions(
            errorType = uiState.errorMessage,
            isLoading = uiState.isLoading,
            onSubmit = callbacks.onSubmit,
            onHaveAccountClicked = callbacks.onHaveAccountClicked,
        )
    }

    FocusFirstInvalidField(
        errors =
            listOf(
                uiState.usernameError,
                uiState.passwordError,
                uiState.passwordConfirmationError,
            ),
        requesters = listOf(usernameFocus, passwordFocus, passwordConfirmFocus),
    )
}

/**
 * Os campos de identidade: nome e e-mail.
 *
 * O nome é o único campo do cadastro que não tem componente próprio — o
 * `UsernameField` e o `PasswordField` são compartilhados com o login. Aqui
 * ele ganha um par, e o formulário passa a listar as suas seções.
 */
@Composable
private fun RegisterIdentityFields(
    uiState: AuthUiState,
    callbacks: RegisterCallbacks,
    usernameFocus: FocusRequester,
    passwordFocus: FocusRequester,
) {
    UsernameField(
        value = uiState.username,
        errorMessage = uiState.usernameError,
        onValueChange = callbacks.onUsernameChange,
        focusRequester = usernameFocus,
        imeAction = ImeAction.Next,
        onNext = { passwordFocus.requestFocus() },
        config =
            UsernameFieldConfig(
                labelId = R.string.register_username_label,
                placeholderId = R.string.register_username_placeholder,
                fieldTestTag = RegisterTestTags.USERNAME_FIELD,
            ),
    )
}

/** Os dois campos de senha: a senha e a confirmação. */
@Composable
private fun RegisterPasswordFields(
    uiState: AuthUiState,
    callbacks: RegisterCallbacks,
    passwordFocus: FocusRequester,
    passwordConfirmFocus: FocusRequester,
) {
    PasswordField(
        value = uiState.password,
        errorMessage = uiState.passwordError,
        onValueChange = callbacks.onPasswordChange,
        focusRequester = passwordFocus,
        imeAction = ImeAction.Next,
        onDone = { passwordConfirmFocus.requestFocus() },
        config =
            PasswordFieldConfig(
                labelId = R.string.register_pwd_label,
                placeholderId = R.string.register_pwd_placeholder,
                showToggleId = R.string.register_visibility_show,
                hideToggleId = R.string.register_visibility_hide,
                fieldTestTag = RegisterTestTags.PASSWORD_FIELD,
                toggleTestTag = RegisterTestTags.PASSWORD_TOGGLE,
            ),
    )

    PasswordField(
        value = uiState.passwordConfirmation,
        errorMessage = uiState.passwordConfirmationError,
        onValueChange = callbacks.onPasswordConfirmationChange,
        focusRequester = passwordConfirmFocus,
        imeAction = ImeAction.Done,
        onDone = callbacks.onSubmit,
        config =
            PasswordFieldConfig(
                labelId = R.string.register_pwd_confirm_label,
                placeholderId = R.string.register_pwd_confirm_placeholder,
                showToggleId = R.string.register_visibility_show,
                hideToggleId = R.string.register_visibility_hide,
                fieldTestTag = RegisterTestTags.PASSWORD_CONFIRM_FIELD,
                toggleTestTag = RegisterTestTags.PASSWORD_CONFIRM_TOGGLE,
            ),
    )
}

/** O erro do envio, o botão de envio e o atalho para o login. */
@Composable
private fun RegisterActions(
    errorType: AuthErrorType?,
    isLoading: Boolean,
    onSubmit: () -> Unit,
    onHaveAccountClicked: () -> Unit,
) {
    AuthErrorBanner(errorType = errorType, testTag = RegisterTestTags.ERROR_BANNER)

    Spacer(modifier = Modifier.height(NeoSpacing.xs))
    AuthSubmitButton(
        labelRes = R.string.register_cta,
        isLoading = isLoading,
        testTag = RegisterTestTags.SUBMIT,
        loadingTestTag = RegisterTestTags.LOADING,
        onClick = onSubmit,
    )
    AuthSecondaryButton(
        labelRes = R.string.register_have_account,
        enabled = !isLoading,
        testTag = RegisterTestTags.HAVE_ACCOUNT,
        onClick = onHaveAccountClicked,
    )
}

/**
 * Preview do formulário de Cadastro.
 *
 * Aponta para [RegisterBody] (e não para [RegisterScreen]) de
 * propósito: a tela resolve o ViewModel por `hiltViewModel()`, que
 * exige uma Activity — no IDE a preview simplesmente não renderizava.
 * O corpo recebe um estado de exemplo e renderiza igual.
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RegisterScreenPreview() {
    BrainOutTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            RegisterBody(
                callbacks =
                    RegisterCallbacks(
                        onUsernameChange = {},
                        onPasswordChange = {},
                        onPasswordConfirmationChange = {},
                        onSubmit = {},
                        onHaveAccountClicked = {},
                    ),
                uiState = AuthUiState(username = "Joao"),
            )
        }
    }
}
