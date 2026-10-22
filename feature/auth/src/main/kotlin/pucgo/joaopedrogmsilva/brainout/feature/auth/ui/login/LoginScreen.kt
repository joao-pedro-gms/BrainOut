// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Tela de Login — formulário funcional com validação (E1.6).
// Marcos E1.7 e E1.8 são cobertos via SessionViewModel em :feature:auth
// e SessionStore em :core:data.

package pucgo.joaopedrogmsilva.brainout.feature.auth.ui.login

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.feature.auth.R
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.AuthErrorBanner
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.AuthFormScaffold
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.AuthSecondaryButton
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.AuthSubmitButton
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.AuthSubtitle
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.FocusFirstInvalidField
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.text
import pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel.AuthErrorType
import pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel.AuthEvent
import pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel.AuthUiState
import pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel.AuthViewModel
import pucgo.joaopedrogmsilva.brainout.core.ui.R as UiR

/**
 * Tela de login — formulário funcional (E1.6).
 *
 * O estado é observado do [AuthViewModel]. As ações do usuário
 * disparam `on*Change`/`submit*` no ViewModel, que centraliza a
 * validação e a chamada ao caso de uso.
 *
 * @param onLoginSubmit disparado após login/cadastro bem-sucedido
 *  (consumido pelo `NavHost` para navegar para `home`).
 * @param onCreateAccountClicked disparado quando o usuário toca em
 *  "Criar conta" — navega para a rota `register`.
 * @param viewModel injetado pelo Hilt; pode ser substituído por um
 *  fake nos `@Preview`/testes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSubmit: () -> Unit,
    onCreateAccountClicked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is AuthEvent.NavigateHome) {
                onLoginSubmit()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.login_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LoginBody(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            uiState = uiState,
            callbacks =
                LoginCallbacks(
                    onEmailChange = viewModel::onEmailChange,
                    onPasswordChange = viewModel::onPasswordChange,
                    onSubmit = { viewModel.submitLogin() },
                    onCreateAccountClicked = onCreateAccountClicked,
                ),
        )
    }
}

/**
 * Conteúdo da tela de login, isolado para manter a função public abaixo
 * do limite de linhas do detekt (LongMethod).
 *
 * [Modifier.imePadding] (DEF-27) vem **antes** do `verticalScroll` para que
 * a altura do teclado entre nas restrições da área rolável: em edge-to-edge
 * o IME não redimensiona a janela, então sem isso o campo de senha e o botão
 * de envio ficariam atrás do teclado em paisagem ou em tela baixa.
 */
@Suppress("LongMethod")
@Composable
internal fun LoginBody(
    callbacks: LoginCallbacks,
    uiState: AuthUiState,
    modifier: Modifier = Modifier,
) {
    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }

    AuthFormScaffold(scrollTestTag = LoginTestTags.SCROLL, modifier = modifier) {
        AuthSubtitle(textRes = R.string.login_subtitle)

        EmailField(
            value = uiState.email,
            errorMessage = uiState.emailError,
            onValueChange = callbacks.onEmailChange,
            focusRequester = emailFocus,
            imeAction = ImeAction.Next,
            onNext = { passwordFocus.requestFocus() },
        )

        PasswordField(
            value = uiState.password,
            errorMessage = uiState.passwordError,
            onValueChange = callbacks.onPasswordChange,
            focusRequester = passwordFocus,
            imeAction = ImeAction.Done,
            onDone = callbacks.onSubmit,
            labels =
                PasswordFieldLabels(
                    labelId = R.string.login_pwd_label,
                    placeholderId = R.string.login_pwd_placeholder,
                    showToggleId = R.string.login_visibility_show,
                    hideToggleId = R.string.login_visibility_hide,
                ),
            tags =
                PasswordFieldTags(
                    field = LoginTestTags.PASSWORD_FIELD,
                    toggle = LoginTestTags.PASSWORD_TOGGLE,
                ),
        )

        AuthErrorBanner(errorType = uiState.errorMessage, testTag = LoginTestTags.ERROR_BANNER)

        Spacer(modifier = Modifier.height(8.dp))
        AuthSubmitButton(
            labelRes = R.string.login_cta,
            isLoading = uiState.isLoading,
            testTag = LoginTestTags.SUBMIT,
            loadingTestTag = LoginTestTags.LOADING,
            onClick = callbacks.onSubmit,
        )
        AuthSecondaryButton(
            labelRes = R.string.login_create_account,
            enabled = !uiState.isLoading,
            testTag = LoginTestTags.CREATE_ACCOUNT,
            onClick = callbacks.onCreateAccountClicked,
        )
    }

    // Reage a mudanças em emailError/passwordError para devolver o foco
    // ao primeiro campo inválido, como exigido pelo roadmap E1.6.
    FocusFirstInvalidField(
        errors = listOf(uiState.emailError, uiState.passwordError),
        requesters = listOf(emailFocus, passwordFocus),
    )
}

/** Identificadores usados por testes Compose. */
object LoginTestTags {
    const val SCROLL: String = "login_scroll"
    const val EMAIL_FIELD: String = "login_email"
    const val PASSWORD_FIELD: String = "login_password"
    const val PASSWORD_TOGGLE: String = "login_visibility_toggle"
    const val SUBMIT: String = "login_submit"
    const val LOADING: String = "login_loading"
    const val ERROR_BANNER: String = "login_error"
    const val CREATE_ACCOUNT: String = "login_create_account"
}

