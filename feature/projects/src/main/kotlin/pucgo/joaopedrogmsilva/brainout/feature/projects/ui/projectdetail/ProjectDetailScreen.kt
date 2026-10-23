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
import androidx.compose.material3.Surface
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.DeadlineInfo
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoCreateActionButton
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.feature.projects.R
import java.time.Instant

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

    // E2.8 — falhas de carga (Room) são expostas via banner de erro (não
    // snackbar) para evitar duplicação visual; as demais mensagens
    // transitórias (RN01, validação) passam pelo snackbar.
    val resolvedMessage = resolveTransientMessage(errorMessage)
    val snackbarHostState = ProjectDetailSnackbarEffect(resolvedMessage, viewModel::clearError)

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteProjectDialog by rememberSaveable { mutableStateOf(false) }
    // Tarefa selecionada para edição de prioridade (RN02); não-nula abre
    // [ChangeTaskPriorityDialog]. Só tarefas ativas chegam aqui (UI filtra).
    // DEF-52 — o estado salvo guarda o **id** da tarefa, não o `Task`: a
    // classe `Task` não tem `Saver`, então girar o aparelho com o diálogo
    // aberto o fechava e perdia a tarefa selecionada.
    val priorityDialogTaskId = rememberPriorityDialogTaskId()
    // DEF-22 — tarefa selecionada para renomear; não-nula abre
    // [RenameTaskDialog]. É o que torna `renameTask` alcançável.
    val renameTaskId = rememberRenameTaskId()

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
        snackbarHost = { ProjectDetailSnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        ProjectDetailContent(
            contentPadding = innerPadding,
            state = state,
            viewModel = viewModel,
            onRenameTask = { renameTaskId.value = it.id },
            onChangePriority = { task ->
                // RN02 — defesa em profundidade: a UI também bloqueia
                // tarefas DONE via DropdownMenuItem(enabled = false).
                if (task.status != TaskStatus.DONE) {
                    priorityDialogTaskId.value = task.id
                }
            },
        )
    }

    NewTaskDialogHost(
        visible = showCreateDialog,
        errorMessage = resolvedMessage,
        onDismiss = { showCreateDialog = false },
        onConfirm = { title, priority, dueDate ->
            viewModel.addTask(title, priority, dueDate) { showCreateDialog = false }
        },
        evaluateDeadline = { deadline -> viewModel.evaluateDeadline(deadline) },
    )

    DeleteProjectDialogHost(
        visible = showDeleteProjectDialog,
        onDismiss = { showDeleteProjectDialog = false },
        onConfirm = {
            showDeleteProjectDialog = false
            viewModel.deleteProject(onDone = onProjectDeleted)
        },
    )

    ChangeTaskPriorityDialogHost(
        taskId = priorityDialogTaskId.value,
        tasks = state.tasks,
        errorMessage = resolvedMessage,
        onDismiss = { priorityDialogTaskId.value = null },
        onConfirm = { task, newPriority ->
            viewModel.changeTaskPriority(task, newPriority) { priorityDialogTaskId.value = null }
        },
    )

    RenameTaskDialogHost(
        taskId = renameTaskId.value,
        tasks = state.tasks,
        errorMessage = resolvedMessage,
        onDismiss = { renameTaskId.value = null },
        onConfirm = { task, newTitle ->
            viewModel.renameTask(task, newTitle) { renameTaskId.value = null }
        },
    )
}

/**
 * O host do snackbar de erro transitório.
 */
@Composable
private fun ProjectDetailSnackbarHost(snackbarHostState: SnackbarHostState) {
    SnackbarHost(snackbarHostState) { data ->
        Snackbar(snackbarData = data)
    }
}

/**
 * O diálogo de criar tarefa, aberto enquanto o botão de criação estiver
 * ativo.
 */
@Composable
private fun NewTaskDialogHost(
    visible: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, priority: TaskPriority, dueDate: Instant?) -> Unit,
    evaluateDeadline: suspend (Instant?) -> DeadlineInfo,
) {
    if (!visible) return
    NewTaskDialog(
        errorMessage = errorMessage,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        evaluateDeadline = evaluateDeadline,
    )
}

/**
 * O diálogo de excluir o projeto, aberto enquanto o botão de exclusão
 * estiver ativo.
 */
