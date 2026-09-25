// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// FAB estendido da Home gated pelo papel (E1.7): Owner cria projeto,
// Member dispara diálogo explicativo. `UpgradeDialog` explica a
// limitação ao Member — resolve as strings do papel via recursos.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

@Composable
internal fun HomeFloatingActionButton(
    isOwner: Boolean,
    onCreateProjectClicked: () -> Unit,
    onMemberFabClicked: () -> Unit,
) {
    ExtendedFloatingActionButton(
        onClick = {
            if (isOwner) {
                onCreateProjectClicked()
            } else {
                onMemberFabClicked()
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(id = R.string.home_fab_create),
            )
        },
        text = {
            Text(
                text = if (isOwner) {
                    stringResource(id = R.string.home_fab_create)
                } else {
                    stringResource(id = R.string.home_fab_disabled_owner)
                },
                style = MaterialTheme.typography.labelLarge,
            )
        },
        containerColor = if (isOwner) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (isOwner) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.testTag(HomeTestTags.FAB),
    )
}

@Composable
internal fun UpgradeDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(HomeTestTags.UPGRADE_DIALOG_ACK),
            ) {
                Text(text = stringResource(id = R.string.home_role_member_dialog_ack))
            }
        },
        title = {
            Text(text = stringResource(id = R.string.home_role_member_dialog_title))
        },
        text = {
            Text(text = stringResource(id = R.string.home_role_member_dialog_body))
        },
        modifier = Modifier.testTag(HomeTestTags.UPGRADE_DIALOG),
    )
}
