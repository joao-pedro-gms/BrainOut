// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Tela de Cadastro — formulário funcional com validação (E1.6).
// Owner/Member são selecionados via RadioButtons e mapeados para
// UserRole do domínio.

package pucgo.joaopedrogmsilva.brainout.feature.auth.ui.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole
import pucgo.joaopedrogmsilva.brainout.feature.auth.R
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.login.EmailField
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.login.EmailFieldConfig
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.login.PasswordField
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.login.PasswordFieldLabels
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.login.PasswordFieldTags
import pucgo.joaopedrogmsilva.brainout.feature.auth.ui.login.resolveAuthMessage
import pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel.AuthEvent
import pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel.AuthUiState
import pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel.AuthViewModel

/**
 * Tela de cadastro — formulário funcional (E1.6).
 *
 * @param onRegisterSubmit disparado após cadastro bem-sucedido
 *  (consumido pelo `NavHost` para navegar para `home`).
 * @param onHaveAccountClicked disparado quando o usuário toca em
 *  "Já tenho conta" — volta para a rota `login`.
 * @param viewModel injetado pelo Hilt; pode ser substituído por um
 *  fake nos `@Preview`/testes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSubmit: () -> Unit,
    onHaveAccountClicked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is AuthEvent.NavigateHome) {
                onRegisterSubmit()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.register_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        RegisterBody(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            uiState = uiState,
            callbacks = RegisterCallbacks(
                onNameChange = viewModel::onNameChange,
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                onPasswordConfirmationChange = viewModel::onPasswordConfirmationChange,
                onRoleChange = viewModel::onRoleChange,
                onSubmit = { viewModel.submitRegister() },
                onHaveAccountClicked = onHaveAccountClicked,
            ),
        )
    }
}

/** Identificadores usados por testes Compose. */
object RegisterTestTags {
    const val SCROLL: String = "register_scroll"
    const val NAME_FIELD: String = "register_name"
    const val EMAIL_FIELD: String = "register_email"
    const val PASSWORD_FIELD: String = "register_password"
    const val PASSWORD_CONFIRM_FIELD: String = "register_visibility_confirm"
    const val PASSWORD_TOGGLE: String = "register_visibility_toggle"
    const val PASSWORD_CONFIRM_TOGGLE: String = "register_visibility_confirm_toggle"
    const val ROLE_OWNER: String = "register_role_owner"
    const val ROLE_MEMBER: String = "register_role_member"
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
    val onNameChange: (String) -> Unit,
    val onEmailChange: (String) -> Unit,
    val onPasswordChange: (String) -> Unit,
    val onPasswordConfirmationChange: (String) -> Unit,
    val onRoleChange: (UserRole) -> Unit,
    val onSubmit: () -> Unit,
    val onHaveAccountClicked: () -> Unit,
)

