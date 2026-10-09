// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Orquestrador da Home (E1.7/E2.1/E2.6/E2.8): mantém o estado de
// sessão/conectividade e roteia intenções da UI para os use cases de
// domínio. O pipeline de listagem vive em HomeListingPipeline.kt; os
// mappers em HomeMappers.kt; os modelos de estado em HomeUiModels.kt;
// os bodies das ações CRUD/preferências em HomeActions.kt.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pucgo.joaopedrogmsilva.brainout.core.data.session.ActiveUserProvider
import pucgo.joaopedrogmsilva.brainout.core.data.sync.ConnectivityObserver
import pucgo.joaopedrogmsilva.brainout.core.data.sync.ConnectivityState
import pucgo.joaopedrogmsilva.brainout.core.data.sync.PendingSyncMonitor
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ListingPreferencesRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TagRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CreateProjectUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CreateTagUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.DeleteProjectUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.UpdateProjectUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * ViewModel da [HomeScreen].
 *
 * Responsabilidades:
 * - E1.7 — observar o usuário ativo e expor [HomeUserState].
 * - E2.1/E2.6/E2.8 — combinar os Flows em [uiState] (delegado a
 *   [homeUiStateFlow]) e capturar falhas de CRUD em [errorMessage].
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Suppress("LongParameterList", "TooManyFunctions")
class HomeViewModel
    @Inject
    constructor(
        private val projectRepository: ProjectRepository,
        private val tagRepository: TagRepository,
        private val activeUserProvider: ActiveUserProvider,
        private val listingPreferences: ListingPreferencesRepository,
        private val connectivityObserver: ConnectivityObserver,
        private val pendingSyncMonitor: PendingSyncMonitor,
        private val createProject: CreateProjectUseCase,
        private val updateProject: UpdateProjectUseCase,
        private val deleteProject: DeleteProjectUseCase,
        private val createTag: CreateTagUseCase,
    ) : ViewModel() {
        /** Estado de sessão — E1.7. */
        val userState: StateFlow<HomeUserState> =
            activeUserProvider
                .observeActiveUser()
                .map { it.toHomeUserState() }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                    initialValue = HomeUserState.Loading,
                )

        /** Estado de conectividade + fila pendente (E3.4). */
        val syncState: StateFlow<HomeSyncState> =
            combine(
                connectivityObserver.observe(),
                pendingSyncMonitor.observePendingCount(),
            ) { connectivity: ConnectivityState, pending: Int ->
                HomeSyncState(isOnline = connectivity.isOnline, pendingOps = pending)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = HomeSyncState(),
            )

        private val _upgradeDialogVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
        val upgradeDialogVisible: StateFlow<Boolean> = _upgradeDialogVisible.asStateFlow()

        fun onMemberFabClicked() {
            _upgradeDialogVisible.value = true
        }

        fun dismissUpgradeDialog() {
            _upgradeDialogVisible.value = false
        }

        /** Filtro estrutural ativo da lista de projetos (E2.5 — RN03). */
        private val _projectFilter: MutableStateFlow<HomeProjectFilter> =
            MutableStateFlow(HomeProjectFilter.Active)
        val projectFilter: StateFlow<HomeProjectFilter> = _projectFilter.asStateFlow()

        fun setProjectFilter(filter: HomeProjectFilter) {
            Log.d(TAG, "Alterando filtro de projetos: $filter")
            _projectFilter.value = filter
        }

        /** Token de retry (E2.8); cada [retry] reinicia o pipeline. */
        private val _retryToken: MutableStateFlow<Int> = MutableStateFlow(0)
        val retryToken: StateFlow<Int> = _retryToken.asStateFlow()

        /** Mensagem de erro pendente para a UI (E2.8). */
        private val _errorMessage: MutableStateFlow<String?> = MutableStateFlow(null)
        val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

        /**
         * Texto bruto da busca conforme o usuário digita (E2.6).
         *
         * O par público se chama `searchQuery` (e não `searchInput`)
         * porque é esse o termo usado pela UI e pelos testes; o
         * backing field precisa do prefixo `_` do padrão
         * StateFlow, e o ktlint casa o par pelo sufixo — daí a
         * única linha a mais, que é apenas nomenclatura.
         */
        private val _searchQuery: MutableStateFlow<String> = MutableStateFlow("")
        val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

        private val debouncedSearchInput: StateFlow<String> =
            debouncedSearchInput(
                scope = viewModelScope,
                rawInput = _searchQuery,
                debounceWindow = SEARCH_DEBOUNCE,
            )

        /** Estado da lista de projetos — E2.1 + E2.5 + E2.6 + E2.8. */
        val uiState: StateFlow<HomeUiState> =
            homeUiStateFlow(
                retryToken = _retryToken,
                activeUserProvider = activeUserProvider,
                listingPreferences = listingPreferences,
                tagRepository = tagRepository,
                projectRepository = projectRepository,
                searchInput = _searchQuery,
                projectFilter = _projectFilter,
                debouncedInput = debouncedSearchInput,
                errorMessage = _errorMessage,
            ).stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = HomeUiState(),
            )

        /** Cria projeto no nome do owner ativo. */
        fun createProject(
            name: String,
            description: String?,
            tagIds: List<String>,
        ) {
            Log.d(TAG, "createProject acionado: name=$name, tagsCount=${tagIds.size}")
            runCreateProject(
                viewModelScope,
                createProject,
                activeUserProvider,
                name,
                description,
                tagIds,
                _errorMessage,
            )
        }

        /** Atualiza projeto existente (campos + tags associadas). */
        fun updateProject(
            project: Project,
            tagIds: List<String>,
        ) {
            Log.d(TAG, "updateProject acionado: id=${project.id}, name=${project.name}")
            runUpdateProject(viewModelScope, updateProject, project, tagIds, _errorMessage)
        }

        /** Remove projeto (tarefas e `project_tags` em cascata). */
        fun deleteProject(projectId: String) {
            Log.d(TAG, "deleteProject acionado: id=$projectId")
            runDeleteProject(viewModelScope, deleteProject, projectId, _errorMessage)
        }

        /** Cria tag para o owner ativo. */
        fun createTag(
            name: String,
            color: String,
        ) {
            Log.d(TAG, "createTag acionado: name=$name, color=$color")
            runCreateTag(viewModelScope, createTag, activeUserProvider, name, color, _errorMessage)
        }

        /** Atualiza a query de busca e persiste no DataStore (E2.6). */
        fun onSearchQueryChange(newQuery: String) {
            _searchQuery.value = newQuery
            runOnSearchQueryChange(viewModelScope, listingPreferences, activeUserProvider, newQuery)
        }

        /** Atualiza a tag selecionada como filtro (E2.6). */
        fun onTagFilterChange(tagId: String?) {
            Log.d(TAG, "onTagFilterChange: tagId=$tagId")
            runOnTagFilterChange(viewModelScope, listingPreferences, activeUserProvider, tagId)
        }

        /** Atualiza a ordenação (E2.6). */
        fun onSortOrderChange(order: SortOrder) {
            Log.d(TAG, "onSortOrderChange: order=$order")
            runOnSortOrderChange(viewModelScope, listingPreferences, activeUserProvider, order)
        }

        /** Re-assina o pipeline após falha (E2.8). */
        fun retry() {
            Log.d(TAG, "retry acionado na Home")
            _errorMessage.value = null
            _retryToken.value = _retryToken.value + 1
        }

        /** Limpa a mensagem de erro atual (E2.8). */
        fun clearError() {
            _errorMessage.value = null
        }

        companion object {
            private const val TAG = "BrainOut:HomeVM"

            // E2.8 — chaves canônicas que a UI resolve em `R.string.home_error_*`.
            const val ERROR_LOAD_FAILED: String = "Não foi possível carregar seus projetos"
            const val ERROR_ACTION_FAILED: String = "Não foi possível concluir a operação"

            /** Indica se a mensagem é a de falha de carga. */
            fun isLoadErrorMessage(message: String): Boolean = message == ERROR_LOAD_FAILED

            /** Janela de debounce da busca textual (E2.6). */
            val SEARCH_DEBOUNCE: kotlin.time.Duration = 300.milliseconds
        }
    }
