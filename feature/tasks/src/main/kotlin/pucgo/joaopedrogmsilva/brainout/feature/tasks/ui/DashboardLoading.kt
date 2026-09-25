// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Estado de loading do Dashboard — extraído de DashboardScreen para
// reduzir o orquestrador. Mesmo padrão usado em Tasks/Home.

package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.feature.tasks.R

@Composable
internal fun DashboardLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .testTag(DashboardTestTags.LOADING),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.height(48.dp))
            Text(
                text = stringResource(id = R.string.dashboard_loading_aria_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
