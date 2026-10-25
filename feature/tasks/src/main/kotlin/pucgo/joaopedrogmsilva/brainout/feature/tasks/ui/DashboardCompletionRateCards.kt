// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Cartões de taxa de conclusão (semanal + global) - extraído de DashboardScreen.kt
package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.LocalNeoWindowInfo
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSpacing
import pucgo.joaopedrogmsilva.brainout.feature.tasks.R

@Composable
internal fun CompletionRateCards(
    weeklyPercent: Int,
    overallPercent: Int,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NeoSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(NeoSpacing.sm),
        maxItemsInEachRow =
            if (LocalConfiguration.current.fontScale > 1.3f ||
                LocalNeoWindowInfo.current.forceSingleColumn
            ) {
                1
            } else {
                2
            },
    ) {
        MetricCard(
            value = stringResource(id = R.string.dashboard_rate_percent, weeklyPercent),
            caption = stringResource(id = R.string.dashboard_weekly_rate_title),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            testTag = DashboardTestTags.WEEKLY_RATE_CARD,
            modifier = Modifier.weight(1f),
            minHeight = 96.dp,
            valueStyle = MaterialTheme.typography.headlineSmall,
        )
        MetricCard(
            value = stringResource(id = R.string.dashboard_rate_percent, overallPercent),
            caption = stringResource(id = R.string.dashboard_overall_rate_title),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            testTag = DashboardTestTags.OVERALL_RATE_CARD,
            modifier = Modifier.weight(1f),
            minHeight = 96.dp,
            valueStyle = MaterialTheme.typography.headlineSmall,
        )
    }
}
