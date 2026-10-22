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
 * A mensagem chega como a chave canônica do [DashboardViewModel]; a
 * tradução para o texto localizado acontece em
 * [resolveDashboardErrorMessage] — o mesmo caminho de Home, Detalhe de
 * Projeto e Tarefas.
 */
@Composable
internal fun DashboardErrorBanner(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    NeoErrorBanner(
        message = resolveDashboardErrorMessage(message),
        onRetry = onRetry,
        onDismiss = onDismiss,
    )
}

/**
 * Resolve a chave canônica de erro do [DashboardViewModel] para a string
 * localizada. Mesma estratégia de `resolveHomeErrorMessage` em Home.
 */
@Composable
internal fun resolveDashboardErrorMessage(message: String): String =
    if (DashboardViewModel.isLoadErrorMessage(message)) {
        stringResource(id = R.string.dashboard_error_load_failed)
    } else {
        message
    }
