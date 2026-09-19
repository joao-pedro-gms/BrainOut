// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Tela Home do BrainOut — agora com usuário real (E1.7), badge de
// papel colorido por role, FAB gated pelo papel (Owner habilita,
// Member abre diálogo explicativo) e lista real de projetos com
// chips de tag (E2.1/E2.6).

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

/**
 * Tela Home do BrainOut — agora com usuário real (E1.7), badge de
 * papel colorido por role, e FAB gated pelo papel (Owner habilita,
 * Member abre diálogo explicativo).
 *
 * A bottom bar tem 3 destinos, todos como navegação externa (E2.6):
 * - [HomeTab.Projects] é a aba inicial; alterna o conteúdo do
 *   Scaffold (estado salvo via [rememberSaveable]).
 * - [HomeTab.Tasks] e [HomeTab.Settings] disparam callbacks
 *   ([onOpenTasks], [onOpenSettings]) que vivem no `BrainOutNavHost`
 *   em :app. O destino "Tarefas" mora em :feature:tasks
 *   (`TasksRoutes.TASKS`).
 *
 * @param onOpenProject chamado quando o usuário toca em um card de
 *  projeto (não dispara no E1.6 porque a lista está vazia; reservado
 *  para E2.1).
 * @param onOpenSettings chamado quando o usuário seleciona a tab
 *  "Configurações" da bottom bar — dispara `navigate(settings)` no
 *  `NavHost` externo.
 * @param onOpenTasks chamado quando o usuário seleciona a tab
 *  "Tarefas" da bottom bar — dispara `navigate(tasks)` no `NavHost`
 *  externo (rota `TasksRoutes.TASKS` em :feature:tasks).
 * @param viewModel injetado pelo Hilt; pode ser substituído por um
 *  fake nos `@Preview`/testes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenProject: (projectId: String) -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenTasks: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val userState by viewModel.userState.collectAsStateWithLifecycle()
    val upgradeDialogVisible by viewModel.upgradeDialogVisible.collectAsStateWithLifecycle()
    val listState by viewModel.uiState.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(HomeTab.Projects) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }

    val homeUser: HomeUser = when (val current = userState) {
        HomeUserState.Loading, HomeUserState.SignedOut -> DefaultHomeUser
        is HomeUserState.SignedIn -> HomeUser(
            displayName = current.displayName,
            initials = current.initials,
            role = current.role,
        )
    }

    val isOwner = homeUser.role == HomeUserRole.Owner

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            HomeTopBar(user = homeUser)
        },
        bottomBar = {
            HomeBottomBar(
                currentTab = currentTab,
                onSelectTab = { selected ->
                    when (selected) {
                        HomeTab.Settings -> onOpenSettings()
                        HomeTab.Tasks -> onOpenTasks()
                        HomeTab.Projects -> currentTab = selected
                    }
                }
            )
        },
        floatingActionButton = {
            HomeFloatingActionButton(
                isOwner = isOwner,
                onCreateProjectClicked = { showCreateDialog = true },
                onMemberFabClicked = viewModel::onMemberFabClicked,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        // Apenas a aba "Projetos" tem conteúdo interno. As outras duas
        // (Tarefas, Configurações) disparam navegação externa via
        // callback — quando ela termina, a `HomeScreen` deixa de
        // existir na pilha. Esta branch existe apenas para satisfazer
        // a exaustividade do `when`.
        when (currentTab) {
            HomeTab.Projects -> HomeProjectsContent(
                contentPadding = innerPadding,
                onOpenProject = onOpenProject,
                projects = listState.projects,
                isLoading = listState.isLoading,
            )
            HomeTab.Tasks -> Unit
            HomeTab.Settings -> Unit
        }
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            availableTags = listState.availableTags,
            onDismiss = { showCreateDialog = false },
            onCreateTag = { name, color -> viewModel.createTag(name, color) },
            onCreateProject = { name, description, tagIds ->
                viewModel.createProject(name, description, tagIds)
                showCreateDialog = false
            },
        )
    }

    if (upgradeDialogVisible) {
        UpgradeDialog(onDismiss = viewModel::dismissUpgradeDialog)
    }
}

/** Identificadores usados por testes Compose. */
object HomeTestTags {
    const val FAB: String = "home_fab"
    const val ROLE_BADGE: String = "home_role_badge"
    const val UPGRADE_DIALOG: String = "home_upgrade_dialog"
    const val UPGRADE_DIALOG_ACK: String = "home_upgrade_dialog_ack"
    const val CREATE_PROJECT_DIALOG: String = "home_create_project_dialog"
    const val CREATE_PROJECT_NAME: String = "home_create_project_name"
    const val CREATE_PROJECT_DESCRIPTION: String = "home_create_project_description"
    const val CREATE_PROJECT_CONFIRM: String = "home_create_project_confirm"
    const val CREATE_PROJECT_ADD_TAG: String = "home_create_project_add_tag"
    const val PROJECT_CARD: String = "home_project_card"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(user: HomeUser) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = user.initials,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column {
                    Text(
                        text = stringResource(id = R.string.home_topbar_greeting, user.displayName),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    val badgeLabel = when (user.role) {
                        HomeUserRole.Owner -> stringResource(id = R.string.home_role_badge_owner)
                        HomeUserRole.Member -> stringResource(id = R.string.home_role_badge_member)
                    }
                    val roleContainer: Color = when (user.role) {
                        HomeUserRole.Owner -> MaterialTheme.colorScheme.primary
                        HomeUserRole.Member -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val roleLabel: Color = when (user.role) {
                        HomeUserRole.Owner -> MaterialTheme.colorScheme.onPrimary
                        HomeUserRole.Member -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    AssistChip(
                        onClick = { /* badge é apenas decorativo */ },
                        label = {
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.testTag(HomeTestTags.ROLE_BADGE),
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = roleContainer,
                            labelColor = roleLabel,
                        ),
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

@Composable
private fun HomeBottomBar(
    currentTab: HomeTab,
    onSelectTab: (HomeTab) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
    ) {
        HomeTab.values().forEach { tab ->
            val isSelected = tab == currentTab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon(),
                        contentDescription = null,
                    )
                },
                label = {
                    Text(
                        text = stringResource(id = tab.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                alwaysShowLabel = true,
            )
        }
    }
}

private fun HomeTab.icon(): ImageVector = when (this) {
    HomeTab.Projects -> Icons.Outlined.Folder
    HomeTab.Tasks -> Icons.AutoMirrored.Outlined.Assignment
    HomeTab.Settings -> Icons.Outlined.Settings
}

@Composable
private fun HomeFloatingActionButton(
    isOwner: Boolean,
    onCreateProjectClicked: () -> Unit,
    onMemberFabClicked: () -> Unit,
) {
    ExtendedFloatingActionButton(
        onClick = {
            if (isOwner) {
                onCreateProjectClicked()
            } else {
                onMemberFabClicked()
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(id = R.string.home_fab_create),
            )
        },
        text = {
            Text(
                text = if (isOwner) {
                    stringResource(id = R.string.home_fab_create)
                } else {
                    stringResource(id = R.string.home_fab_disabled_owner)
                },
                style = MaterialTheme.typography.labelLarge,
            )
        },
        containerColor = if (isOwner) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (isOwner) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.testTag(HomeTestTags.FAB),
    )
}

@Composable
private fun UpgradeDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(HomeTestTags.UPGRADE_DIALOG_ACK),
            ) {
                Text(text = stringResource(id = R.string.home_role_member_dialog_ack))
            }
        },
        title = {
            Text(text = stringResource(id = R.string.home_role_member_dialog_title))
        },
        text = {
            Text(text = stringResource(id = R.string.home_role_member_dialog_body))
        },
        modifier = Modifier.testTag(HomeTestTags.UPGRADE_DIALOG),
    )
}

@Composable
private fun HomeProjectsContent(
    contentPadding: PaddingValues,
    onOpenProject: (projectId: String) -> Unit,
    projects: List<ProjectCardItem>,
    isLoading: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(id = R.string.home_section_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        if (projects.isEmpty() && !isLoading) {
            HomeEmptyState(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = projects, key = { it.project.id }) { card ->
                    ProjectCard(
                        item = card,
                        onClick = { onOpenProject(card.project.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(
    item: ProjectCardItem,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.PROJECT_CARD)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = item.project.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            item.project.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item.tags.take(MAX_TAG_CHIPS_PREVIEW).forEach { chip ->
                        TagChipView(chip = chip, selected = true)
                    }
                }
            }
        }
    }
}

private const val MAX_TAG_CHIPS_PREVIEW: Int = 4

@Composable
private fun TagChipView(chip: TagChip, selected: Boolean) {
    val container = remember(chip.color) { parseHexColor(chip.color) }
    AssistChip(
        onClick = { /* chips de visualização não disparam ação */ },
        label = {
            Text(
                text = chip.name,
                style = MaterialTheme.typography.labelSmall,
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) container.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = AssistChipDefaults.assistChipBorder(
            enabled = true,
            borderColor = container,
        ),
    )
}

/**
 * Constantes para o parser de cor no formato `#RRGGBB` — mantidas
 * no nível de arquivo para serem reutilizadas e ficarem fora do
 * detekt MagicNumber.
 */
private const val HEX_COLOR_BASE: Int = 16
private const val HEX_COLOR_RED_SHIFT: Int = 16
private const val HEX_COLOR_GREEN_SHIFT: Int = 8
private const val HEX_COLOR_CHANNEL_MASK: Long = 0xFFL

/**
 * Faz parse manual de cor no formato `#RRGGBB` para evitar
 * `Color(android.graphics.Color.parseColor(...))` que lança em
 * hex inválido — neste ponto a validação já passou pelo domínio.
 */
private fun parseHexColor(hex: String): Color {
    val cleaned = hex.removePrefix("#")
    val value = cleaned.toLong(HEX_COLOR_BASE)
    val r = ((value shr HEX_COLOR_RED_SHIFT) and HEX_COLOR_CHANNEL_MASK).toInt()
    val g = ((value shr HEX_COLOR_GREEN_SHIFT) and HEX_COLOR_CHANNEL_MASK).toInt()
    val b = (value and HEX_COLOR_CHANNEL_MASK).toInt()
    return Color(red = r, green = g, blue = b)
}

@Composable
private fun CreateProjectDialog(
    availableTags: List<TagChip>,
    onDismiss: () -> Unit,
    onCreateTag: (name: String, color: String) -> Unit,
    onCreateProject: (name: String, description: String?, tagIds: List<String>) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    val selectedTagIds = rememberSaveable(saver = androidx.compose.runtime.saveable.listSaver(
        save = { it.toList() },
        restore = { it.toMutableStateList() },
    )) { mutableStateListOf<String>() }
    var showAddTag by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.home_create_project_title)) },
        text = {
            CreateProjectDialogBody(
                name = name,
                onNameChange = { name = it },
                description = description,
                onDescriptionChange = { description = it },
                availableTags = availableTags,
                selectedTagIds = selectedTagIds,
                onShowAddTag = { showAddTag = true },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedName = name.trim()
                    if (trimmedName.isEmpty()) return@TextButton
                    onCreateProject(
                        trimmedName,
                        description.trim().takeIf { it.isNotEmpty() },
                        selectedTagIds.toList(),
                    )
                },
                modifier = Modifier.testTag(HomeTestTags.CREATE_PROJECT_CONFIRM),
            ) {
                Text(text = stringResource(id = R.string.home_create_project_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.common_close))
            }
        },
        modifier = Modifier.testTag(HomeTestTags.CREATE_PROJECT_DIALOG),
    )

    if (showAddTag) {
        AddTagDialog(
            onDismiss = { showAddTag = false },
            onCreate = { tagName, color ->
                onCreateTag(tagName, color)
                showAddTag = false
            },
        )
    }
}

