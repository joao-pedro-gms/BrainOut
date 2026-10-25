// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Cartão de contagem de projetos por estado (ativos/concluídos) do
// Dashboard — extraído do orquestrador para reduzir DashboardScreen.kt.

package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoCard
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSpacing
import pucgo.joaopedrogmsilva.brainout.feature.tasks.R

@Composable
internal fun ProjectStateCard(
    activeProjects: Int,
    completedProjects: Int,
) {
    NeoCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(DashboardTestTags.PROJECT_STATE_CARD),
    ) {
        Text(
            text = stringResource(id = R.string.dashboard_projects_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NeoSpacing.md),
        ) {
            MetricCard(
                value = activeProjects.toString(),
                caption = stringResource(id = R.string.dashboard_projects_active),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                testTag = DashboardTestTags.PROJECT_ACTIVE_COUNT,
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                value = completedProjects.toString(),
                caption = stringResource(id = R.string.dashboard_projects_completed),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                testTag = DashboardTestTags.PROJECT_COMPLETED_COUNT,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
