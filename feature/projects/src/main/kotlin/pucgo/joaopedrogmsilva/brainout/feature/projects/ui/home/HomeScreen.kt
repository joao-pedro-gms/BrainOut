// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Home orchestrator (E1.7/E2.1/E2.6): Scaffold + callback routing.
// Seções vivem nos irmãos Home*.kt; aqui ficam `HomeScreen`,
// `HomeTestTags` e o preview.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSpacing

/** Lista de projetos; navegação global pertence ao shell do app. */
@Composable
fun HomeScreen(
    onOpenProject: (projectId: String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val userState by viewModel.userState.collectAsStateWithLifecycle()
    val listState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentFilter by viewModel.projectFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var editingProjectId by rememberSaveable { mutableStateOf<String?>(null) }
    val editingItem = listState.projects.firstOrNull { it.project.id == editingProjectId }

    // DEF-72 — sem sessão (carregando ou sem login) a Home não
    // inventa uma identidade: a TopAppBar mostra a saudação genérica.
    val homeUser: HomeUser? =
        when (val current = userState) {
            HomeUserState.Loading, HomeUserState.SignedOut -> null
            is HomeUserState.SignedIn ->
                HomeUser(
                    displayName = current.displayName,
                    initials = current.initials,
                )
        }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            HomeTopBar(user = homeUser)
        },
        floatingActionButton = {
            HomeFloatingActionButton(
                onCreateProjectClicked = { showCreateDialog = true },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        HomeProjectsContent(
            contentPadding = innerPadding,
            state =
                HomeProjectsState(
                    projects = listState.projects,
                    isLoading = listState.isLoading,
                    currentFilter = currentFilter,
                    errorMessage = listState.errorMessage,
                    availableTags = listState.availableTags,
                    searchQuery = searchQuery,
                    selectedTagId = listState.selectedTagId,
                    sortOrder = listState.sortOrder,
                ),
            actions =
                HomeProjectsActions(
                    onOpenProject = onOpenProject,
                    onSelectFilter = viewModel::setProjectFilter,
                    onRetry = viewModel::retry,
                    onDismissError = viewModel::clearError,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onTagFilterChange = viewModel::onTagFilterChange,
                    onSortOrderChange = viewModel::onSortOrderChange,
                    onEditProject = { editingProjectId = it },
                ),
        )
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            errorMessage = listState.errorMessage?.let { resolveHomeErrorMessage(it) },
            availableTags = listState.availableTags,
            onDismiss = { showCreateDialog = false },
            onCreateTag = { name, color -> viewModel.createTag(name, color) },
            onCreateProject = { name, description, tagIds ->
                viewModel.createProject(name, description, tagIds) { showCreateDialog = false }
            },
        )
    }

    editingItem?.let { item ->
        EditProjectDialog(
            item = item,
            errorMessage = listState.errorMessage?.let { resolveHomeErrorMessage(it) },
            availableTags = listState.availableTags,
            onDismiss = { editingProjectId = null },
            onCreateTag = { name, color -> viewModel.createTag(name, color) },
            onUpdateProject = { project, tagIds ->
                viewModel.updateProject(project, tagIds) { editingProjectId = null }
            },
        )
    }
}

/** Identificadores usados por testes Compose. */
object HomeTestTags {
    const val FAB: String = "home_fab"
    const val ROLE_BADGE: String = "home_role_badge"
    const val UPGRADE_DIALOG: String = "home_upgrade_dialog"
    const val UPGRADE_DIALOG_ACK: String = "home_upgrade_dialog_ack"
    const val CREATE_PROJECT_FORM_PREFIX: String = "home_create_project"
    const val CREATE_PROJECT_DIALOG: String = "${CREATE_PROJECT_FORM_PREFIX}_dialog"
    const val CREATE_PROJECT_NAME: String = "${CREATE_PROJECT_FORM_PREFIX}_name"
    const val CREATE_PROJECT_DESCRIPTION: String = "${CREATE_PROJECT_FORM_PREFIX}_description"
    const val CREATE_PROJECT_CONFIRM: String = "${CREATE_PROJECT_FORM_PREFIX}_confirm"
    const val CREATE_PROJECT_ADD_TAG: String = "${CREATE_PROJECT_FORM_PREFIX}_add_tag"
    const val CREATE_PROJECT_ERROR: String = "${CREATE_PROJECT_FORM_PREFIX}_error"
    const val EDIT_PROJECT_BUTTON: String = "home_edit_project_button"
    const val EDIT_PROJECT_FORM_PREFIX: String = "home_edit_project"
    const val EDIT_PROJECT_DIALOG: String = "${EDIT_PROJECT_FORM_PREFIX}_dialog"
    const val EDIT_PROJECT_NAME: String = "${EDIT_PROJECT_FORM_PREFIX}_name"
    const val EDIT_PROJECT_DESCRIPTION: String = "${EDIT_PROJECT_FORM_PREFIX}_description"
    const val EDIT_PROJECT_CONFIRM: String = "${EDIT_PROJECT_FORM_PREFIX}_confirm"
    const val EDIT_PROJECT_ADD_TAG: String = "${EDIT_PROJECT_FORM_PREFIX}_add_tag"
    const val EDIT_PROJECT_ERROR: String = "${EDIT_PROJECT_FORM_PREFIX}_error"
    const val EDIT_PROJECT_TAG: String = "${EDIT_PROJECT_FORM_PREFIX}_tag"

    /**
     * Chip de tag selecionável no diálogo de criação de projeto.
     *
     * O sufixo é o id da tag (`home_create_project_tag_<id>`), porque
     * há um chip por tag disponível.
     */
    const val CREATE_PROJECT_TAG: String = "home_create_project_tag"
    const val PROJECT_CARD: String = "home_project_card"
    const val FILTER_GROUP: String = "home_filter_group"
    const val FILTER_ACTIVE: String = "home_filter_active"
    const val FILTER_COMPLETED: String = "home_filter_completed"
    const val LOADING: String = "home_loading"

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
 * Preview da lista de projetos da Home.
 *
 * Aponta para [HomeProjectsContent] (e não para [HomeScreen]) de
 * propósito: a tela resolve o ViewModel por `hiltViewModel()`, que
 * exige uma Activity — no IDE a preview simplesmente não renderizava.
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    BrainOutTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeProjectsContent(
                contentPadding = PaddingValues(NeoSpacing.none),
                state =
                    HomeProjectsState(
                        projects = emptyList(),
                        isLoading = false,
                        currentFilter = HomeProjectFilter.Active,
                        errorMessage = null,
                        availableTags = emptyList(),
                        searchQuery = "",
                        selectedTagId = null,
                        sortOrder = SortOrder.CreatedDesc,
                    ),
                actions =
                    HomeProjectsActions(
                        onOpenProject = {},
                        onSelectFilter = {},
                        onRetry = {},
                        onDismissError = {},
                        onSearchQueryChange = {},
                        onTagFilterChange = {},
                        onSortOrderChange = {},
                    ),
            )
        }
    }
}