@Suppress("LongMethod")
@Composable
internal fun RegisterBody(
    callbacks: RegisterCallbacks,
    uiState: AuthUiState,
    modifier: Modifier = Modifier,
) {
    val nameFocus = remember { FocusRequester() }
    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val passwordConfirmFocus = remember { FocusRequester() }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag(RegisterTestTags.SCROLL),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(id = R.string.register_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = uiState.name,
            onValueChange = callbacks.onNameChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(nameFocus)
                .testTag(RegisterTestTags.NAME_FIELD),
            label = { Text(text = stringResource(id = R.string.register_name_label)) },
            placeholder = { Text(text = stringResource(id = R.string.register_name_placeholder)) },
            singleLine = true,
            isError = uiState.nameError != null,
            supportingText = uiState.nameError?.let { msg ->
                { Text(text = resolveAuthMessage(msg)) }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { emailFocus.requestFocus() },
                onDone = { emailFocus.requestFocus() },
            ),
        )

        EmailField(
            value = uiState.email,
            errorMessage = uiState.emailError,
            onValueChange = callbacks.onEmailChange,
            focusRequester = emailFocus,
            imeAction = ImeAction.Next,
            onNext = { passwordFocus.requestFocus() },
            config = EmailFieldConfig(
                labelId = R.string.register_email_label,
                placeholderId = R.string.register_email_placeholder,
                fieldTestTag = RegisterTestTags.EMAIL_FIELD,
            ),
        )

        PasswordField(
            value = uiState.password,
            errorMessage = uiState.passwordError,
            onValueChange = callbacks.onPasswordChange,
            focusRequester = passwordFocus,
            imeAction = ImeAction.Next,
            onDone = { passwordConfirmFocus.requestFocus() },
            labels = PasswordFieldLabels(
                labelId = R.string.register_pwd_label,
                placeholderId = R.string.register_pwd_placeholder,
                showToggleId = R.string.register_visibility_show,
                hideToggleId = R.string.register_visibility_hide,
            ),
            tags = PasswordFieldTags(
                field = RegisterTestTags.PASSWORD_FIELD,
                toggle = RegisterTestTags.PASSWORD_TOGGLE,
            ),
        )

        PasswordField(
            value = uiState.passwordConfirmation,
            errorMessage = uiState.passwordConfirmationError,
            onValueChange = callbacks.onPasswordConfirmationChange,
            focusRequester = passwordConfirmFocus,
            imeAction = ImeAction.Done,
            onDone = callbacks.onSubmit,
            labels = PasswordFieldLabels(
                labelId = R.string.register_pwd_confirm_label,
                placeholderId = R.string.register_pwd_confirm_placeholder,
                showToggleId = R.string.register_visibility_show,
                hideToggleId = R.string.register_visibility_hide,
            ),
            tags = PasswordFieldTags(
                field = RegisterTestTags.PASSWORD_CONFIRM_FIELD,
                toggle = RegisterTestTags.PASSWORD_CONFIRM_TOGGLE,
            ),
        )

        RoleSelector(
            selected = uiState.selectedRole,
            onSelect = callbacks.onRoleChange,
        )

        uiState.errorMessage?.let { message ->
            val resolvedMessage = resolveAuthMessage(message)
            Text(
                text = resolvedMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(RegisterTestTags.ERROR_BANNER),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = callbacks.onSubmit,
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(RegisterTestTags.SUBMIT),
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .height(20.dp)
                        .testTag(RegisterTestTags.LOADING),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    text = stringResource(id = R.string.register_cta),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        TextButton(
            onClick = callbacks.onHaveAccountClicked,
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(RegisterTestTags.HAVE_ACCOUNT),
        ) {
            Text(
                text = stringResource(id = R.string.register_have_account),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }

    LaunchedEffect(
        uiState.nameError,
        uiState.emailError,
        uiState.passwordError,
        uiState.passwordConfirmationError,
    ) {
        when {
            uiState.nameError != null -> runCatching { nameFocus.requestFocus() }
            uiState.emailError != null -> runCatching { emailFocus.requestFocus() }
            uiState.passwordError != null -> runCatching { passwordFocus.requestFocus() }
            uiState.passwordConfirmationError != null ->
                runCatching { passwordConfirmFocus.requestFocus() }
        }
    }
}

@Composable
private fun RoleSelector(
    selected: UserRole,
    onSelect: (UserRole) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RoleRow(
            title = stringResource(id = R.string.register_role_owner),
            help = stringResource(id = R.string.register_role_owner_help),
            selected = selected == UserRole.OWNER,
            onSelect = { onSelect(UserRole.OWNER) },
            testTag = RegisterTestTags.ROLE_OWNER,
        )
        RoleRow(
            title = stringResource(id = R.string.register_role_member),
            help = stringResource(id = R.string.register_role_member_help),
            selected = selected == UserRole.MEMBER,
            onSelect = { onSelect(UserRole.MEMBER) },
            testTag = RegisterTestTags.ROLE_MEMBER,
        )
    }
}

@Composable
private fun RoleRow(
    title: String,
    help: String,
    selected: Boolean,
    onSelect: () -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = SemanticsRole.RadioButton,
                onClick = onSelect,
            )
            .testTag(testTag)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = help,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RegisterScreenPreview() {
    RegisterScreen(
        onRegisterSubmit = {},
        onHaveAccountClicked = {},
    )
}
