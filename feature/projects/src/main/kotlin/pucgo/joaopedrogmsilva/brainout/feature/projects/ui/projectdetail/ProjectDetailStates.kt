// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Estados de feedback da ProjectDetail (E2.8): loader centralizado,
// banner de erro com retry/dispensar, card de "sem tarefas" e
// resolução da chave canônica do ViewModel para string localizada.
// Espelha `HomeStates` em :feature:projects/ui/home.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.MAX_ACTIVE_TASKS_PER_PROJECT
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

/**
 * Loader centralizado da ProjectDetail (E2.8). Mantém paridade visual
 * com `HomeLoadingState` em :feature:projects/ui/home.
 */
@Composable
internal fun ProjectDetailLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp)
            .testTag(ProjectDetailTestTags.LOADING),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.project_detail_loading_aria_label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Banner de erro com retry/dispensar (E2.8). Espelha `HomeErrorBanner`
 * em estrutura visual.
 */
@Composable
internal fun ProjectDetailErrorBanner(
    @Suppress("UNUSED_PARAMETER") message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .testTag(ProjectDetailTestTags.ERROR_BANNER),
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
                text = stringResource(id = R.string.project_detail_error_load_failed),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .testTag(ProjectDetailTestTags.ERROR_RETRY)
                        .heightIn(min = 48.dp),
                ) {
                    Text(text = stringResource(id = R.string.project_detail_error_retry))
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .testTag(ProjectDetailTestTags.ERROR_DISMISS)
                        .heightIn(min = 48.dp),
                ) {
                    Text(text = stringResource(id = R.string.project_detail_error_dismiss))
                }
            }
        }
    }
}

/** Card exibido quando a lista de tarefas está vazia. */
@Composable
internal fun EmptyTasksCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.project_detail_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.project_detail_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Resolve a mensagem vinda do [ProjectDetailViewModel] para a versão
 * localizada em `strings.xml` quando ela casar com uma chave conhecida;
 * caso contrário devolve a própria mensagem (mesma estratégia de
 * `resolveAuthMessage` em `:feature:auth`, marco E4.6).
 */
@Composable
internal fun resolveProjectDetailMessage(message: String): String {
    val limit = MAX_ACTIVE_TASKS_PER_PROJECT
    return when {
        message == ProjectDetailViewModel.ERROR_EMPTY_TITLE ->
            stringResource(id = R.string.project_detail_error_empty_title)
        message == ProjectDetailViewModel.ERROR_INVALID_TITLE ->
            stringResource(id = R.string.project_detail_error_invalid_title)
        message == ProjectDetailViewModel.ERROR_INVALID_TRANSITION ->
            stringResource(id = R.string.project_detail_error_invalid_transition)
        message == ProjectDetailViewModel.ERROR_PRIORITY_LOCKED ||
            ProjectDetailViewModel.isPriorityLockedMessage(message) ->
            stringResource(id = R.string.project_detail_error_priority_locked_on_done)
        // RN01: mensagem cru do domínio inclui o id do projeto; casamos
        // por prefixo/sufixo e usamos o limite global no recurso localizado.
        ProjectDetailViewModel.isTaskLimitMessage(message) ->
            stringResource(id = R.string.project_detail_error_task_limit, limit)
        else -> message
    }
}
