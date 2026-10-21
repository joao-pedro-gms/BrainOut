// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.ui.R
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.LocalNeoWindowInfo
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoLayout
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoMotion

/** Apresentação de uma aba. Rotas e estado de navegação pertencem ao app. */
data class NeoNavigationItem(
    val id: String,
    val label: String,
    @DrawableRes val icon: Int,
)

@Composable
fun NeoNavigationBar(
    items: List<NeoNavigationItem>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier.testTag("main_navigation")) {
        val columns =
            if (LocalDensity.current.fontScale > LARGE_FONT_SCALE &&
                LocalNeoWindowInfo.current.widthDp < NeoLayout.mediumMinWidth
            ) {
                2
            } else {
                items.size.coerceAtLeast(1)
            }
        Column(Modifier.fillMaxWidth()) {
            items.chunked(columns).forEach { rowItems ->
                Row(Modifier.fillMaxWidth()) {
                    rowItems.forEach { item ->
                        val selected = item.id == selectedId
                        val scale by animateFloatAsState(
                            targetValue = if (selected) SELECTED_SCALE else 1f,
                            animationSpec = tween(NeoMotion.selection),
                            label = "navigationSelection",
                        )
                        NavigationBarItem(
                            selected = selected,
                            colors =
                                NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                ),
                            onClick = { if (!selected) onSelect(item.id) },
                            icon = {
                                Icon(
                                    painter = painterResource(item.icon),
                                    contentDescription = null,
                                    modifier =
                                        Modifier.size(24.dp).graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        },
                                )
                            },
                            label = { Text(item.label) },
                            modifier = Modifier.testTag("main_tab_${item.id}"),
                        )
                    }
                }
            }
        }
    }
}

private const val LARGE_FONT_SCALE = 1.3f

private const val SELECTED_SCALE = 1.12f

@Preview
@Composable
private fun NeoNavigationBarPreview() {
    BrainOutTheme {
        NeoNavigationBar(listOf(NeoNavigationItem("projects", "Projetos", R.drawable.neo_ic_folder)), "projects", {})
    }
}