/**
 * Callbacks do [LoginBody] agrupados em um único parâmetro —
 * mantém a assinatura abaixo do limite de `LongParameterList` do
 * detekt.
 */
internal data class LoginCallbacks(
    val onEmailChange: (String) -> Unit,
    val onPasswordChange: (String) -> Unit,
    val onSubmit: () -> Unit,
    val onCreateAccountClicked: () -> Unit,
)

/** Toggle para tornar o campo de senha persistente entre composições. */
@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PasswordField(
    value: String,
    errorMessage: AuthErrorType?,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    imeAction: ImeAction,
    onDone: () -> Unit,
    labels: PasswordFieldLabels,
    tags: PasswordFieldTags,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val showDescription = stringResource(id = labels.showToggleId)
    val hideDescription = stringResource(id = labels.hideToggleId)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .testTag(tags.field),
        label = { Text(text = stringResource(id = labels.labelId)) },
        placeholder = { Text(text = stringResource(id = labels.placeholderId)) },
        singleLine = true,
        visualTransformation =
            if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = imeAction,
            ),
        keyboardActions =
            KeyboardActions(
                onDone = { onDone() },
                onNext = { onDone() },
                onGo = { onDone() },
            ),
        isError = errorMessage != null,
        supportingText =
            errorMessage?.let { errorType ->
                { Text(text = errorType.text()) }
            },
        trailingIcon = {
            IconButton(
                onClick = { passwordVisible = !passwordVisible },
                modifier =
                    Modifier
                        .testTag(tags.toggle)
                        .semantics {
                            contentDescription =
                                if (passwordVisible) {
                                    hideDescription
                                } else {
                                    showDescription
                                }
                        },
            ) {
                Icon(
                    painter =
                        if (passwordVisible) {
                            painterResource(UiR.drawable.neo_ic_visibility_off)
                        } else {
                            painterResource(UiR.drawable.neo_ic_visibility)
                        },
                    contentDescription =
                        if (passwordVisible) {
                            hideDescription
                        } else {
                            showDescription
                        },
                )
            }
        },
    )
}

/**
 * Conjunto de strings (resource ids) usadas pelo [PasswordField].
 * Agrupadas em um único parâmetro para manter a assinatura da
 * função abaixo do limite de `LongParameterList` do detekt.
 */
internal data class PasswordFieldLabels(
    @androidx.annotation.StringRes val labelId: Int,
    @androidx.annotation.StringRes val placeholderId: Int,
    @androidx.annotation.StringRes val showToggleId: Int,
    @androidx.annotation.StringRes val hideToggleId: Int,
)

/** Tags de teste Compose aplicadas ao [PasswordField]. */
internal data class PasswordFieldTags(
    val field: String,
    val toggle: String,
)

/**
 * Wrapper interno do campo de e-mail para reuso entre Login e Register
 * sem duplicar lógica.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EmailField(
    value: String,
    errorMessage: AuthErrorType?,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    imeAction: ImeAction,
    onNext: () -> Unit,
    config: EmailFieldConfig = EmailFieldConfig.DEFAULT_LOGIN,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .testTag(config.fieldTestTag),
        label = { Text(text = stringResource(id = config.labelId)) },
        placeholder = { Text(text = stringResource(id = config.placeholderId)) },
        singleLine = true,
        isError = errorMessage != null,
        supportingText =
            errorMessage?.let { errorType ->
                { Text(text = errorType.text()) }
            },
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = imeAction,
            ),
        keyboardActions =
            KeyboardActions(
                onNext = { onNext() },
                onDone = { onNext() },
            ),
    )
}

/** Configuração textual + tag do [EmailField]. */
internal data class EmailFieldConfig(
    @androidx.annotation.StringRes val labelId: Int,
    @androidx.annotation.StringRes val placeholderId: Int,
    val fieldTestTag: String,
) {
    companion object {
        val DEFAULT_LOGIN: EmailFieldConfig =
            EmailFieldConfig(
                labelId = pucgo.joaopedrogmsilva.brainout.feature.auth.R.string.login_email_label,
                placeholderId = pucgo.joaopedrogmsilva.brainout.feature.auth.R.string.login_email_placeholder,
                fieldTestTag = LoginTestTags.EMAIL_FIELD,
            )
    }
}

/**
 * Preview do formulário de Login.
 *
 * Aponta para [LoginBody] (e não para [LoginScreen]) de propósito: a
 * tela resolve o ViewModel por `hiltViewModel()`, que exige uma
 * Activity — no IDE a preview simplesmente não renderizava. O corpo
 * recebe um estado de exemplo e renderiza igual.
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    BrainOutTheme {
        LoginBody(
            callbacks =
                LoginCallbacks(
                    onEmailChange = {},
                    onPasswordChange = {},
                    onSubmit = {},
                    onCreateAccountClicked = {},
                ),
            uiState = AuthUiState(email = "joao@example.com", password = "senha-de-exemplo"),
        )
    }
}
