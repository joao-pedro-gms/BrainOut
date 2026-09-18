// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Tela de Login — esqueleto navegável (E1.3). Validação fica para E1.6.

package pucgo.joaopedrogmsilva.brainout.feature.auth.ui.login

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
 * Tela de login — esqueleto navegável.
 *
 * @param onLoginSubmit disparado quando o usuário toca em "Entrar". No E1.3
 *  já navega para `home` (placeholder); em E1.6 fará validação antes.
 * @param onCreateAccountClicked disparado quando o usuário toca em
 *  "Criar conta" e deve navegar para a rota `register`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSubmit: () -> Unit,
    onCreateAccountClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.login_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LoginBody(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            onLoginSubmit = onLoginSubmit,
            onCreateAccountClicked = onCreateAccountClicked
        )
    }
}

/**
 * Conteúdo da tela de login, isolado para manter a função pública abaixo
 * do limite de linhas do detekt (LongMethod).
 */
@Composable
private fun LoginBody(
    onLoginSubmit: () -> Unit,
    onCreateAccountClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LoginFormFields()
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onLoginSubmit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.login_cta),
                style = MaterialTheme.typography.labelLarge
            )
        }
        TextButton(
            onClick = onCreateAccountClicked,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.login_create_account),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun LoginFormFields() {
    OutlinedTextField(
        value = "",
        onValueChange = { /* placeholder E1.3 — wiring real fica em E1.6 */ },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = stringResource(id = R.string.login_email_label)) },
        placeholder = { Text(text = stringResource(id = R.string.login_email_placeholder)) },
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
        label = { Text(text = stringResource(id = R.string.login_password_label)) },
        placeholder = { Text(text = stringResource(id = R.string.login_password_placeholder)) },
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
private fun LoginScreenPreview() {
    LoginScreen(
        onLoginSubmit = {},
        onCreateAccountClicked = {}
    )
}
