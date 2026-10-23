// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Orquestrador da Home (E1.7/E2.1/E2.6/E2.8): mantém o estado de
// sessão local e roteia intenções da UI para os use cases de
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
 *   [homeUiStateFlow]) e capturar falhas de CRUD no canal de erro,
 *   que chega à UI por [HomeUiState.errorMessage].
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Suppress("LongParameterList")
class HomeViewModel
    @Inject
    constructor(
        private val projectRepository: ProjectRepository,
        private val tagRepository: TagRepository,
        private val activeUserProvider: ActiveUserProvider,
        private val listingPreferences: ListingPreferencesRepository,
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
        private val retryToken: MutableStateFlow<Int> = MutableStateFlow(0)

        /**
         * Canal único de erro da Home (E2.8).
         *
         * Toda falha — de leitura ou de ação — é gravada aqui e chega
         * à UI por [HomeUiState.errorMessage], que é a **única**
         * superfície de leitura do erro.
         *
         * Antes o erro de carga era duplicado: o pipeline o escrevia
         * no canal *e* dentro do [HomeUiState], e a tela resolvia
         * `actionError ?: listState.errorMessage`. Como o estado é uma
         * fotografia imutável, o "Dispensar" (que só limpava o canal)
         * não apagava a cópia — o banner ficava na tela para sempre
         * (DEF-47).
         */
        private val errorSink: MutableStateFlow<HomeErrorType?> = MutableStateFlow(null)

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

        /**
         * Estado da lista de projetos — E2.1 + E2.5 + E2.6 + E2.8.
         *
         * O erro não vem pronto do pipeline: o canal [errorSink] é
         * combinado com o estado aqui, de modo que a UI leia um único
         * campo ([HomeUiState.errorMessage]) e o "Dispensar" apague o
         * banner de fato (DEF-47).
         */
        val uiState: StateFlow<HomeUiState> =
            combine(
                homeUiStateFlow(
                    dependencies =
                        ListingDependencies(
                            activeUserProvider = activeUserProvider,
                            listingPreferences = listingPreferences,
                            projectRepository = projectRepository,
                            tagRepository = tagRepository,
                        ),
                    signals =
                        ListingSignals(
                            retryToken = retryToken,
                            searchInput = _searchQuery,
                            projectFilter = _projectFilter,
                            debouncedInput = debouncedSearchInput,
                            errorSink = errorSink,
                        ),
                ),
                errorSink,
            ) { state: HomeUiState, error: HomeErrorType? ->
                state.copy(errorMessage = error)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = HomeUiState(),
            )

        /** Cria projeto no nome do owner ativo. */
        fun createProject(
            name: String,
            description: String?,
            tagIds: List<String>,
            onSuccess: () -> Unit = {},
        ) {
            Log.d(TAG, "createProject acionado: name=$name, tagsCount=${tagIds.size}")
            runCreateProject(
                viewModelScope,
                createProject,
                activeUserProvider,
                name,
                description,
                tagIds,
                errorSink,
                onSuccess,
            )
        }

        /** Atualiza projeto existente (campos + tags associadas). */
        fun updateProject(
            project: Project,
            tagIds: List<String>,
        ) {
            Log.d(TAG, "updateProject acionado: id=${project.id}, name=${project.name}")
            runUpdateProject(viewModelScope, updateProject, project, tagIds, errorSink)
        }

        /** Remove projeto (tarefas e `project_tags` em cascata). */
        fun deleteProject(projectId: String) {
            Log.d(TAG, "deleteProject acionado: id=$projectId")
            runDeleteProject(viewModelScope, deleteProject, projectId, errorSink)
        }

        /** Cria tag para o owner ativo. */
        fun createTag(
            name: String,
            color: String,
        ) {
            Log.d(TAG, "createTag acionado: name=$name, color=$color")
            runCreateTag(viewModelScope, createTag, activeUserProvider, name, color, errorSink)
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
            errorSink.value = null
            retryToken.value = retryToken.value + 1
        }

        /** Limpa a mensagem de erro atual (E2.8). */
        fun clearError() {
            errorSink.value = null
        }

        companion object {
            private const val TAG = "BrainOut:HomeVM"

            /** Janela de debounce da busca textual (E2.6). */
            val SEARCH_DEBOUNCE: kotlin.time.Duration = 300.milliseconds
        }
    }
