// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Cartão de contagem de projetos por estado (ativos/concluídos) do
// Dashboard — extraído do orquestrador para reduzir DashboardScreen.kt.

package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.feature.tasks.R

@Composable
internal fun ProjectStateCard(
    activeProjects: Int,
    completedProjects: Int,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(DashboardTestTags.PROJECT_STATE_CARD),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(id = R.string.dashboard_projects_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
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
}
