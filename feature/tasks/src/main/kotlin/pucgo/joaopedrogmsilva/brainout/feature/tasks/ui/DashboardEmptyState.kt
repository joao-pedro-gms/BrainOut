// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Estado vazio do Dashboard — extraído para reduzir o orquestrador.
// Mesmo padrão visual usado em TasksScreen e HomeScreen.

package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoEmptyState
import pucgo.joaopedrogmsilva.brainout.feature.tasks.R

@Composable
internal fun DashboardEmptyState(modifier: Modifier = Modifier) {
    NeoEmptyState(
        title = stringResource(id = R.string.dashboard_empty_title),
        body = stringResource(id = R.string.dashboard_empty_body),
        modifier =
            modifier
                .height(280.dp)
                .testTag(DashboardTestTags.EMPTY),
        // O cartão do Painel centraliza o corpo; as listas mantêm o
        // alinhamento inicial herdado.
        bodyTextAlign = TextAlign.Center,
    )
}
