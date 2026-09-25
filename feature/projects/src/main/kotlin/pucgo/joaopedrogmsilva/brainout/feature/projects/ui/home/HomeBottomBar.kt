// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Bottom bar da Home com 4 destinos (E2.6/E2.7). Todas as posições
// são renderizadas como itens de `NavigationBar`; só "Projetos" é
// aba interna — as demais disparam navegação externa via callback
// no orchestrator. O ícone de cada aba vem de `HomeTab.icon()`.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
internal fun HomeBottomBar(
    currentTab: HomeTab,
    onSelectTab: (HomeTab) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
    ) {
        HomeTab.values().forEach { tab ->
            val isSelected = tab == currentTab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon(),
                        contentDescription = null,
                    )
                },
                label = {
                    Text(
                        text = stringResource(id = tab.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                alwaysShowLabel = true,
            )
        }
    }
}

internal fun HomeTab.icon(): ImageVector = when (this) {
    HomeTab.Projects -> Icons.Outlined.Folder
    HomeTab.Tasks -> Icons.AutoMirrored.Outlined.Assignment
    HomeTab.Dashboard -> Icons.Outlined.BarChart
    HomeTab.Settings -> Icons.Outlined.Settings
}
