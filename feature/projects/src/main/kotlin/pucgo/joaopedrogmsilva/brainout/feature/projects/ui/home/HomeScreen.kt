// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Tela Home do BrainOut — agora com usuário real (E1.7), badge de
// papel colorido por role, FAB gated pelo papel (Owner habilita,
// Member abre diálogo explicativo) e lista real de projetos com
// chips de tag (E2.1/E2.6).
//
// Telas Compose legítimas concentram muitos composables pequenos
// em um único arquivo; suprimimos TooManyFunctions/LongParameterList
// para manter a coesão da feature em vez de dispersar widgets
// correlatos (mesma decisão do ProjectDetailScreen).

@file:Suppress("TooManyFunctions", "LongParameterList", "LongMethod")

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder
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
    onOpenDashboard: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val userState by viewModel.userState.collectAsStateWithLifecycle()
    val upgradeDialogVisible by viewModel.upgradeDialogVisible.collectAsStateWithLifecycle()
    val listState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentFilter by viewModel.projectFilter.collectAsStateWithLifecycle()

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
                        HomeTab.Dashboard -> onOpenDashboard()
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
                currentFilter = currentFilter,
                onSelectFilter = viewModel::setProjectFilter,
                errorMessage = listState.errorMessage,
                onRetry = viewModel::retry,
                onDismissError = viewModel::clearError,
                availableTags = listState.availableTags,
                searchQuery = viewModel.searchQuery.collectAsStateWithLifecycle().value,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                selectedTagId = listState.selectedTagId,
                onTagFilterChange = viewModel::onTagFilterChange,
                sortOrder = listState.sortOrder,
                onSortOrderChange = viewModel::onSortOrderChange,
            )
            HomeTab.Tasks -> Unit
            HomeTab.Dashboard -> Unit
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
    const val FILTER_GROUP: String = "home_filter_group"
    const val FILTER_ACTIVE: String = "home_filter_active"
    const val FILTER_COMPLETED: String = "home_filter_completed"
    const val LOADING: String = "home_loading"
    const val ERROR_BANNER: String = "home_error_banner"
    const val ERROR_RETRY: String = "home_error_retry"
    const val ERROR_DISMISS: String = "home_error_dismiss"

    // E2.6 — busca + filtro por tag + ordenação.
    const val SEARCH_FIELD: String = "home_search_field"
    const val SEARCH_CLEAR: String = "home_search_clear"
    const val TAG_FILTER_ROW: String = "home_tag_filter_row"
    const val TAG_FILTER_ALL: String = "home_tag_filter_all"
    const val SORT_MENU_BUTTON: String = "home_sort_menu_button"
    const val SORT_MENU: String = "home_sort_menu"
    const val NO_MATCHES: String = "home_no_matches"

    /** Test tag para um chip de filtro por tag específico. */
    fun tagFilterChip(tagId: String): String = "home_tag_filter_chip_$tagId"

    /** Test tag para um item do menu de ordenação. */
    fun sortMenuItem(order: SortOrder): String = "home_sort_item_${order.name.lowercase()}"
}

/**
 * Chave estável do chip "Todas" no [HomeTagFilterRow]. Mantida como
 * constante no escopo do arquivo (em vez de string literal inline)
 * para que o `LazyRow` possa referenciar a mesma `key` na hora do
 * recompose e não destrua/recrie o chip quando o `availableTags`
 * muda.
 */
