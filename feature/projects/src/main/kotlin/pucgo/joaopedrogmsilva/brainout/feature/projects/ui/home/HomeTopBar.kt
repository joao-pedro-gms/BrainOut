// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// TopAppBar da Home com monograma do usuário e badge de papel
// colorido por role (E1.7): Owner usa container primário, Member
// usa surfaceVariant. O badge é decorativo (AssistChip sem ação).

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoInfoChip
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

/**
 * TopAppBar da Home com monograma do usuário e badge de papel
 * colorido por role (E1.7): Owner usa container primário, Member
 * usa surfaceVariant. O badge é decorativo (AssistChip sem ação).
 *
 * [user] nulo significa "sem sessão ainda" (carregando ou sem login):
 * a barra mostra a saudação genérica, sem avatar e sem badge de
 * papel — inventar uma identidade seria mostrar ao usuário o nome de
 * outra pessoa (DEF-72).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeTopBar(user: HomeUser?) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (user != null) {
                    Box(
                        modifier =
                            Modifier
                                .size(40.dp * LocalDensity.current.fontScale.coerceAtLeast(1f))
                                .clearAndSetSemantics {}
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = user.initials,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Column {
                    Text(
                        text =
                            user?.let { stringResource(id = R.string.home_topbar_greeting, it.displayName) }
                                ?: stringResource(id = R.string.home_topbar_greeting_generic),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (user != null) {
                        val badgeLabel =
                            when (user.role) {
                                HomeUserRole.Owner -> stringResource(id = R.string.home_role_badge_owner)
                                HomeUserRole.Member -> stringResource(id = R.string.home_role_badge_member)
                            }
                        val roleContainer: Color =
                            when (user.role) {
                                HomeUserRole.Owner -> MaterialTheme.colorScheme.primary
                                HomeUserRole.Member -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        val roleLabel: Color =
                            when (user.role) {
                                HomeUserRole.Owner -> MaterialTheme.colorScheme.onPrimary
                                HomeUserRole.Member -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        // DEF-22 — era `AssistChip(onClick = { })`: o badge
                        // de papel é informativo, então o TalkBack o
                        // anunciava como botão e oferecia um toque que não
                        // fazia nada. `NeoInfoChip` não tem `onClick`.
                        NeoInfoChip(
                            label = badgeLabel,
                            containerColor = roleContainer,
                            labelColor = roleLabel,
                            modifier = Modifier.testTag(HomeTestTags.ROLE_BADGE),
                        )
                    }
                }
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
    )
}
