// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Estados de feedback da Home (E2.8): loader centralizado, banner de
// erro com retry/dispensar e resolução da chave canônica do ViewModel
// para string localizada. Sem regra de negócio; só apresentação.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

/**
 * Loader da Home (E2.8). Centralizado e com `testTag` para os
 * testes Compose. Mantemos a área de toque ≥ 48dp no `Box` para
 * consistência com a diretriz E4.4.
 */
@Composable
internal fun HomeLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 240.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.testTag(HomeTestTags.LOADING),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Text(
                text = stringResource(id = R.string.home_loading_aria_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Banner de erro com ações de retry/dispensar (E2.8). Renderizado
 * sempre que `errorMessage != null`. A mensagem vinda do ViewModel
 * é uma chave canônica (`ERROR_LOAD_FAILED` ou
 * `ERROR_ACTION_FAILED`); a UI resolve para o recurso localizado
 * via [resolveHomeErrorMessage].
 */
@Composable
internal fun HomeErrorBanner(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.testTag(HomeTestTags.ERROR_BANNER),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = resolveHomeErrorMessage(message),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .testTag(HomeTestTags.ERROR_RETRY)
                        // E4.4: 48dp mínimo WCAG 2.5.5.
                        .heightIn(min = 48.dp),
                ) {
                    Text(text = stringResource(id = R.string.home_error_retry))
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .testTag(HomeTestTags.ERROR_DISMISS)
                        .heightIn(min = 48.dp),
                ) {
                    Text(text = stringResource(id = R.string.home_error_dismiss))
                }
            }
        }
    }
}

/**
 * Resolve a chave canônica de erro do [HomeViewModel] para a string
 * localizada. Mesma estratégia de `resolveProjectDetailMessage` em
 * E2.1 — chaves técnicas do ViewModel viram recurso pt/en sem
 * expor stack traces para o usuário.
 */
@Composable
internal fun resolveHomeErrorMessage(message: String): String = when {
    HomeViewModel.isLoadErrorMessage(message) ->
        stringResource(id = R.string.home_error_load_failed)
    message == HomeViewModel.ERROR_ACTION_FAILED ->
        stringResource(id = R.string.home_error_action_failed)
    else -> message
}
