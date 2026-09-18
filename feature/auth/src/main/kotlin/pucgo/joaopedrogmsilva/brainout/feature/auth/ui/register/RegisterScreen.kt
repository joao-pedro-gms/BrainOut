// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Tela de Cadastro — esqueleto navegável (E1.3). Validação fica para E1.6.

package pucgo.joaopedrogmsilva.brainout.feature.auth.ui.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.feature.auth.R

/**
 * Tela de cadastro — esqueleto navegável.
 *
 * @param onRegisterSubmit disparado quando o usuário toca em "Cadastrar".
 *  Navega para `home` no esqueleto E1.3; a lógica de validação real é
 *  entregue no marco E1.6.
 * @param onHaveAccountClicked disparado quando o usuário toca em
 *  "Já tenho conta" — volta para a rota `login` (sem limpar backstack,
 *  pois o usuário pode querer cancelar).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSubmit: () -> Unit,
    onHaveAccountClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.register_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        RegisterBody(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            onRegisterSubmit = onRegisterSubmit,
            onHaveAccountClicked = onHaveAccountClicked
        )
    }
}

/**
 * Conteúdo da tela de cadastro, isolado para manter a função pública
 * abaixo do limite de linhas do detekt (LongMethod).
 */
@Composable
private fun RegisterBody(
    onRegisterSubmit: () -> Unit,
    onHaveAccountClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.register_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        RegisterFormFields()
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onRegisterSubmit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.register_cta),
                style = MaterialTheme.typography.labelLarge
            )
        }
        TextButton(
            onClick = onHaveAccountClicked,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.register_have_account),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun RegisterFormFields() {
    OutlinedTextField(
        value = "",
        onValueChange = { /* placeholder E1.3 */ },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = stringResource(id = R.string.register_name_label)) },
        placeholder = { Text(text = stringResource(id = R.string.register_name_placeholder)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )
    OutlinedTextField(
        value = "",
        onValueChange = { /* placeholder E1.3 */ },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = stringResource(id = R.string.register_email_label)) },
        placeholder = { Text(text = stringResource(id = R.string.register_email_placeholder)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        )
    )
    OutlinedTextField(
        value = "",
        onValueChange = { /* placeholder E1.3 */ },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = stringResource(id = R.string.register_password_label)) },
        placeholder = { Text(text = stringResource(id = R.string.register_password_placeholder)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next
        )
    )
    OutlinedTextField(
        value = "",
        onValueChange = { /* placeholder E1.3 */ },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = stringResource(id = R.string.register_password_confirm_label)) },
        placeholder = {
            Text(text = stringResource(id = R.string.register_password_confirm_placeholder))
        },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        )
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RegisterScreenPreview() {
    RegisterScreen(
        onRegisterSubmit = {},
        onHaveAccountClicked = {}
    )
}