private const val TAG_FILTER_ALL_KEY: String = "__all__"

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
                        // E4.4: garante área de toque mínima de 48dp
                        // (WCAG 2.5.5 Target Size). M3 AssistChip é ~32dp
                        // de altura por padrão.
                        modifier = Modifier.heightIn(min = 48.dp),
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
    HomeTab.Dashboard -> Icons.Outlined.BarChart
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
    currentFilter: HomeProjectFilter,
    onSelectFilter: (HomeProjectFilter) -> Unit,
    errorMessage: String?,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    availableTags: List<TagChip>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedTagId: String?,
    onTagFilterChange: (String?) -> Unit,
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // E2.6 — barra de busca textual. `OutlinedTextField` com ícone
        // de limpar (X) à direita quando o texto não está vazio. O
        // placeholder vem de `values/strings.xml` para manter a regra
        // E4.6 (pt/en).
        HomeSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
        )
        // E2.6 — chips horizontais com todas as tags do usuário +
        // "Todas" para o filtro por tag. Renderizado como `LazyRow`
        // para escalar quando o usuário tem muitas tags.
        HomeTagFilterRow(
            availableTags = availableTags,
            selectedTagId = selectedTagId,
            onSelect = onTagFilterChange,
        )
        Text(
            text = when (currentFilter) {
                HomeProjectFilter.Active -> stringResource(id = R.string.home_section_title)
                HomeProjectFilter.Completed ->
                    stringResource(id = R.string.home_section_completed_title)
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        // Toggle de filtros Ativos / Concluídos (RN03 — E2.5).
        // Fica sempre visível para que o usuário possa alternar mesmo
        // quando uma das listas estiver vazia.
        HomeProjectFilterRow(
            current = currentFilter,
            onSelect = onSelectFilter,
            sortOrder = sortOrder,
            onSortOrderChange = onSortOrderChange,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        // E2.8 — banner de erro com retry. Tem prioridade sobre o
        // estado vazio: se o Room falhou, oferecemos "Tentar
        // novamente" em vez do empty state (que mostraria uma
        // coleção vazia de forma enganosa).
        if (errorMessage != null) {
            HomeErrorBanner(
                message = errorMessage,
                onRetry = onRetry,
                onDismiss = onDismissError,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
            )
        } else if (projects.isEmpty() && isLoading) {
            // E2.8 — loader enquanto o Flow do Room não emite a
            // primeira lista. Empty state só aparece após a primeira
            // coleta efetiva, evitando a sobreposição das duas
            // visualizações.
            HomeLoadingState(modifier = Modifier.fillMaxWidth())
        } else if (projects.isEmpty()) {
            // E2.6 — diferenciar "lista vazia sem filtro" de
            // "filtro/busca não retornou nada". No segundo caso, a
            // mensagem cita a query (ou a tag) que está restringindo
            // a visualização.
            if (searchQuery.isNotBlank() || selectedTagId != null) {
                HomeNoMatchesState(
                    query = searchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                )
            } else if (currentFilter == HomeProjectFilter.Completed) {
                HomeCompletedEmptyState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                )
            } else {
                HomeEmptyState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                )
            }
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

/**
 * Loader da Home (E2.8). Centralizado e com `testTag` para os
 * testes Compose. Mantemos a área de toque ≥ 48dp no `Box` para
 * consistência com a diretriz E4.4.
 */
@Composable
private fun HomeLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 240.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.testTag(HomeTestTags.LOADING),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Text(
                text = stringResource(id = R.string.home_loading_aria_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Banner de erro com ações de retry/dispensar (E2.8). Renderizado
 * sempre que `errorMessage != null`. A mensagem vinda do ViewModel
 * é uma chave canônica (`ERROR_LOAD_FAILED` ou
 * `ERROR_ACTION_FAILED`); a UI resolve para o recurso localizado
 * via [resolveHomeErrorMessage].
 */
@Composable
private fun HomeErrorBanner(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.testTag(HomeTestTags.ERROR_BANNER),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = resolveHomeErrorMessage(message),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .testTag(HomeTestTags.ERROR_RETRY)
                        // E4.4: 48dp mínimo WCAG 2.5.5.
                        .heightIn(min = 48.dp),
                ) {
                    Text(text = stringResource(id = R.string.home_error_retry))
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .testTag(HomeTestTags.ERROR_DISMISS)
                        .heightIn(min = 48.dp),
                ) {
                    Text(text = stringResource(id = R.string.home_error_dismiss))
                }
            }
        }
    }
}

/**
 * Resolve a chave canônica de erro do [HomeViewModel] para a string
 * localizada. Mesma estratégia de `resolveProjectDetailMessage` em
 * E2.1 — chaves técnicas do ViewModel viram recurso pt/en sem
 * expor stack traces para o usuário.
 */
