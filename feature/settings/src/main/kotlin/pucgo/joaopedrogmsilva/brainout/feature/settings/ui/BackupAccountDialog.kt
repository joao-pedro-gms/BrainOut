// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupAccountOption
import pucgo.joaopedrogmsilva.brainout.feature.settings.R

/** A escolha é a confirmação final da substituição dos dados da conta conectada. */
@Composable
internal fun BackupAccountDialog(
    accounts: List<BackupAccountOption>,
    onSelect: (String) -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.settings_backup_select_account)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.settings_backup_select_account_help))
                accounts.forEach { account ->
                    TextButton(onClick = { onSelect(account.id) }) { Text(account.label) }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onCancel,
            ) { Text(stringResource(R.string.settings_backup_cta_cancel)) }
        },
    )
}
