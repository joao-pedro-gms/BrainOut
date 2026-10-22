// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Tela "Tarefas" (TasksScreen) — aba exibida quando o usuário seleciona
// o item "Tarefas" na bottom bar da HomeScreen. Lista todas as tarefas
// do owner ativo, agrupadas por projeto e com chips de status/prioridade.
//
// Marco E2.6 do ROADMAP.md.

package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoEmptyState
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoErrorBanner
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoPriorityLegend
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoTaskPriorityChip
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoTaskStatusChip
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.LocalNeoWindowInfo
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoMotion
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.neoContentWidth
import pucgo.joaopedrogmsilva.brainout.feature.tasks.R

/**
 * Tela "Tarefas" do BrainOut — listagem global das tarefas do owner
 * ativo, com join do nome do projeto para exibição inline.
 *
 * Renderização (E2.8 — estados de erro):
 * - `isLoading = true` e sem erro → [CircularProgressIndicator] centralizado.
 * - `errorMessage != null` → banner com "Tentar novamente" (prioridade sobre
 *   empty state para evitar lista vazia enganosa).
 * - lista vazia → cartão com mensagem explicando o que fazer.
 * - lista populada → [LazyColumn] de cards, cada card exibe título,
 *   projeto, prioridade e status via chips.
 *
 * DEF-22 — a tela passou a usar `Scaffold` + [TasksTopBar]: antes ela não
 * tinha como sair, o título ficava sob a barra de status e a lista sob a
 * barra de gestos, porque `MainActivity` liga `enableEdgeToEdge()`.
 *
 * @param onBackClicked chamado pelo botão de voltar da [TasksTopBar].
 * @param viewModel injetado pelo Hilt; pode ser substituído por um
 *  fake nos `@Preview`/testes Compose.
 */
@Composable
fun TasksScreen(
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TasksTopBar(
                titleRes = R.string.tasks_screen_title,
                onBackClicked = onBackClicked,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        TasksContent(
            state = state,
            onRetry = viewModel::retry,
            onDismissError = viewModel::clearError,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/** Identificadores para testes de UI Compose. */
object TasksTestTags {
    const val LOADING: String = "tasks_loading"
    const val EMPTY: String = "tasks_empty"
    const val LIST: String = "tasks_list"

    // DEF-22 — a barra de topo com voltar é o que tira a tela do beco
    // sem saída; a tag dá ao teste um alvo para o clique.
    const val BACK_BUTTON: String = "tasks_back_button"
}

@Composable
private fun TasksContent(
    state: TasksUiState,
    onRetry: () -> Unit = {},
    onDismissError: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().neoContentWidth().padding(horizontal = LocalNeoWindowInfo.current.margin),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        item { NeoPriorityLegend() }
        item { HorizontalDivider(color = MaterialTheme.colorScheme.outline) }
        item {
            when {
                state.errorMessage != null -> TasksErrorBanner(state.errorMessage, onRetry, onDismissError)
                state.isLoading -> CircularProgressIndicator()
                state.rows.isEmpty() -> TasksEmptyState(Modifier.fillMaxWidth())
            }
        }
        if (!state.isLoading && state.errorMessage == null) {
            items(items = state.rows, key = { it.task.id }) { row ->
                Box(
                    Modifier.animateItem(
                        fadeInSpec = tween(NeoMotion.selection),
                        fadeOutSpec = tween(NeoMotion.selection),
                        placementSpec = tween(NeoMotion.selection),
                    ),
                ) {
                    TaskCard(row)
                }
            }
        }
    }
}

/**
 * Banner de erro com retry (E2.8). Mesma estrutura visual do
 * `HomeErrorBanner` em `:feature:projects`.
 *
 * DEF-26: a mensagem do [TasksViewModel] é usada em vez do texto fixo
 * — chave de erro conhecida vira recurso pt/en e o resto é exibida
 * como veio, sem esconder a causa atrás de "falha ao carregar".
 */
@Composable
internal fun TasksErrorBanner(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val texto =
        if (TasksViewModel.isLoadErrorMessage(message)) {
            stringResource(id = R.string.tasks_error_load_failed)
        } else {
            message
        }
    NeoErrorBanner(
        message = texto,
        onRetry = onRetry,
        onDismiss = onDismiss,
    )
}

@Composable
private fun TasksEmptyState(modifier: Modifier = Modifier) {
    NeoEmptyState(
        title = stringResource(id = R.string.tasks_empty_title),
        body = stringResource(id = R.string.tasks_empty_body),
        modifier = modifier.heightIn(min = 160.dp),
        bodyTextAlign = TextAlign.Center,
    )
}

@Composable
private fun TaskCard(row: TaskRow) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
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
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NeoTaskPriorityChip(priority = row.task.priority)
                NeoTaskStatusChip(status = row.task.status)
            }
        }
    }
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
    val rows =
        listOf(
            TaskRow(
                task =
                    Task.create(
                        projectId = "p1",
                        title = "Revisar relatório mensal",
                        priority = TaskPriority.HIGH,
                        status = TaskStatus.DOING,
                        now = now,
                    ),
                projectName = "Projeto Demo",
            ),
            TaskRow(
                task =
                    Task.create(
                        projectId = "p2",
                        title = "Atualizar wireframes low-fi",
                        priority = TaskPriority.URGENT,
                        status = TaskStatus.TODO,
                        now = now,
                    ),
                projectName = "BrainOut",
            ),
            TaskRow(
                task =
                    Task.create(
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
