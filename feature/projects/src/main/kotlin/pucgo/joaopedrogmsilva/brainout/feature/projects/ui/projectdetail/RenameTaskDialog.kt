// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Diálogo de renomear tarefa (DEF-22).
//
// `ProjectDetailViewModel.renameTask` e `Task.rename` já existiam e
// validavam o título, mas nada na UI os chamava: o parâmetro chegava
// em `ProjectDetailBody` marcado `@Suppress("unused")` e morria ali.
// Este diálogo é a metade que faltava.
//
// Espelha [NewTaskDialog] de propósito: mesma estrutura de `AlertDialog`
// com um `OutlinedTextField` único, para que o padrão de título/salvar/
// cancelar seja o mesmo nos dois diálogos do detalhe.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

/**
 * @param initialTitle título atual, usado como valor inicial do campo.
 * @param onConfirm recebe o título digitado; a validação e a
 *  persistência ficam no domínio ([Task.rename]) e no ViewModel.
 */
@Composable
internal fun RenameTaskDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (newTitle: String) -> Unit,
) {
    var title by rememberSaveable(initialTitle) { mutableStateOf(initialTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = R.string.project_detail_task_rename_dialog_title))
        },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(text = stringResource(id = R.string.project_detail_new_task_title_label)) },
                singleLine = true,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(ProjectDetailTestTags.RENAME_TASK_TITLE_FIELD),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title) },
                modifier = Modifier.testTag(ProjectDetailTestTags.RENAME_TASK_SAVE),
            ) {
                Text(text = stringResource(id = R.string.project_detail_new_task_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.common_close))
            }
        },
        modifier = Modifier.testTag(ProjectDetailTestTags.RENAME_TASK_DIALOG),
    )
}
