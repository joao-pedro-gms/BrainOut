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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeTopBar(user: HomeUser) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
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
                Column {
                    Text(
                        text = stringResource(id = R.string.home_topbar_greeting, user.displayName),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    val badgeLabel = when (user.role) {
                        HomeUserRole.Owner -> stringResource(id = R.string.home_role_badge_owner)
                        HomeUserRole.Member -> stringResource(id = R.string.home_role_badge_member)
                    }
                    val roleContainer: Color = when (user.role) {
                        HomeUserRole.Owner -> MaterialTheme.colorScheme.primary
                        HomeUserRole.Member -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val roleLabel: Color = when (user.role) {
                        HomeUserRole.Owner -> MaterialTheme.colorScheme.onPrimary
                        HomeUserRole.Member -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    AssistChip(
                        onClick = { /* badge é apenas decorativo */ },
                        label = {
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.testTag(HomeTestTags.ROLE_BADGE),
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = roleContainer,
                            labelColor = roleLabel,
                        ),
                        // E4.4: garante área de toque mínima de 48dp
                        // (WCAG 2.5.5 Target Size). M3 AssistChip é ~32dp
                        // de altura por padrão.
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}
