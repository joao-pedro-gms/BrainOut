// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Wrapper Home do banner offline compartilhado (E3.4). Aplica a
// testTag da Home (`home_offline_banner`) e delega a renderização
// para `OfflineBanner` em `ui/common`. Espelha `ProjectDetailOfflineBanner`
// em :feature:projects/ui/projectdetail — ambos consomem o mesmo
// composable compartilhado.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import pucgo.joaopedrogmsilva.brainout.feature.projects.ui.common.OfflineBanner

@Composable
internal fun HomeOfflineBanner(
    syncState: HomeSyncState,
    modifier: Modifier = Modifier,
) {
    OfflineBanner(
        showBanner = syncState.showOfflineBanner,
        pendingOps = syncState.pendingOps,
        modifier = modifier.testTag(HomeTestTags.OFFLINE_BANNER),
    )
}
