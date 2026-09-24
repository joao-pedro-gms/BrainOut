// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Diálogo de confirmação para excluir o projeto corrente (E2.2).

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

@Composable
internal fun DeleteProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.project_detail_delete_confirm_title)) },
        text = {
            Text(text = stringResource(id = R.string.project_detail_delete_confirm_body))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(id = R.string.project_detail_delete_project),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.common_close))
            }
        },
    )
}
