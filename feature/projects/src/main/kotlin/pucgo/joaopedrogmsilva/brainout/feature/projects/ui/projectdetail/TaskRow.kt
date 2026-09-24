// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Item de tarefa do detalhe do projeto (E2.4/RN02): superfície com
// título, chips de status/prioridade e menu de contexto (mover status,
// alterar prioridade, excluir). Helpers de rótulo/transição vivem aqui
// por serem correlatos ao item.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

@Composable
internal fun TaskRow(
    task: Task,
    onChangeStatus: (taskId: String, target: TaskStatus) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onChangePriority: (Task, TaskPriority) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TaskRowHeader(
                title = task.title,
                onMenuClicked = { menuExpanded = true },
            )
            TaskRowChips(status = task.status, priority = task.priority)
        }

        TaskRowMenu(
            expanded = menuExpanded,
            onDismiss = { menuExpanded = false },
            task = task,
            onChangeStatus = onChangeStatus,
            onDeleteTask = onDeleteTask,
            onChangePriority = onChangePriority,
        )
    }
}

@Composable
private fun TaskRowHeader(
    title: String,
    onMenuClicked: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onMenuClicked) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                // E4.4: descreve a ação do botão para TalkBack; o rótulo
                // textual do menu é anunciado quando o dropdown abre,
                // mas o botão em si precisa de descrição própria.
                contentDescription = stringResource(id = R.string.project_detail_task_menu_more),
            )
        }
    }
}

@Composable
private fun TaskRowChips(
    status: TaskStatus,
    priority: TaskPriority,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusChip(status = status)
        PriorityChip(priority = priority)
    }
}

@Composable
private fun TaskRowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    task: Task,
    onChangeStatus: (taskId: String, target: TaskStatus) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onChangePriority: (Task, TaskPriority) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(ProjectDetailTestTags.TASK_ITEM_MENU),
    ) {
        // Opções de mudança de status — só as transições válidas
        // pela matriz do domínio aparecem para evitar cliques
        // que resultariam em InvalidStateTransitionException.
        task.status.allowedTransitions().forEach { (target, labelRes) ->
            DropdownMenuItem(
                text = { Text(text = stringResource(id = labelRes)) },
                onClick = {
                    onDismiss()
                    onChangeStatus(task.id, target)
                },
                modifier = Modifier.testTag(target.menuTag()),
            )
        }
        HorizontalDivider()
        // RN02 (E2.4) — alterar prioridade:
        // - tarefa ativa (TODO/DOING): item "Alterar prioridade"
        //   ativo que abre o diálogo de prioridade.
        // - tarefa concluída (DONE): item desabilitado com
        //   rótulo "(somente leitura)" e não abre diálogo.
        // A defesa em profundidade fica no domínio
        // (`Task.changePriority` lança
        // `TaskPriorityChangeForbiddenException`), mas a UI já não
        // oferece a ação — bom para RN02 também.
        DropdownMenuItem(
            text = {
                val isLocked = task.status == TaskStatus.DONE
                Text(
                    text = stringResource(
                        id = if (isLocked) {
                            R.string.project_detail_task_menu_priority_locked_label
                        } else {
                            R.string.project_detail_task_menu_change_priority
                        },
                    ),
                    color = if (isLocked) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            },
            enabled = task.status != TaskStatus.DONE,
            onClick = {
                onDismiss()
                onChangePriority(task, task.priority)
            },
            modifier = Modifier.testTag(ProjectDetailTestTags.TASK_ITEM_MENU_CHANGE_PRIORITY),
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(id = R.string.project_detail_task_menu_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            },
            onClick = {
                onDismiss()
                onDeleteTask(task)
            },
            modifier = Modifier.testTag(ProjectDetailTestTags.TASK_ITEM_MENU_DELETE),
        )
    }
}

@Composable
private fun StatusChip(status: TaskStatus) {
    val (label, container, content) = when (status) {
        TaskStatus.TODO -> Triple(
            stringResource(id = R.string.task_status_todo),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TaskStatus.DOING -> Triple(
            stringResource(id = R.string.task_status_doing),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        TaskStatus.DONE -> Triple(
            stringResource(id = R.string.task_status_done),
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
        )
    }
    AssistChip(
        onClick = { /* status chip é apenas informativo */ },
        label = { Text(text = label, style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = container,
            labelColor = content,
        ),
        // E4.4: 48dp mínimo (WCAG 2.5.5 Target Size).
        modifier = Modifier.heightIn(min = 48.dp),
    )
}

@Composable
private fun PriorityChip(priority: TaskPriority) {
    AssistChip(
        onClick = { /* priority chip é apenas informativo */ },
        label = {
            Text(text = stringResource(id = priority.labelRes()), style = MaterialTheme.typography.labelSmall)
        },
        // E4.4: 48dp mínimo (WCAG 2.5.5 Target Size).
        modifier = Modifier.heightIn(min = 48.dp),
    )
}

/** Helper para mapear [TaskPriority] ao recurso de string correspondente. */
@Composable
internal fun TaskPriority.labelRes(): Int = when (this) {
    TaskPriority.LOW -> R.string.task_priority_low
    TaskPriority.MEDIUM -> R.string.task_priority_medium
    TaskPriority.HIGH -> R.string.task_priority_high
    TaskPriority.URGENT -> R.string.task_priority_urgent
    TaskPriority.CRITICAL -> R.string.task_priority_critical
}

/**
 * Transições válidas a partir do estado atual, com o rótulo do menu
 * correspondente. Mantém a paridade com [TaskStatus.canTransitionTo]
 * do domínio e evita oferecer opções que resultariam em
 * `InvalidStateTransitionException` ao chamar `changeStatus`.
 */
private fun TaskStatus.allowedTransitions(): List<Pair<TaskStatus, Int>> = when (this) {
    TaskStatus.TODO -> listOf(
        TaskStatus.DOING to R.string.project_detail_task_menu_move_doing,
    )
    TaskStatus.DOING -> listOf(
        TaskStatus.TODO to R.string.project_detail_task_menu_move_todo,
        TaskStatus.DONE to R.string.project_detail_task_menu_move_done,
    )
    TaskStatus.DONE -> listOf(
        TaskStatus.DOING to R.string.project_detail_task_menu_move_doing,
    )
}

/** Resolve o test tag do item de menu correspondente ao status alvo. */
private fun TaskStatus.menuTag(): String = when (this) {
    TaskStatus.TODO -> ProjectDetailTestTags.TASK_ITEM_MENU_MOVE_TODO
    TaskStatus.DOING -> ProjectDetailTestTags.TASK_ITEM_MENU_MOVE_DOING
    TaskStatus.DONE -> ProjectDetailTestTags.TASK_ITEM_MENU_MOVE_DONE
}