@Composable
private fun CreateProjectDialogBody(
    name: String,
    onNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    availableTags: List<TagChip>,
    selectedTagIds: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    onShowAddTag: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(text = stringResource(id = R.string.home_create_project_name_label)) },
            singleLine = true,
            modifier = Modifier.testTag(HomeTestTags.CREATE_PROJECT_NAME),
        )
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text(text = stringResource(id = R.string.home_create_project_description_label)) },
            singleLine = false,
            modifier = Modifier.testTag(HomeTestTags.CREATE_PROJECT_DESCRIPTION),
        )
        Text(
            text = stringResource(id = R.string.home_create_project_tags_label),
            style = MaterialTheme.typography.labelMedium,
        )
        if (availableTags.isEmpty()) {
            Text(
                text = stringResource(id = R.string.home_create_project_tags_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            CreateProjectTagsFlow(
                availableTags = availableTags,
                selectedTagIds = selectedTagIds,
            )
        }
        TextButton(
            onClick = onShowAddTag,
            modifier = Modifier.testTag(HomeTestTags.CREATE_PROJECT_ADD_TAG),
        ) {
            Text(text = stringResource(id = R.string.home_create_project_add_tag))
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CreateProjectTagsFlow(
    availableTags: List<TagChip>,
    selectedTagIds: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        availableTags.forEach { chip ->
            val isSelected = selectedTagIds.contains(chip.id)
            FilterChip(
                selected = isSelected,
                onClick = {
                    if (isSelected) selectedTagIds.remove(chip.id)
                    else selectedTagIds.add(chip.id)
                },
                label = { Text(text = chip.name) },
            )
        }
    }
}

@Composable
private fun AddTagDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, color: String) -> Unit,
) {
    var tagName by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf("#6750A4") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.home_create_project_add_tag_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text(text = stringResource(id = R.string.home_create_project_add_tag_name_label)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text(text = stringResource(id = R.string.home_create_project_add_tag_color_label)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmedName = tagName.trim()
                if (trimmedName.isEmpty()) return@TextButton
                onCreate(trimmedName, color.trim())
            }) {
                Text(text = stringResource(id = R.string.home_create_project_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.common_close))
            }
        },
    )
}

@Composable
private fun HomeEmptyState(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
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
                text = stringResource(id = R.string.home_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.home_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(onOpenSettings = {}, onOpenTasks = {})
}
