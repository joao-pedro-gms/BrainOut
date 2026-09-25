// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Banner de erro do Dashboard (E2.8) — extraído para reduzir o
// orquestrador. Estrutura visual igual ao banner de Tasks/Home.

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.feature.tasks.R

@Composable
internal fun DashboardErrorBanner(
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .testTag(DashboardTestTags.ERROR_BANNER),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.dashboard_error_load_failed),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .testTag(DashboardTestTags.ERROR_RETRY)
                        .heightIn(min = 48.dp),
                ) {
                    Text(text = stringResource(id = R.string.dashboard_error_retry))
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .testTag(DashboardTestTags.ERROR_DISMISS)
                        .heightIn(min = 48.dp),
                ) {
                    Text(text = stringResource(id = R.string.dashboard_error_dismiss))
                }
            }
        }
    }
}
