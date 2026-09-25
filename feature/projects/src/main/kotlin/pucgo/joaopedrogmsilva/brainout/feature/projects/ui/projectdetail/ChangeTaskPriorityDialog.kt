// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Diálogo de alteração de prioridade (RN02 — E2.4). Só abre para
// tarefas ativas (TODO/DOING); a UI não oferece a ação para tarefas
// concluídas — mas o domínio também bloquearia o update por defesa em
// profundidade. Mostra a prioridade atual pré-selecionada como radio do
// `FilterChip` (visual com check); `onConfirm(newPriority)` dispara em
// "Salvar".

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ChangeTaskPriorityDialog(
    currentPriority: TaskPriority,
    onDismiss: () -> Unit,
    onConfirm: (TaskPriority) -> Unit,
) {
    var selected by rememberSaveable(currentPriority) { mutableStateOf(currentPriority) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = R.string.project_detail_task_priority_dialog_title))
        },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TaskPriority.entries.forEach { option ->
                    FilterChip(
                        selected = option == selected,
                        onClick = { selected = option },
                        label = {
                            Text(text = stringResource(id = option.labelRes()))
                        },
                        modifier = Modifier
                            .testTag(
                                ProjectDetailTestTags.TASK_PRIORITY_CHIP_OPTION_PREFIX +
                                    option.priorityCode.toString(),
                            )
                            // E4.4: 48dp mínimo WCAG 2.5.5.
                            .heightIn(min = 48.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selected) },
            ) {
                Text(text = stringResource(id = R.string.project_detail_new_task_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.common_close))
            }
        },
        modifier = Modifier.testTag(ProjectDetailTestTags.TASK_PRIORITY_DIALOG),
    )
}
