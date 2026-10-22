// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Banner de erro do Dashboard (E2.8) — extraído para reduzir o
// orquestrador. Estrutura visual igual ao banner de Tasks/Home: ambos
// delegam ao componente compartilhado `NeoErrorBanner` de `:core:ui`.

package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoErrorBanner
import pucgo.joaopedrogmsilva.brainout.feature.tasks.R

/**
 * Banner de erro do painel.
 *
 * O erro chega **tipado** ([TasksErrorType]); a tradução para o texto
 * localizado acontece em [resolveDashboardErrorMessage] — o mesmo
 * caminho de Home, Detalhe de Projeto e Tarefas.
 */
@Composable
internal fun DashboardErrorBanner(
    error: TasksErrorType,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    NeoErrorBanner(
        message = resolveDashboardErrorMessage(error),
        onRetry = onRetry,
        onDismiss = onDismiss,
    )
}

/**
 * Resolve o erro do [DashboardViewModel] para o texto localizado.
 *
 * O ViewModel entrega o **tipo** do erro ([TasksErrorType]); o texto é
 * de cada tela — o do Painel é `dashboard_error_load_failed`.
 */
@Composable
internal fun resolveDashboardErrorMessage(error: TasksErrorType): String =
    when (error) {
        TasksErrorType.LoadFailed ->
            stringResource(id = R.string.dashboard_error_load_failed)
    }
