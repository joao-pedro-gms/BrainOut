// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Tela de detalhe do projeto — lista de tarefas observada do Room,
// criação via AlertDialog, ações via menu (mover status / excluir),
// exclusão do projeto e snackbar de erro efêmero (RN01).
//
// Telas Compose legítimas concentram muitos composables pequenos
// em um único arquivo; suprimimos TooManyFunctions para manter a
// coesão da feature em vez de dispersar widgets correlatos.

@file:Suppress("TooManyFunctions", "LongParameterList", "LongMethod")

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.MAX_ACTIVE_TASKS_PER_PROJECT
import pucgo.joaopedrogmsilva.brainout.feature.projects.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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
@OptIn(ExperimentalMaterial3Api::class)
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
    // A mensagem vem do ViewModel em texto cru (constantes do domínio/VM).
    // A resolução para o recurso localizado acontece fora do efeito, no
    // contexto de composição (stringResource exige composição); o efeito
    // apenas consome o texto já resolvido quando `errorMessage` muda.
    val resolvedMessage = errorMessage?.let { resolveProjectDetailMessage(it) }
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
    // Tarefa atualmente selecionada para ter a prioridade editada;
    // quando não-nula, o diálogo [ChangeTaskPriorityDialog] é
    // aberto. RN02 — só tarefas ativas chegam aqui (UI filtra).
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
            contentPadding = innerPadding,
            onChangeStatus = viewModel::changeStatus,
            onDeleteTask = viewModel::deleteTask,
            onRenameTask = viewModel::renameTask,
            onChangePriority = { task, _ ->
                // RN02 — defesa em profundidade: a UI também bloqueia
                // tarefas DONE via DropdownMenuItem(enabled = false);
                // este `if` evita uma corrida em que alguém construa
                // um diálogo por outro caminho antes do menu fechar.
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectDetailTopBar(
    onBackClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.project_detail_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClicked) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(id = R.string.common_back),
                )
            }
        },
        actions = {
            IconButton(
                onClick = onDeleteClicked,
                modifier = Modifier.testTag(ProjectDetailTestTags.DELETE_PROJECT_BUTTON),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(id = R.string.project_detail_delete_project),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

/** Identificadores usados por testes Compose. */
object ProjectDetailTestTags {
    const val NEW_TASK_FAB: String = "project_detail_new_task_fab"
    const val DELETE_PROJECT_BUTTON: String = "project_detail_delete_project_button"
    const val NEW_TASK_DIALOG: String = "project_detail_new_task_dialog"
    const val NEW_TASK_TITLE_FIELD: String = "project_detail_new_task_title"
    const val NEW_TASK_SAVE: String = "project_detail_new_task_save"
    const val NEW_TASK_CANCEL: String = "project_detail_new_task_cancel"
    const val NEW_TASK_DUE_DATE_FIELD: String = "project_detail_new_task_due_date_field"
    const val NEW_TASK_DUE_DATE_FIELD_OVERLAY: String = "project_detail_new_task_due_date_overlay"
    const val NEW_TASK_HOLIDAY_HINT: String = "project_detail_new_task_holiday_hint"
    const val TASK_ITEM_MENU: String = "project_detail_task_item_menu"
    const val TASK_ITEM_MENU_DELETE: String = "project_detail_task_item_menu_delete"
    const val TASK_ITEM_MENU_MOVE_DOING: String = "project_detail_task_item_menu_move_doing"
    const val TASK_ITEM_MENU_MOVE_DONE: String = "project_detail_task_item_menu_move_done"
    const val TASK_ITEM_MENU_MOVE_TODO: String = "project_detail_task_item_menu_move_todo"
    const val TASK_ITEM_MENU_CHANGE_PRIORITY: String = "project_detail_task_item_menu_change_priority"
    const val TASK_PRIORITY_DIALOG: String = "project_detail_task_priority_dialog"
    const val TASK_PRIORITY_CHIP_OPTION_PREFIX: String = "project_detail_task_priority_option_"
}

private const val ERROR_AUTO_DISMISS_MS: Long = 5_000L

@Composable
private fun ProjectDetailBody(
    projectId: String,
    tasks: List<Task>,
    isLoading: Boolean,
    contentPadding: PaddingValues,
    onChangeStatus: (taskId: String, target: TaskStatus) -> Unit,
    onDeleteTask: (Task) -> Unit,
    @Suppress("unused") onRenameTask: (Task, String) -> Unit,
    onChangePriority: (Task, TaskPriority) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(id = R.string.project_detail_id_label, projectId),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(id = R.string.project_detail_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        Text(
            text = stringResource(id = R.string.project_detail_tasks_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        if (tasks.isEmpty() && !isLoading) {
            EmptyTasksCard()
        } else {
            tasks.forEach { task ->
                TaskRow(
                    task = task,
                    onChangeStatus = onChangeStatus,
                    onDeleteTask = onDeleteTask,
                    onChangePriority = onChangePriority,
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp)) // espaço para o FAB
    }
}

@Composable
private fun TaskRow(
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
    val labelRes = when (priority) {
        TaskPriority.LOW -> R.string.task_priority_low
        TaskPriority.MEDIUM -> R.string.task_priority_medium
        TaskPriority.HIGH -> R.string.task_priority_high
        TaskPriority.URGENT -> R.string.task_priority_urgent
        TaskPriority.CRITICAL -> R.string.task_priority_critical
    }
    AssistChip(
        onClick = { /* priority chip é apenas informativo */ },
        label = {
            Text(text = stringResource(id = labelRes), style = MaterialTheme.typography.labelSmall)
        },
        // E4.4: 48dp mínimo (WCAG 2.5.5 Target Size).
        modifier = Modifier.heightIn(min = 48.dp),
    )
}

@Composable
private fun EmptyTasksCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.project_detail_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.project_detail_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, priority: TaskPriority, dueDate: Instant?) -> Unit,
    evaluateDeadline: suspend (Instant?) -> pucgo.joaopedrogmsilva.brainout.core.domain.usecase.DeadlineInfo,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf(TaskPriority.MEDIUM) }
    var priorityMenuExpanded by remember { mutableStateOf(false) }
    // E3.5: prazo opcional em UTC (midnight na zona do usuário).
    var dueDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dueDate: Instant? = remember(dueDateMillis) {
        dueDateMillis?.let {
            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toInstant()
        }
    }
    // Captura a string do padrão dd/MM/yyyy no escopo @Composable.
    val dueDatePattern = stringResource(id = R.string.project_detail_new_task_due_date_format)
    val dueDateFormatter = remember(dueDatePattern) {
        DateTimeFormatter.ofPattern(dueDatePattern)
    }
    val dueDateLabel: String = remember(dueDateMillis, dueDateFormatter) {
        if (dueDateMillis == null) {
            "" // será substituído pelo placeholder
        } else {
            val localDate = Instant.ofEpochMilli(dueDateMillis!!)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
            dueDateFormatter.format(localDate)
        }
    }

    // E3.5: dica inline — busca o próximo feriado via use case quando
    // o prazo muda. Falha silenciosa (use case degrada para lista vazia).
    var holidayHint by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(dueDate) {
        holidayHint = if (dueDate == null) {
            null
        } else {
            val info = evaluateDeadline(dueDate)
            val next = info.nextHoliday
            if (next != null) {
                val dateText = dueDateFormatter.format(next.date)
                "⚠ Próximo feriado: ${next.name} ($dateText) antes do prazo"
            } else {
                null
            }
        }
    }

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
                        color = androidx.compose.ui.graphics.Color.Transparent,
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
                // E3.5: campo de prazo opcional + dica inline.
                OutlinedTextField(
                    value = dueDateLabel,
                    onValueChange = { /* read-only — vem do DatePicker */ },
                    readOnly = true,
                    label = { Text(text = stringResource(id = R.string.project_detail_new_task_due_date_label)) },
                    placeholder = {
                        Text(text = stringResource(id = R.string.project_detail_new_task_due_date_clear))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ProjectDetailTestTags.NEW_TASK_DUE_DATE_FIELD),
                )
                Box {
                    Surface(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(4.dp))
                            .testTag(ProjectDetailTestTags.NEW_TASK_DUE_DATE_FIELD_OVERLAY),
                        color = androidx.compose.ui.graphics.Color.Transparent,
                        content = {},
                        onClick = { showDatePicker = true },
                    )
                }
                if (holidayHint != null) {
                    Text(
                        text = holidayHint!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(ProjectDetailTestTags.NEW_TASK_HOLIDAY_HINT),
                    )
                }
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

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dueDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dueDateMillis = pickerState.selectedDateMillis ?: dueDateMillis
                        showDatePicker = false
                    },
                ) { Text(text = stringResource(id = R.string.project_detail_new_task_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(id = R.string.project_detail_new_task_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun DeleteProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.project_detail_delete_project)) },
        text = {
            Text(text = stringResource(id = R.string.project_detail_description))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(id = R.string.project_detail_delete_project),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.common_close))
            }
        },
    )
}