@Composable
internal fun resolveHomeErrorMessage(message: String): String = when {
    HomeViewModel.isLoadErrorMessage(message) ->
        stringResource(id = R.string.home_error_load_failed)
    message == HomeViewModel.ERROR_ACTION_FAILED ->
        stringResource(id = R.string.home_error_action_failed)
    else -> message
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeProjectFilterRow(
    current: HomeProjectFilter,
    onSelect: (HomeProjectFilter) -> Unit,
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.FILTER_GROUP),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeProjectFilter.entries.forEach { option ->
            FilterChip(
                selected = option == current,
                onClick = { onSelect(option) },
                label = {
                    Text(
                        text = stringResource(id = option.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                modifier = Modifier
                    .testTag(
                        when (option) {
                            HomeProjectFilter.Active -> HomeTestTags.FILTER_ACTIVE
                            HomeProjectFilter.Completed -> HomeTestTags.FILTER_COMPLETED
                        },
                    )
                    // E4.4: 48dp mínimo para área de toque (WCAG 2.5.5).
                    .heightIn(min = 48.dp),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        // Rótulo de acessibilidade oculto para o agrupamento (TalkBack).
        Text(
            text = stringResource(id = R.string.home_filter_aria_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // E2.6 — menu de ordenação (Nome A→Z / Z→A / Mais recentes /
        // Mais antigas). Dispara um `DropdownMenu` ancorado no
        // `IconButton` de sort. O valor ativo fica marcado com
        // leading checkmark via `leadingIcon`.
        HomeSortMenu(
            current = sortOrder,
            onSelect = onSortOrderChange,
        )
    }
}

/**
 * Menu dropdown de ordenação (E2.6). Acionado pelo ícone de sort
 * (`Icons.Outlined.Sort`) à direita da linha de filtros estruturais.
 * O item ativo recebe um leading check via `leadingIcon`; sem isso,
 * o usuário teria que adivinhar qual opção está selecionada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeSortMenu(
    current: SortOrder,
    onSelect: (SortOrder) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .testTag(HomeTestTags.SORT_MENU_BUTTON)
                // E4.4: 48dp mínimo WCAG 2.5.5.
                .heightIn(min = 48.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Sort,
                contentDescription = stringResource(id = R.string.home_sort_aria_label),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.testTag(HomeTestTags.SORT_MENU),
        ) {
            SortOrder.entries.forEach { order ->
                val isSelected = order == current
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = sortOrderLabelRes(order))) },
                    onClick = {
                        onSelect(order)
                        expanded = false
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                            )
                        }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .testTag(HomeTestTags.sortMenuItem(order))
                        .heightIn(min = 48.dp),
                )
            }
        }
    }
}

/**
 * Empty state quando há busca/filtro ativo mas a query não
 * retornou nada (E2.6). Diferencia do
 * [HomeCompletedEmptyState] / [HomeEmptyState] ao citar o termo
 * buscado para que o usuário saiba que o filtro é a razão da
 * lista vazia (não a ausência de projetos).
 */
@Composable
private fun HomeNoMatchesState(
    query: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.testTag(HomeTestTags.NO_MATCHES),
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
                text = if (query.isNotBlank()) {
                    stringResource(id = R.string.home_no_matches_title_with_query, query)
                } else {
                    stringResource(id = R.string.home_no_matches_title)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.home_no_matches_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Barra de busca textual (E2.6). `OutlinedTextField` com placeholder
 * localizado; ícone de limpar (X) surge apenas quando o campo tem
 * texto para reduzir ruído visual. O debounce de I/O é aplicado no
 * ViewModel — esta composable dispara o callback em cada keystroke
 * para feedback visual imediato do campo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(text = stringResource(id = R.string.home_search_placeholder)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Sort,
                contentDescription = null,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    // E4.4: 48dp mínimo para área de toque (WCAG 2.5.5).
                    modifier = Modifier
                        .testTag(HomeTestTags.SEARCH_CLEAR)
                        .heightIn(min = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = stringResource(id = R.string.home_search_clear),
                    )
                }
            }
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.SEARCH_FIELD),
    )
}

/**
 * Linha de chips para filtro por tag (E2.6). Primeiro chip é
 * "Todas" (`null` no filtro) e em seguida cada tag do owner.
 * `LazyRow` para escalar quando o usuário tem dezenas de tags.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTagFilterRow(
    availableTags: List<TagChip>,
    selectedTagId: String?,
    onSelect: (String?) -> Unit,
) {
    if (availableTags.isEmpty()) return
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.TAG_FILTER_ROW),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        item(key = TAG_FILTER_ALL_KEY) {
            FilterChip(
                selected = selectedTagId == null,
                onClick = { onSelect(null) },
                label = {
                    Text(text = stringResource(id = R.string.home_tag_filter_all))
                },
                modifier = Modifier
                    .testTag(HomeTestTags.TAG_FILTER_ALL)
                    // E4.4: 48dp mínimo WCAG 2.5.5.
                    .heightIn(min = 48.dp),
            )
        }
        items(items = availableTags, key = { it.id }) { chip ->
            FilterChip(
                selected = chip.id == selectedTagId,
                onClick = {
                    onSelect(if (chip.id == selectedTagId) null else chip.id)
                },
                label = { Text(text = chip.name) },
                modifier = Modifier
                    .testTag(HomeTestTags.tagFilterChip(chip.id))
                    // E4.4: 48dp mínimo WCAG 2.5.5.
                    .heightIn(min = 48.dp),
            )
        }
    }
}

@Composable
private fun HomeCompletedEmptyState(modifier: Modifier = Modifier) {
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
                text = stringResource(id = R.string.home_completed_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.home_completed_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
        // E4.4: 48dp mínimo para área de toque (WCAG 2.5.5).
        modifier = Modifier.heightIn(min = 48.dp),
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
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
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
                onNameChange = {
                    name = it
                    // E2.8 — limpa o erro assim que o usuário
                    // começa a digitar, evitando mensagem fixa no
                    // campo enquanto a interação continua.
                    if (nameError != null) nameError = null
                },
                nameError = nameError,
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
                    if (trimmedName.isEmpty()) {
                        // E2.8 — validação inline: não fecha o
                        // diálogo, marca `nameError` para o
                        // `TextField` exibir `isError` +
                        // `supportingText`.
                        nameError = CREATE_PROJECT_NAME_REQUIRED_KEY
                        return@TextButton
                    }
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

/**
 * Chave canônica de validação inline (E2.8). Mesma estratégia do
 * `ERROR_LOAD_FAILED` — a UI resolve para o recurso localizado via
 * [resolveCreateProjectNameError]. Permanece como constante para
 * que os testes Compose possam comparar com a chave sem depender de
 * texto em pt/en.
 */
private const val CREATE_PROJECT_NAME_REQUIRED_KEY: String = "CREATE_PROJECT_NAME_REQUIRED"

@Composable
internal fun resolveCreateProjectNameError(key: String?): String? = when (key) {
    CREATE_PROJECT_NAME_REQUIRED_KEY ->
        stringResource(id = R.string.home_create_project_name_required)
    null -> null
    else -> key
}

@Composable
private fun CreateProjectDialogBody(
    name: String,
    onNameChange: (String) -> Unit,
    nameError: String?,
    description: String,
    onDescriptionChange: (String) -> Unit,
    availableTags: List<TagChip>,
    selectedTagIds: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    onShowAddTag: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // E2.8 — `isError` + `supportingText` quando `nameError`
        // estiver preenchido. A mensagem é resolvida via
        // [resolveCreateProjectNameError] para manter a string
        // localizada em pt/en (regra E4.6).
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(text = stringResource(id = R.string.home_create_project_name_label)) },
            singleLine = true,
            isError = nameError != null,
            supportingText = {
                val resolved = resolveCreateProjectNameError(nameError)
                if (resolved != null) {
                    Text(
                        text = resolved,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
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
                // E4.4: FilterChip padrão M3 tem ~32dp de altura.
                // Aplicamos 48dp para satisfazer WCAG 2.5.5.
                modifier = Modifier.heightIn(min = 48.dp),
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

/**
 * Resolve o rótulo localizado de uma [SortOrder] para a UI
 * (E2.6 do ROADMAP).
 *
 * Mantida fora do enum em `:core:domain` porque o enum não tem
 * acesso ao `R.string` da feature. A correspondência é exaustiva
 * (`when` sem `else`) — adições/remoções em [SortOrder] serão
 * sinalizadas em tempo de compilação aqui.
 */
@androidx.annotation.StringRes
private fun sortOrderLabelRes(order: SortOrder): Int = when (order) {
    SortOrder.NameAsc -> R.string.home_sort_name_asc
    SortOrder.NameDesc -> R.string.home_sort_name_desc
    SortOrder.CreatedDesc -> R.string.home_sort_created_desc
    SortOrder.CreatedAsc -> R.string.home_sort_created_asc
}
