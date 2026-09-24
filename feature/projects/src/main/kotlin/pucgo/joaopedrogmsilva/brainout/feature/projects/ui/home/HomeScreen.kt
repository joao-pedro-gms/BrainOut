// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Home orchestrator (E1.7/E2.1/E2.6): Scaffold + callback routing.
// Seções vivem nos irmãos Home*.kt; aqui ficam `HomeScreen`,
// `HomeTestTags` e o preview.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder

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
    // deleteProject/createTag) grava em `viewModel.errorMessage`,
    // não em `uiState.errorMessage`. Sem coletar este flow, falhas
    // de escrita ficam invisíveis na UI.
    val actionError by viewModel.errorMessage.collectAsStateWithLifecycle()

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
                errorMessage = actionError ?: listState.errorMessage,
                onRetry = viewModel::retry,
                onDismissError = viewModel::clearError,
                availableTags = listState.availableTags,
                searchQuery = viewModel.searchQuery.collectAsStateWithLifecycle().value,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                selectedTagId = listState.selectedTagId,
                onTagFilterChange = viewModel::onTagFilterChange,
                sortOrder = listState.sortOrder,
                onSortOrderChange = viewModel::onSortOrderChange,
                syncState = viewModel.syncState.collectAsStateWithLifecycle().value,
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

    // E3.4 — banner offline + contagem da fila de sincronização.
    const val OFFLINE_BANNER: String = "home_offline_banner"

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

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(onOpenSettings = {}, onOpenTasks = {})
}
