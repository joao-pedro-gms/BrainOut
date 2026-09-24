// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Orquestrador da tela de detalhe do projeto (E2.2/E2.4/E2.8/E3.4):
// Scaffold + coleta de estado + snackbar de erro efêmero (RN01) +
// roteamento dos diálogos e callbacks do ViewModel. As seções vivem
// nos irmãos ProjectDetail*.kt.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

private const val ERROR_AUTO_DISMISS_MS: Long = 5_000L

/**
 * Tela de detalhe do projeto (E2.2).
 *
 * @param projectId identificador do projeto vindo da rota
 *  `project/{projectId}`. Alimenta o [ProjectDetailViewModel] via
 *  `SavedStateHandle`.
 * @param onBackClicked disparado pelo botão de voltar (top app bar).
 *  Deve chamar `popBackStack()` no NavHost.
 * @param onProjectDeleted disparado após a exclusão do projeto.
 *  Por padrão, delega para `onBackClicked`.
 */
@Suppress("LongMethod") // Orquestrador: coleta 3 flows e roteia 3 diálogos.
@Composable
fun ProjectDetailScreen(
    projectId: String,
    onBackClicked: () -> Unit,
    onProjectDeleted: () -> Unit = onBackClicked,
    modifier: Modifier = Modifier,
    viewModel: ProjectDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Snackbar de erro com auto-dismiss em 5s (RN01, título inválido).
    // E2.8 — falhas de carga (Room) são expostas via banner de erro
    // (não snackbar) para evitar duplicação visual; as demais mensagens
    // transitórias (RN01, validação) passam pelo snackbar.
    val isLoadError = errorMessage?.let { ProjectDetailViewModel.isLoadErrorMessage(it) } == true
    val resolvedMessage = errorMessage
        ?.takeUnless { isLoadError }
        ?.let { resolveProjectDetailMessage(it) }
    LaunchedEffect(resolvedMessage) {
        val message = resolvedMessage
        if (!message.isNullOrBlank()) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message)
            }
            delay(ERROR_AUTO_DISMISS_MS)
            viewModel.clearError()
        }
    }

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteProjectDialog by rememberSaveable { mutableStateOf(false) }
    // Tarefa selecionada para edição de prioridade (RN02); não-nula abre
    // [ChangeTaskPriorityDialog]. Só tarefas ativas chegam aqui (UI filtra).
    var priorityDialogTask by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ProjectDetailTopBar(
                onBackClicked = onBackClicked,
                onDeleteClicked = { showDeleteProjectDialog = true },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                },
                text = {
                    Text(text = stringResource(id = R.string.project_detail_new_task))
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag(ProjectDetailTestTags.NEW_TASK_FAB),
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        ProjectDetailBody(
            projectId = projectId,
            tasks = state.tasks,
            isLoading = state.isLoading,
            errorMessage = state.errorMessage,
            onRetry = viewModel::retry,
            onDismissError = viewModel::clearError,
            contentPadding = innerPadding,
            onChangeStatus = viewModel::changeStatus,
            onDeleteTask = viewModel::deleteTask,
            onRenameTask = viewModel::renameTask,
            syncState = syncState,
            onChangePriority = { task, _ ->
                // RN02 — defesa em profundidade: a UI também bloqueia
                // tarefas DONE via DropdownMenuItem(enabled = false).
                if (task.status != TaskStatus.DONE) {
                    priorityDialogTask = task
                }
            },
        )
    }

    if (showCreateDialog) {
        NewTaskDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, priority, dueDate ->
                viewModel.addTask(title, priority, dueDate)
                showCreateDialog = false
            },
            evaluateDeadline = { deadline -> viewModel.evaluateDeadline(deadline) },
        )
    }

    if (showDeleteProjectDialog) {
        DeleteProjectDialog(
            onDismiss = { showDeleteProjectDialog = false },
            onConfirm = {
                showDeleteProjectDialog = false
                viewModel.deleteProject(onDone = onProjectDeleted)
            },
        )
    }

    val priorityTarget = priorityDialogTask
    if (priorityTarget != null) {
        ChangeTaskPriorityDialog(
            currentPriority = priorityTarget.priority,
            onDismiss = { priorityDialogTask = null },
            onConfirm = { newPriority ->
                viewModel.changeTaskPriority(priorityTarget, newPriority)
                priorityDialogTask = null
            },
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProjectDetailScreenPreview() {
    ProjectDetailScreen(
        projectId = "demo-project",
        onBackClicked = {},
    )
}
