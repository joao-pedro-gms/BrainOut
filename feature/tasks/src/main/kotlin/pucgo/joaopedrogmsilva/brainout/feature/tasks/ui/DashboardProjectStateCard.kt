// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Cartão de contagem de projetos por estado (ativos/concluídos) do
// Dashboard — extraído do orquestrador para reduzir DashboardScreen.kt.

package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.feature.tasks.R

@Composable
internal fun ProjectStateCard(
    activeProjects: Int,
    completedProjects: Int,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DashboardTestTags.PROJECT_STATE_CARD),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
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
                StateCounter(
                    label = stringResource(id = R.string.dashboard_projects_active),
                    count = activeProjects,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(DashboardTestTags.PROJECT_ACTIVE_COUNT),
                )
                StateCounter(
                    label = stringResource(id = R.string.dashboard_projects_completed),
                    count = completedProjects,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(DashboardTestTags.PROJECT_COMPLETED_COUNT),
                )
            }
        }
    }
}

@Composable
private fun StateCounter(
    label: String,
    count: Int,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            // E4.4: área de toque/percepção mínima de 48dp.
            .heightIn(min = 72.dp),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = contentColor,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}