@Composable
private fun DeleteProjectDialogHost(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!visible) return
    DeleteProjectDialog(
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

/**
 * O corpo da tela: o estado e as ações que o [ProjectDetailBody] recebe.
 */
@Composable
private fun ProjectDetailContent(
    contentPadding: PaddingValues,
    state: ProjectDetailUiState,
    viewModel: ProjectDetailViewModel,
    onRenameTask: (Task) -> Unit,
    onChangePriority: (Task) -> Unit,
) {
    ProjectDetailBody(
        contentPadding = contentPadding,
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
                onRenameTask = onRenameTask,
                onChangePriority = { task, _ -> onChangePriority(task) },
            ),
    )
}

/**
 * A mensagem transitória do snackbar.
 *
 * E2.8 — as falhas de carga do Room aparecem no **banner** de erro, e
 * não aqui: repetir a mesma falha nos dois lugares seria duplicação
 * visual. As demais (RN01, validação, gravação) passam pelo snackbar.
 */
@Composable
private fun resolveTransientMessage(errorMessage: ProjectDetailError?): String? =
    errorMessage
        ?.takeUnless { it is ProjectDetailError.LoadFailed }
        ?.let { resolveProjectDetailMessage(it) }

/**
 * O snackbar de erro transitório, com auto-dismiss em 5 s (RN01).
 *
 * Devolve o estado do host, para que a tela não precise criá-lo: o
 * efeito e o host nascem juntos.
 */
@Composable
private fun ProjectDetailSnackbarEffect(
    message: String?,
    onDismiss: () -> Unit,
): SnackbarHostState {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message)
            }
            delay(ERROR_AUTO_DISMISS_MS)
            onDismiss()
        }
    }
    return snackbarHostState
}

/**
 * O diálogo de prioridade, aberto enquanto houver uma tarefa em edição.
 *
 * O alvo é resolvido pelo **id** guardado (DEF-52): a `Task` não tem
 * `Saver`, então o estado salvo guarda o id e a lista viva re-resolve.
 */
@Composable
private fun ChangeTaskPriorityDialogHost(
    taskId: String?,
    tasks: List<Task>,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (Task, TaskPriority) -> Unit,
) {
    val target = remember(taskId, tasks) { taskById(tasks, taskId) }
    if (target != null) {
        ChangeTaskPriorityDialog(
            errorMessage = errorMessage,
            currentPriority = target.priority,
            onDismiss = onDismiss,
            onConfirm = { newPriority -> onConfirm(target, newPriority) },
        )
    }
}

/**
 * O diálogo de renomear, aberto enquanto houver uma tarefa em edição.
 *
 * O alvo é resolvido pelo **id** guardado (DEF-52), como no diálogo de
 * prioridade.
 */
@Composable
private fun RenameTaskDialogHost(
    taskId: String?,
    tasks: List<Task>,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (Task, String) -> Unit,
) {
    val target = remember(taskId, tasks) { taskById(tasks, taskId) }
    if (target != null) {
        RenameTaskDialog(
            errorMessage = errorMessage,
            initialTitle = target.title,
            onDismiss = onDismiss,
            onConfirm = { newTitle -> onConfirm(target, newTitle) },
        )
    }
}

/**
 * O id da tarefa em edição no diálogo de prioridade (RN02), guardado no
 * estado salvo da Activity.
 *
 * DEF-52 — o que fica salvo é o **id**, não o `Task`: a classe `Task`
 * não tem `Saver`, então girar o aparelho com o diálogo de edição
 * aberto o fechava e perdia a tarefa selecionada.
 *
 * O id fica no estado salvo pelo escopo posicional do `rememberSaveable`
 * — o runtime do Compose gera uma chave única para cada local de código
 * exato da composição. A versão anterior passava uma chave manual
 * (`rememberSaveable(key = ...)`), que o Compose depreciou por burlar
 * esse escopo.
 */
@Composable
internal fun rememberPriorityDialogTaskId(): MutableState<String?> = rememberSaveable { mutableStateOf<String?>(null) }

/**
 * O id da tarefa em edição no diálogo de renomear (DEF-22), guardado no
 * estado salvo da Activity.
 *
 * Fica em uma função própria — e não em um `rememberSaveable` a mais ao
 * lado do de prioridade — porque a separação dos dois slots depende do
 * local de código: duas chamadas no mesmo corpo compartilhariam o
 * mesmo local e, portanto, o mesmo slot.
 */
@Composable
internal fun rememberRenameTaskId(): MutableState<String?> = rememberSaveable { mutableStateOf<String?>(null) }

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
        Surface(color = MaterialTheme.colorScheme.background) {
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
}
