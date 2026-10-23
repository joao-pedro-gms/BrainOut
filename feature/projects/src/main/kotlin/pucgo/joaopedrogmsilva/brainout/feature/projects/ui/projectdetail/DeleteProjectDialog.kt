// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Diálogo de confirmação para excluir o projeto corrente (E2.2).

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import pucgo.joaopedrogmsilva.brainout.feature.projects.R
import pucgo.joaopedrogmsilva.brainout.feature.projects.ui.ProjectDialog

@Composable
internal fun DeleteProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ProjectDialog(
        titleRes = R.string.project_detail_delete_confirm_title,
        onDismiss = onDismiss,
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
    )
}
