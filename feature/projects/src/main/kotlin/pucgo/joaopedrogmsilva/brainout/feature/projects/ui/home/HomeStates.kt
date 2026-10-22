// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Estados de feedback da Home (E2.8): loader centralizado, banner de
// erro com retry/dispensar e resolução da chave canônica do ViewModel
// para string localizada. Sem regra de negócio; só apresentação.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoErrorBanner
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSpacing
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

/**
 * Loader da Home (E2.8). Centralizado e com `testTag` para os
 * testes Compose. Mantemos a área de toque ≥ 48dp no `Box` para
 * consistência com a diretriz E4.4.
 */
@Composable
internal fun HomeLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .heightIn(min = 240.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NeoSpacing.sm),
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
 * sempre que há erro. O erro chega **tipado**
 * ([HomeErrorType]) e a UI resolve para o recurso localizado via
 * [resolveHomeErrorMessage].
 */
@Composable
internal fun HomeErrorBanner(
    error: HomeErrorType,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NeoErrorBanner(
        message = resolveHomeErrorMessage(error),
        onRetry = onRetry,
        onDismiss = onDismiss,
        modifier = modifier,
        // O banner da Home não reservava altura mínima; manter o
        // comportamento original evita uma mudança visual silenciosa.
        minHeight = 0.dp,
    )
}

/**
 * Resolve o erro do [HomeViewModel] para a string localizada.
 *
 * O ViewModel entrega o **tipo** do erro; aqui a escolha do texto
 * pt/en. Antes o tipo era deduzido comparando a mensagem em
 * português.
 */
@Composable
internal fun resolveHomeErrorMessage(error: HomeErrorType): String =
    when (error) {
        HomeErrorType.LoadFailed ->
            stringResource(id = R.string.home_error_load_failed)
        HomeErrorType.ActionFailed ->
            stringResource(id = R.string.home_error_action_failed)
        HomeErrorType.NameRequired ->
            stringResource(id = R.string.home_create_project_name_required)
        HomeErrorType.DuplicateTagName ->
            stringResource(id = R.string.home_error_duplicate_tag_name)
    }
