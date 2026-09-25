// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Diálogo de criação de tarefa (E2.2/RN01): título, prioridade (menu
// sobreposto ao TextField read-only) e prazo opcional via `DeadlineField`.
// O `evaluateDeadline` vem do `ProjectDetailViewModel` para que a UI
// não precise importar o caso de uso de domínio diretamente.

@file:Suppress("LongMethod") // três campos (título/prioridade/prazo) + diálogo; extrair quebraria a coesão.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.DeadlineInfo
import pucgo.joaopedrogmsilva.brainout.feature.projects.R
import java.time.Instant

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun NewTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, priority: TaskPriority, dueDate: Instant?) -> Unit,
    evaluateDeadline: suspend (Instant?) -> DeadlineInfo,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf(TaskPriority.MEDIUM) }
    var priorityMenuExpanded by remember { mutableStateOf(false) }
    var dueDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    val dueDate: Instant? = @Suppress("NewApi") dueDateMillis?.let(Instant::ofEpochMilli)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = R.string.project_detail_new_task_dialog_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(text = stringResource(id = R.string.project_detail_new_task_title_label)) },
                    placeholder = {
                        Text(text = stringResource(id = R.string.project_detail_new_task_title_placeholder))
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ProjectDetailTestTags.NEW_TASK_TITLE_FIELD),
                )
                Box {
                    OutlinedTextField(
                        value = stringResource(id = priority.labelRes()),
                        onValueChange = { /* read-only */ },
                        readOnly = true,
                        label = { Text(text = stringResource(id = R.string.project_detail_new_task_priority_label)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    // Overlay transparente para abrir o menu ao tocar.
                    Surface(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(4.dp))
                            .testTag("project_detail_new_task_priority_field"),
                        color = Color.Transparent,
                        content = {},
                        onClick = { priorityMenuExpanded = true },
                    )
                    DropdownMenu(
                        expanded = priorityMenuExpanded,
                        onDismissRequest = { priorityMenuExpanded = false },
                    ) {
                        TaskPriority.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(text = stringResource(id = option.labelRes())) },
                                onClick = {
                                    priority = option
                                    priorityMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                DeadlineField(
                    dueDate = dueDate,
                    onDateChange = { dueDateMillis = @Suppress("NewApi") it?.toEpochMilli() },
                    evaluateDeadline = evaluateDeadline,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, priority, dueDate) },
                modifier = Modifier.testTag(ProjectDetailTestTags.NEW_TASK_SAVE),
            ) {
                Text(text = stringResource(id = R.string.project_detail_new_task_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(ProjectDetailTestTags.NEW_TASK_CANCEL),
            ) {
                Text(text = stringResource(id = R.string.project_detail_new_task_cancel))
            }
        },
        modifier = Modifier.testTag(ProjectDetailTestTags.NEW_TASK_DIALOG),
    )
}
