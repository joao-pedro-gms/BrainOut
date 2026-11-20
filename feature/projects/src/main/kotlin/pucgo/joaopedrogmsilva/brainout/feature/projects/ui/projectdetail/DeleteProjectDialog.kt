// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Diálogo de confirmação para excluir o projeto corrente (E2.2).

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSizes
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
            TextButton(
                onClick = onConfirm,
                // E4.4: 48dp mínimo para área de toque (WCAG 2.5.5) —
                // o `TextButton` padrão do M3 marca 40dp.
                modifier = Modifier.heightIn(min = NeoSizes.touchMin),
            ) {
                Text(
                    text = stringResource(id = R.string.project_detail_delete_project),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun DeleteProjectDialogPreview() {
    BrainOutTheme {
        androidx.compose.material3.Surface {
            DeleteProjectDialog(
                onDismiss = {},
                onConfirm = {},
            )
        }
    }
}
