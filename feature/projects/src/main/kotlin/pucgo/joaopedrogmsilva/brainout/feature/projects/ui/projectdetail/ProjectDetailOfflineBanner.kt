// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Wrapper ProjectDetail do banner offline compartilhado (E3.4). Aplica
// a testTag do detalhe e delega para `OfflineBanner` em `ui/common`.
// Espelha `HomeOfflineBanner` em :feature:projects/ui/home — ambos
// consomem o mesmo composable compartilhado.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import pucgo.joaopedrogmsilva.brainout.feature.projects.ui.common.OfflineBanner

@Composable
internal fun ProjectDetailOfflineBanner(
    syncState: ProjectDetailSyncState,
    modifier: Modifier = Modifier,
) {
    OfflineBanner(
        showBanner = syncState.showOfflineBanner,
        pendingOps = syncState.pendingOps,
        modifier = modifier.testTag(ProjectDetailTestTags.OFFLINE_BANNER),
    )
}
