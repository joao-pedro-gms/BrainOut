// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel.AuthEvent
import pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel.AuthUiState
import pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel.AuthViewModel

/**
 * Observa o [AuthViewModel] e devolve o estado do formulário.
 *
 * Login e cadastro reagem ao **mesmo** evento ([AuthEvent.NavigateHome]);
 * o que muda entre as duas telas é apenas o que fazer quando ele chega.
 * O vínculo entre o ViewModel e a tela — coletar o estado e consumir o
 * evento — é, portanto, o mesmo, e mora aqui uma única vez.
 *
 * @param viewModel o ViewModel da tela.
 * @param onNavigateHome disparado quando o evento de navegação chega.
 * @return o estado atual do formulário.
 */
@Composable
internal fun rememberAuthUiState(
    viewModel: AuthViewModel,
    onNavigateHome: () -> Unit,
): AuthUiState {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is AuthEvent.NavigateHome) {
                onNavigateHome()
            }
        }
    }

    return uiState
}
