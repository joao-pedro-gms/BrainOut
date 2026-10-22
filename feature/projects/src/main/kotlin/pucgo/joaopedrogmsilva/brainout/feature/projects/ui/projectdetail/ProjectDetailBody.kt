// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Coluna do corpo da ProjectDetail (E2.2/E2.8): cabeçalho (id,
// descrição), divisória, título da seção de tarefas e
// a área da lista com as branches loading/empty/error/list (LazyColumn).
// Rotas os callbacks do ViewModel recebidos do orquestrador.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoPriorityLegend
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.LocalNeoWindowInfo
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoMotion
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.neoContentWidth
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

@Composable
internal fun ProjectDetailBody(
    contentPadding: PaddingValues,
    state: ProjectDetailState,
    actions: ProjectDetailActions,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .neoContentWidth()
                .padding(horizontal = LocalNeoWindowInfo.current.margin, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 88.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                Text(
                    text = stringResource(id = R.string.project_detail_tasks_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                NeoPriorityLegend()
            }
        }
        item {
            val errorMessage = state.errorMessage
            if (errorMessage is ProjectDetailError.LoadFailed) {
                ProjectDetailErrorBanner(
                    error = errorMessage,
                    onRetry = actions.onRetry,
                    onDismiss = actions.onDismissError,
                )
            } else if (state.tasks.isEmpty() && state.isLoading) {
                ProjectDetailLoadingState()
            } else if (state.tasks.isEmpty()) {
                EmptyTasksCard()
            }
        }
        items(items = state.tasks, key = { it.id }) { task ->
            androidx.compose.foundation.layout.Box(
                Modifier.animateItem(
                    fadeInSpec = tween(NeoMotion.selection),
                    fadeOutSpec = tween(NeoMotion.selection),
                    placementSpec = tween(NeoMotion.selection),
                ),
            ) {
                TaskRow(task = task, actions = actions)
            }
        }
    }
}