/**
 * Diálogo de alteração de prioridade (RN02 — E2.4). Só abre para
 * tarefas ativas (TODO/DOING); a UI não oferece a ação para
 * tarefas concluídas — mas o domínio também bloquearia o update
 * por defesa em profundidade.
 *
 * Mostra a prioridade atual pré-selecionada como radio do
 * `ChipGroup`. `FilterChip` é usado para um visual com check, e
 * a `onConfirm(newPriority)` é chamada ao tocar em "Salvar".
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ChangeTaskPriorityDialog(
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
            androidx.compose.foundation.layout.FlowRow(
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

/** Helper para mapear [TaskPriority] ao recurso de string correspondente. */
@Composable
private fun TaskPriority.labelRes(): Int = when (this) {
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

/**
 * Resolve a mensagem vinda do [ProjectDetailViewModel] para a versão
 * localizada em `strings.xml` quando ela casar com uma chave conhecida;
 * caso contrário devolve a própria mensagem (mesma estratégia de
 * `resolveAuthMessage` em `:feature:auth`, marco E4.6).
 */
@Composable
internal fun resolveProjectDetailMessage(message: String): String {
    val limit = MAX_ACTIVE_TASKS_PER_PROJECT
    return when {
        message == ProjectDetailViewModel.ERROR_EMPTY_TITLE ->
            stringResource(id = R.string.project_detail_error_empty_title)
        message == ProjectDetailViewModel.ERROR_INVALID_TITLE ->
            stringResource(id = R.string.project_detail_error_invalid_title)
        message == ProjectDetailViewModel.ERROR_INVALID_TRANSITION ->
            stringResource(id = R.string.project_detail_error_invalid_transition)
        message == ProjectDetailViewModel.ERROR_PRIORITY_LOCKED ||
            ProjectDetailViewModel.isPriorityLockedMessage(message) ->
            stringResource(id = R.string.project_detail_error_priority_locked_on_done)
        // RN01: mensagem cru do domínio inclui o id do projeto; casamos
        // por prefixo/sufixo e usamos o limite global no recurso localizado.
        ProjectDetailViewModel.isTaskLimitMessage(message) ->
            stringResource(id = R.string.project_detail_error_task_limit, limit)
        else -> message
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
