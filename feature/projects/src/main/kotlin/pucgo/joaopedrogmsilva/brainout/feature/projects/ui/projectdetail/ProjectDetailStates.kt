// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Estados de feedback da ProjectDetail (E2.8): loader centralizado,
// banner de erro com retry/dispensar, card de "sem tarefas" e
// resolução da chave canônica do ViewModel para string localizada.
// Espelha `HomeStates` em :feature:projects/ui/home.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoErrorBanner
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoBorders
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

/**
 * Loader centralizado da ProjectDetail (E2.8). Mantém paridade visual
 * com `HomeLoadingState` em :feature:projects/ui/home.
 */
@Composable
internal fun ProjectDetailLoadingState() {
    Column(
        modifier =
            Modifier
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
 *
 * DEF-26: o erro chega **tipado** ([ProjectDetailError]) e o texto sai
 * de [resolveProjectDetailMessage] — a falha de escrita (título vazio,
 * RN02) não é mais escondida atrás de "falha ao carregar".
 */
@Composable
internal fun ProjectDetailErrorBanner(
    error: ProjectDetailError,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    NeoErrorBanner(
        message = resolveProjectDetailMessage(error),
        onRetry = onRetry,
        onDismiss = onDismiss,
    )
}

/** Card exibido quando a lista de tarefas está vazia. */
@Composable
internal fun EmptyTasksCard() {
    // DEF-22 — ver [HomeEmptyState]: `surfaceVariant.copy(alpha = 0.4f)`
    // dava 1.06:1 / 1.11:1. A região ganha uma borda `outline` real
    // (16.10:1 / 15.95:1) em vez de continuar quase invisível.
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(NeoBorders.default, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier =
                Modifier
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
 * Resolve o erro do [ProjectDetailViewModel] para o texto localizado.
 *
 * O ViewModel entrega o **tipo** do erro ([ProjectDetailError]); aqui a
 * escolha do texto em pt/en. Antes o tipo era deduzido comparando a
 * mensagem em português — inclusive extraindo o limite de tarefas por
 * prefixo/sufixo —, o que quebrava se o texto mudasse.
 */
@Composable
internal fun resolveProjectDetailMessage(error: ProjectDetailError): String =
    when (error) {
        ProjectDetailError.LoadFailed ->
            stringResource(id = R.string.project_detail_error_load_failed)
        ProjectDetailError.EmptyTitle ->
            stringResource(id = R.string.project_detail_error_empty_title)
        ProjectDetailError.InvalidTitle ->
            stringResource(id = R.string.project_detail_error_invalid_title)
        ProjectDetailError.InvalidTransition ->
            stringResource(id = R.string.project_detail_error_invalid_transition)
        ProjectDetailError.PriorityLocked ->
            stringResource(id = R.string.project_detail_error_priority_locked_on_done)
        ProjectDetailError.ProjectNotFound ->
            stringResource(id = R.string.project_detail_error_project_not_found)
        is ProjectDetailError.TaskLimitReached ->
            stringResource(id = R.string.project_detail_error_task_limit, error.limit)
        ProjectDetailError.ActionFailed ->
            stringResource(id = R.string.project_detail_error_action_failed)
    }
