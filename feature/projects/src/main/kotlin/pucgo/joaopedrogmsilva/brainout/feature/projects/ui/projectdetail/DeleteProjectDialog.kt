// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Diálogo de confirmação para excluir o projeto corrente (E2.2).

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoActionButton
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoActionVariant
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
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
            // Destrutiva: o bloco vermelho é o único botão de cor
            // diferente no diálogo — antes era só a tinta do texto.
            NeoActionButton(
                text = stringResource(id = R.string.project_detail_delete_project),
                onClick = onConfirm,
                variant = NeoActionVariant.Danger,
            )
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
