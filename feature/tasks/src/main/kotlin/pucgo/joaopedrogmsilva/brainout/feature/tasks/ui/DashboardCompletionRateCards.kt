// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Cartões de taxa de conclusão (semanal + global) - extraído de DashboardScreen.kt
package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
internal fun CompletionRateCards(
    weeklyPercent: Int,
    overallPercent: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RateCard(
            title = stringResource(id = R.string.dashboard_weekly_rate_title),
            percent = weeklyPercent,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            testTag = DashboardTestTags.WEEKLY_RATE_CARD,
            modifier = Modifier.weight(1f),
        )
        RateCard(
            title = stringResource(id = R.string.dashboard_overall_rate_title),
            percent = overallPercent,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            testTag = DashboardTestTags.OVERALL_RATE_CARD,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RateCard(
    title: String,
    percent: Int,
    containerColor: Color,
    contentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .testTag(testTag)
            .heightIn(min = 96.dp),
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
                text = stringResource(id = R.string.dashboard_rate_percent, percent),
                style = MaterialTheme.typography.headlineSmall,
                color = contentColor,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}
