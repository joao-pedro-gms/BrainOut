// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Orquestrador da tela de detalhe do projeto (E2.2/E2.4/E2.8/E3.4):
// Scaffold + coleta de estado + snackbar de erro efêmero (RN01) +
// roteamento dos diálogos e callbacks do ViewModel. As seções vivem
// nos irmãos ProjectDetail*.kt.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoCreateActionButton
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
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

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Snackbar de erro com auto-dismiss em 5s (RN01, título inválido).
    // E2.8 — falhas de carga (Room) são expostas via banner de erro
    // (não snackbar) para evitar duplicação visual; as demais mensagens
    // transitórias (RN01, validação) passam pelo snackbar.
    // Falhas de carga (Room) aparecem no banner, não no snackbar; as
    // demais (RN01, validação, gravação) passam pelo snackbar.
    val resolvedMessage =
        errorMessage
            ?.takeUnless { it is ProjectDetailError.LoadFailed }
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
    // DEF-52 — o estado salvo guarda o **id** da tarefa, não o `Task`: a
    // classe `Task` não tem `Saver`, então girar o aparelho com o diálogo
    // aberto o fechava e perdia a tarefa selecionada.
    val priorityDialogTaskId = rememberEditedTaskId(key = "priorityDialogTaskId")
    // DEF-22 — tarefa selecionada para renomear; não-nula abre
    // [RenameTaskDialog]. É o que torna `renameTask` alcançável.
    val renameTaskId = rememberEditedTaskId(key = "renameTaskId")

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("project_detail_$projectId"),
        topBar = {
            ProjectDetailTopBar(
                onBackClicked = onBackClicked,
                onDeleteClicked = { showDeleteProjectDialog = true },
            )
        },
        floatingActionButton = {
            NeoCreateActionButton(
                onClick = { showCreateDialog = true },
                label = stringResource(R.string.project_detail_new_task),
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
            contentPadding = innerPadding,
            state =
                ProjectDetailState(
                    tasks = state.tasks,
                    isLoading = state.isLoading,
                    errorMessage = state.errorMessage,
                ),
            actions =
                ProjectDetailActions(
                    onRetry = viewModel::retry,
                    onDismissError = viewModel::clearError,
                    onChangeStatus = viewModel::changeStatus,
                    onDeleteTask = viewModel::deleteTask,
                    onRenameTask = { renameTaskId.value = it.id },
                    onChangePriority = { task, _ ->
                        // RN02 — defesa em profundidade: a UI também bloqueia
                        // tarefas DONE via DropdownMenuItem(enabled = false).
                        if (task.status != TaskStatus.DONE) {
                            priorityDialogTaskId.value = task.id
                        }
                    },
                ),
        )
    }

    if (showCreateDialog) {
        NewTaskDialog(
            errorMessage = resolvedMessage,
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, priority, dueDate ->
                viewModel.addTask(title, priority, dueDate) { showCreateDialog = false }
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

    val priorityTarget =
        remember(priorityDialogTaskId.value, state.tasks) {
            taskById(state.tasks, priorityDialogTaskId.value)
        }
    if (priorityTarget != null) {
        ChangeTaskPriorityDialog(
            errorMessage = resolvedMessage,
            currentPriority = priorityTarget.priority,
            onDismiss = { priorityDialogTaskId.value = null },
            onConfirm = { newPriority ->
                viewModel.changeTaskPriority(priorityTarget, newPriority) { priorityDialogTaskId.value = null }
            },
        )
    }

    val renameTarget =
        remember(renameTaskId.value, state.tasks) {
            taskById(state.tasks, renameTaskId.value)
        }
    if (renameTarget != null) {
        RenameTaskDialog(
            errorMessage = resolvedMessage,
            initialTitle = renameTarget.title,
            onDismiss = { renameTaskId.value = null },
            onConfirm = { newTitle ->
                viewModel.renameTask(renameTarget, newTitle) { renameTaskId.value = null }
            },
        )
    }
}

/**
 * Id da tarefa em edição (prioridade ou renomear), guardado no estado
 * salvo da Activity.
 *
 * DEF-52 — o que fica salvo é o **id**, não o `Task`: a classe `Task`
 * não tem `Saver`, então girar o aparelho com o diálogo de edição
 * aberto o fechava e perdia a tarefa selecionada.
 *
 * A chave é explícita porque dois diálogos usam este estado: sem ela,
 * o `rememberSaveable` deriva a chave da posição do código e os dois
 * compartilhariam o mesmo slot.
 */
@Composable
internal fun rememberEditedTaskId(key: String): MutableState<String?> =
    rememberSaveable(key = key) { mutableStateOf(null) }

/**
 * Resolve a tarefa em edição pelo id guardado no estado salvo.
 *
 * Devolve `null` quando a tarefa saiu da lista (foi excluída no
 * meantime): o diálogo fecha sozinho, em vez de operar sobre uma
 * cópia obsoleta da tarefa.
 */
internal fun taskById(
    tasks: List<Task>,
    taskId: String?,
): Task? = taskId?.let { id -> tasks.firstOrNull { it.id == id } }

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProjectDetailScreenPreview() {
    BrainOutTheme {
        // O corpo, e não a tela: `ProjectDetailScreen` resolve o
        // ViewModel por `hiltViewModel()`, que exige uma Activity — no
        // IDE a preview não renderizava.
        ProjectDetailBody(
            contentPadding = PaddingValues(0.dp),
            state =
                ProjectDetailState(
                    tasks = emptyList(),
                    isLoading = false,
                    errorMessage = null,
                ),
            actions =
                ProjectDetailActions(
                    onRetry = {},
                    onDismissError = {},
                    onChangeStatus = { _, _ -> },
                    onDeleteTask = {},
                    onRenameTask = {},
                    onChangePriority = { _, _ -> },
                ),
        )
    }
}
