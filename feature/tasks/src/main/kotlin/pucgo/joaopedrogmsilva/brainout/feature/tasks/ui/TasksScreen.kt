// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Tela "Tarefas" (TasksScreen) — aba exibida quando o usuário seleciona
// o item "Tarefas" na bottom bar da HomeScreen. Lista todas as tarefas
// do owner ativo, agrupadas por projeto e com chips de status/prioridade.
//
// Marco E2.6 do ROADMAP.md.

package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.feature.tasks.R

/**
 * Tela "Tarefas" do BrainOut — listagem global das tarefas do owner
 * ativo, com join do nome do projeto para exibição inline.
 *
 * Renderização:
 * - `isLoading = true` → [CircularProgressIndicator] centralizado.
 * - lista vazia → cartão com mensagem explicando o que fazer.
 * - lista populada → [LazyColumn] de cards, cada card exibe título,
 *   projeto, prioridade e status via chips.
 *
 * @param viewModel injetado pelo Hilt; pode ser substituído por um
 *  fake nos `@Preview`/testes Compose.
 */
@Composable
fun TasksScreen(
    modifier: Modifier = Modifier,
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TasksContent(
        state = state,
        modifier = modifier,
    )
}

/** Identificadores para testes de UI Compose. */
object TasksTestTags {
    const val LOADING: String = "tasks_loading"
    const val EMPTY: String = "tasks_empty"
    const val LIST: String = "tasks_list"
}

@Composable
private fun TasksContent(
    state: TasksUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(id = R.string.tasks_screen_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        when {
            state.isLoading -> TasksLoading()
            state.rows.isEmpty() -> TasksEmptyState(modifier = Modifier.fillMaxWidth())
            else -> TasksList(
                rows = state.rows,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TasksLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun TasksEmptyState(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .height(280.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.tasks_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.tasks_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TasksList(
    rows: List<TaskRow>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(items = rows, key = { it.task.id }) { row ->
            TaskCard(row = row)
        }
    }
}

@Composable
private fun TaskCard(row: TaskRow) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = row.task.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = row.projectName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PriorityChip(priority = row.task.priority)
                StatusChip(status = row.task.status)
            }
        }
    }
}

/** Chip de prioridade da tarefa. Cores derivadas do MaterialTheme. */
@Composable
private fun PriorityChip(priority: TaskPriority) {
    val (labelRes, containerColor, contentColor) = when (priority) {
        TaskPriority.LOW -> Triple(
            R.string.task_priority_low,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TaskPriority.MEDIUM -> Triple(
            R.string.task_priority_medium,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TaskPriority.HIGH -> Triple(
            R.string.task_priority_high,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        TaskPriority.URGENT -> Triple(
            R.string.task_priority_urgent,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        TaskPriority.CRITICAL -> Triple(
            R.string.task_priority_critical,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
    }
    AssistChip(
        onClick = { /* chip é decorativo */ },
        label = { Text(text = stringResource(id = labelRes), style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
            labelColor = contentColor,
        ),
        // E4.4: 48dp mínimo (WCAG 2.5.5 Target Size).
        modifier = Modifier.heightIn(min = 48.dp),
    )
}

/** Chip de status da tarefa. Cores derivadas do MaterialTheme. */
@Composable
private fun StatusChip(status: TaskStatus) {
    val (labelRes, containerColor, contentColor) = when (status) {
        TaskStatus.TODO -> Triple(
            R.string.task_status_todo,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TaskStatus.DOING -> Triple(
            R.string.task_status_doing,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        TaskStatus.DONE -> Triple(
            R.string.task_status_done,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
    AssistChip(
        onClick = { /* chip é decorativo */ },
        label = { Text(text = stringResource(id = labelRes), style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
            labelColor = contentColor,
        ),
        // E4.4: 48dp mínimo (WCAG 2.5.5 Target Size).
        modifier = Modifier.heightIn(min = 48.dp),
    )
}

// --- Previews ---------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TasksScreenLoadingPreview() {
    TasksContent(state = TasksUiState(isLoading = true))
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TasksScreenEmptyPreview() {
    TasksContent(state = TasksUiState(rows = emptyList(), isLoading = false))
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TasksScreenPopulatedPreview() {
    val now = java.time.Instant.parse("2026-09-19T00:00:00Z")
    val rows = listOf(
        TaskRow(
            task = Task.create(
                projectId = "p1",
                title = "Revisar relatório mensal",
                priority = TaskPriority.HIGH,
                status = TaskStatus.DOING,
                now = now,
            ),
            projectName = "Projeto Demo",
        ),
        TaskRow(
            task = Task.create(
                projectId = "p2",
                title = "Atualizar wireframes low-fi",
                priority = TaskPriority.URGENT,
                status = TaskStatus.TODO,
                now = now,
            ),
            projectName = "BrainOut",
        ),
        TaskRow(
            task = Task.create(
                projectId = "p1",
                title = "Subir release 0.1.0-alpha02",
                priority = TaskPriority.CRITICAL,
                status = TaskStatus.DONE,
                now = now,
            ),
            projectName = "Projeto Demo",
        ),
    )
    TasksContent(state = TasksUiState(rows = rows, isLoading = false))
}
