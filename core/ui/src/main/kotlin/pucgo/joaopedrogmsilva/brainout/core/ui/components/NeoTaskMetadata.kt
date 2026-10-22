// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.ui.R
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoMotion
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSizes
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.neoConfirmedColor

private val priorityIcons =
    listOf(
        R.drawable.neo_ic_priority_low,
        R.drawable.neo_ic_priority_medium,
        R.drawable.neo_ic_priority_high,
        R.drawable.neo_ic_priority_urgent,
        R.drawable.neo_ic_priority_critical,
    )
private val priorityLabels =
    listOf(
        R.string.neo_priority_low,
        R.string.neo_priority_medium,
        R.string.neo_priority_high,
        R.string.neo_priority_urgent,
        R.string.neo_priority_critical,
    )

/** Indicador informativo, sem clique e sem dependência de cor para distinguir níveis. */
@Composable
fun NeoPriorityIndicator(
    priorityCode: Int,
    modifier: Modifier = Modifier,
) {
    require(priorityCode in priorityIcons.indices)
    val description = stringResource(R.string.neo_priority_description, stringResource(priorityLabels[priorityCode]))
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        color =
            when (priorityCode) {
                4 -> colors.errorContainer
                3 -> colors.secondaryContainer
                2 -> colors.tertiaryContainer
                else -> colors.surfaceVariant
            },
        contentColor =
            when (priorityCode) {
                4 -> colors.onErrorContainer
                3 -> colors.onSecondaryContainer
                2 -> colors.onTertiaryContainer
                else -> colors.onSurfaceVariant
            },
        border = BorderStroke(1.dp, colors.outline),
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Icon(
            painterResource(priorityIcons[priorityCode]),
            contentDescription = null,
            modifier = Modifier.padding(4.dp).size(NeoSizes.iconMeta),
        )
    }
}

/** A cor de confirmação acompanha apenas o estado observado, sem mudança otimista. */
@Composable
fun NeoStatusChip(
    label: String,
    completed: Boolean,
    inProgress: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val container =
        when {
            completed -> colors.primaryContainer
            inProgress -> colors.tertiaryContainer
            else -> colors.surfaceVariant
        }
    val foreground =
        when {
            completed -> colors.onPrimaryContainer
            inProgress -> colors.onTertiaryContainer
            else -> colors.onSurfaceVariant
        }
    NeoInfoChip(
        label,
        modifier,
        containerColor = neoConfirmedColor(container),
        labelColor = neoConfirmedColor(foreground),
    )
}

/**
 * Rótulo localizado do status de uma tarefa.
 *
 * O mapeamento status → texto mora aqui, e não em cada feature: antes,
 * `:feature:projects` e `:feature:tasks` carregavam a mesma tabela e as
 * mesmas strings (`task_status_*`) em cópia.
 */
@Composable
fun NeoTaskStatusLabel(status: TaskStatus): String = stringResource(statusLabelRes(status))

/** Chip de status de uma tarefa, com o rótulo já localizado. */
@Composable
fun NeoTaskStatusChip(
    status: TaskStatus,
    modifier: Modifier = Modifier,
) {
    NeoStatusChip(
        label = NeoTaskStatusLabel(status),
        completed = status == TaskStatus.DONE,
        inProgress = status == TaskStatus.DOING,
        modifier = modifier,
    )
}

/** Rótulo localizado da prioridade de uma tarefa. */
@Composable
fun NeoTaskPriorityLabel(priority: TaskPriority): String = stringResource(priorityLabelRes(priority))

/** Chip de prioridade de uma tarefa. */
@Composable
fun NeoTaskPriorityChip(
    priority: TaskPriority,
    modifier: Modifier = Modifier,
) {
    NeoPriorityIndicator(priority.priorityCode, modifier)
}

private fun statusLabelRes(status: TaskStatus): Int =
    when (status) {
        TaskStatus.TODO -> R.string.neo_task_status_todo
        TaskStatus.DOING -> R.string.neo_task_status_doing
        TaskStatus.DONE -> R.string.neo_task_status_done
    }

private fun priorityLabelRes(priority: TaskPriority): Int =
    when (priority) {
        TaskPriority.LOW -> R.string.neo_priority_low
        TaskPriority.MEDIUM -> R.string.neo_priority_medium
        TaskPriority.HIGH -> R.string.neo_priority_high
        TaskPriority.URGENT -> R.string.neo_priority_urgent
        TaskPriority.CRITICAL -> R.string.neo_priority_critical
    }

@Composable
fun NeoPriorityLegend(modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val expandedLabel = stringResource(if (expanded) R.string.neo_expanded else R.string.neo_collapsed)
    Column(modifier) {
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.semantics { stateDescription = expandedLabel },
        ) {
            Text(stringResource(R.string.neo_priority_legend))
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(NeoMotion.selection)),
            exit = shrinkVertically(tween(NeoMotion.selection)),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                priorityIcons.indices.forEach { code ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NeoPriorityIndicator(code)
                        Text(stringResource(priorityLabels[code]), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun NeoTaskMetadataPreview() {
    BrainOutTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeoPriorityIndicator(4)
            NeoStatusChip("Concluída", completed = true, inProgress = false)
        }
    }
}
