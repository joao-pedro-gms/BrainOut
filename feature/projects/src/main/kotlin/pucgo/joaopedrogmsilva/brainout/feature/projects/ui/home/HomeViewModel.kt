// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pucgo.joaopedrogmsilva.brainout.core.data.session.ActiveUserProvider
import pucgo.joaopedrogmsilva.brainout.core.data.sync.ConnectivityObserver
import pucgo.joaopedrogmsilva.brainout.core.data.sync.ConnectivityState
import pucgo.joaopedrogmsilva.brainout.core.data.sync.PendingSyncMonitor
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ListingPreferences
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ListingPreferencesRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TagRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CreateProjectUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CreateTagUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.DeleteProjectUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.UpdateProjectUseCase

/**
 * Estado da sessão ativa usado pela [HomeScreen] para saudação,
 * monograma e badge de papel.
 */
sealed interface HomeUserState {
    data object Loading : HomeUserState
    data object SignedOut : HomeUserState
    data class SignedIn(
        val displayName: String,
        val initials: String,
        val role: HomeUserRole,
    ) : HomeUserState
}

/**
 * Estado de conectividade/fila de sincronização consumido pela
 * [HomeScreen] para o banner offline e o indicador da fila (E3.4).
 */
data class HomeSyncState(
    val isOnline: Boolean = true,
    val pendingOps: Int = 0,
) {
    /** Indica se o banner offline deve estar visível. */
    val showOfflineBanner: Boolean get() = !isOnline
}

/**
 * Estado da lista de projetos (E2.1/E2.5/E2.6/E2.8) consumido pela
 * [HomeScreen].
 */
data class HomeUiState(
    val projects: List<ProjectCardItem> = emptyList(),
    val availableTags: List<TagChip> = emptyList(),
    val searchQuery: String = "",
    val selectedTagId: String? = null,
    val sortOrder: SortOrder = SortOrder.CreatedDesc,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val projectFilter: HomeProjectFilter = HomeProjectFilter.Active,
)

/** Item de card: projeto + chips de tag a renderizar. */
data class ProjectCardItem(
    val project: Project,
    val tags: List<TagChip>,
)

/** Representação visual de uma tag na lista de chips. */
data class TagChip(val id: String, val name: String, val color: String)

/**
 * ViewModel da [HomeScreen].
 *
 * Responsabilidades:
 * - E1.7 — observar o [User] ativo via [ActiveUserProvider] e expor
 *   [HomeUserState] (saudação, monograma, papel) em [userState].
 * - E2.1 — observar projetos + tags do owner em [uiState],
 *   combinando os Flows de [ProjectRepository] e [TagRepository].
 * - E2.6 — busca textual por nome + filtro por tag + ordenação
 *   (`name_asc`/`name_desc`/`created_desc`/`created_asc`),
 *   persistidos por usuário em [ListingPreferencesRepository].
 *   A busca aplica `debounce(300ms)` antes de re-executar a query
 *   no Room para evitar I/O a cada tecla.
 * - E2.6 — disparar `createProject` / `deleteProject` / `createTag`
 *   através dos use cases de domínio correspondentes.
 * - E2.8 — capturar falhas dos Flows do Room e dos use cases de
 *   CRUD; expor [HomeUiState.errorMessage] e oferecer [retry] /
 *   [clearError] para que a UI apresente banner de erro com botão
 *   "Tentar novamente" sem travar a tela.
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Suppress(
    "LongParameterList",
    "TooManyFunctions",
)
class HomeViewModel @Inject constructor(
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
    val userState: StateFlow<HomeUserState> = activeUserProvider.observeActiveUser()
        .let { userFlow ->
            kotlinx.coroutines.flow.flow {
                userFlow.collect { user -> emit(user.toHomeUserState()) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = HomeUserState.Loading,
        )

    /**
     * Estado de conectividade + fila pendente (E3.4). Combina o
     * callback de rede ([ConnectivityObserver]) com a contagem da
     * tabela `pending_ops` ([PendingSyncMonitor]) em um único
     * [StateFlow] — a UI mostra o banner offline e o indicador
     * "X alterações aguardando sincronização" a partir dele.
     */
    val syncState: StateFlow<HomeSyncState> = combine(
        connectivityObserver.observe(),
        pendingSyncMonitor.observePendingCount(),
    ) { connectivity: ConnectivityState, pending: Int ->
        HomeSyncState(isOnline = connectivity.isOnline, pendingOps = pending)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = HomeSyncState(),
        )

    /**
     * Flag do diálogo de upgrade para o papel [HomeUserRole.Member]
     * (E1.7). Mantido como `MutableStateFlow` separado para que a UI
     * possa mostrá-lo/ocultá-lo sem afetar o restante do estado.
     */
    private val _upgradeDialogVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val upgradeDialogVisible: StateFlow<Boolean> = _upgradeDialogVisible.asStateFlow()

    /** Sinaliza que o Member tocou no FAB desabilitado. */
    fun onMemberFabClicked() {
        _upgradeDialogVisible.value = true
    }

    /** Fecha o diálogo de upgrade após o usuário tocar em "Entendi". */
    fun dismissUpgradeDialog() {
        _upgradeDialogVisible.value = false
    }

    /**
     * Filtro estrutural ativo da lista de projetos (E2.5 — RN03).
     * Mantido em [MutableStateFlow] para que a UI possa alternar sem
     * precisar pedir ao ViewModel para recarregar.
     */
    private val _projectFilter: MutableStateFlow<HomeProjectFilter> =
        MutableStateFlow(HomeProjectFilter.Active)
    val projectFilter: StateFlow<HomeProjectFilter> = _projectFilter.asStateFlow()

    /** Define o filtro estrutural ativo. Chamado pela UI nos toggles chips. */
    fun setProjectFilter(filter: HomeProjectFilter) {
        _projectFilter.value = filter
    }

    /**
     * Token de retry (E2.8). Cada chamada a [retry] incrementa este
     * valor; o pipeline [uiState] depende dele como chave de
     * `flatMapLatest`, então a mudança descarta o Flow anterior
     * (liberando o `WhileSubscribed` interno) e inicia uma nova
     * inscrição nos repositórios.
     */
    private val _retryToken: MutableStateFlow<Int> = MutableStateFlow(0)
    val retryToken: StateFlow<Int> = _retryToken.asStateFlow()

    /**
     * Mensagem de erro pendente para a UI (E2.8).
     */
    private val _errorMessage: MutableStateFlow<String?> = MutableStateFlow(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Texto bruto da busca conforme o usuário digita (E2.6).
     */
    private val _searchInput: MutableStateFlow<String> = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchInput.asStateFlow()

    /**
     * Versão debounced do [_searchInput] que efetivamente dispara a
     * query no Room (E2.6). `debounce(300)` + `distinctUntilChanged`
     * garantem que cada "rajada" de digitação só acarreta uma nova
     * consulta SQL. Quando o texto é vazio, usamos `0.milliseconds`
     * para que o "limpar busca" reaja imediatamente.
     */
    private val debouncedSearchInput: StateFlow<String> = _searchInput
        .debounce { value -> if (value.isEmpty()) 0.milliseconds else SEARCH_DEBOUNCE }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = "",
        )

    /**
     * Snapshot namespaced das preferências de listagem do owner
     * atual — search query + selected tag id + sort order.
     * Hidrata a partir do `ListingPreferencesRepository.observe(ownerId)`
     * e propaga para o pipeline de UI.
     */
    private val listingPrefs: Flow<ListingSnapshot> =
        activeUserProvider.observeActiveUserId()
            .flatMapLatest { ownerId ->
                if (ownerId == null) {
                    flowOf(ListingSnapshot.empty())
                } else {
                    listingPreferences.observe(ownerId).map { it.toSnapshot() }
                }
            }
            .distinctUntilChanged()

    /**
     * Tags convertidas em [TagChip] para o owner atual. Pré-mapeado
     * para que o `combine` no [uiState] possa trabalhar com tipos
     * `Flow<TagChip>` em vez de `Flow<Tag>` (mais simples para o
     * compilador inferir).
     */
    private val tagChipsForOwner: Flow<List<TagChip>> =
        activeUserProvider.observeActiveUserId()
            .flatMapLatest { ownerId ->
                if (ownerId == null) {
                    flowOf(emptyList())
                } else {
                    tagRepository.observeForOwner(ownerId)
                        .map { tags -> tags.map { it.toChip() } }
                }
            }
            .distinctUntilChanged()

    private data class ListingSnapshot(
        val searchQuery: String,
        val selectedTagId: String?,
        val sortOrder: SortOrder,
    ) {
        companion object {
            fun empty(): ListingSnapshot = ListingSnapshot("", null, SortOrder.CreatedDesc)
        }
    }

    private fun ListingPreferences.toSnapshot(): ListingSnapshot =
        ListingSnapshot(searchQuery, selectedTagId, sortOrder)

    private fun Tag.toChip(): TagChip = TagChip(id = id, name = name, color = color)

    /**
     * Estado da lista de projetos — E2.1 + E2.5 + E2.6 + E2.8.
     *
     * O `.catch` é o ponto de captura E2.8: quando o Room emite um
     * erro (e.g. banco corrompido, I/O falho), a exceção é
     * convertida em [HomeUiState.errorMessage] + `isLoading = false`.
     */
    val uiState: StateFlow<HomeUiState> = _retryToken
        .flatMapLatest { _ ->
            activeUserProvider.observeActiveUserId()
                .flatMapLatest { ownerId ->
                    if (ownerId == null) {
                        flowOf(HomeUiState(isLoading = false))
                    } else {
                        val capturedOwnerId = ownerId
                        // Combina os 4 sinais "instantâneos"
                        // (preferências, busca debounceada, tags em
                        // chips, filtro estrutural) e usa os valores
                        // para chamar `observeSearch` no Room. O
                        // `observeSearch` + `_searchInput` são
                        // combinados no `combine` interno (5 fontes
                        // no total: projects + searchInput +
                        // listingPrefs + tagChips + projectFilter).
                        val listingInputs: Flow<ListingInputs> = combine(
                            listingPrefs,
                            debouncedSearchInput,
                            tagChipsForOwner,
                            _projectFilter,
                        ) { prefs: ListingSnapshot, query: String,
                            tagChips: List<TagChip>, filter: HomeProjectFilter ->
                            // Sincroniza o input visual com o snapshot
                            // persistido quando o owner muda (ex.: login).
                            if (query.isEmpty() &&
                                _searchInput.value != prefs.searchQuery
                            ) {
                                _searchInput.value = prefs.searchQuery
                            }
                            ListingInputs(prefs, query, tagChips, filter)
                        }
                        combine(
                            listingInputs,
                            _searchInput,
                        ) { inputs: ListingInputs, currentInput: String ->
                            val projectsFromRoom: Flow<List<Project>> =
                                projectRepository.observeSearch(
                                    ownerId = capturedOwnerId,
                                    query = inputs.query,
                                    tagId = inputs.prefs.selectedTagId,
                                    sortOrder = inputs.prefs.sortOrder,
                                )
                            // E2.6/R5 — tags por projeto (não todas as
                            // tags do owner). Cada card lista apenas as
                            // tags associadas ao seu projeto.
                            val tagsByProject: Flow<Map<String, List<Tag>>> =
                                projectsFromRoom.flatMapLatest { projects ->
                                    tagRepository.observeByProjectIds(projects.map { it.id }.toSet())
                                }
                            combine(
                                projectsFromRoom,
                                tagsByProject,
                                flowOf(inputs),
                            ) { projects: List<Project>,
                                tagsMap: Map<String, List<Tag>>,
                                inp: ListingInputs,
                                ->
                                buildHomeUiState(inp, currentInput, projects, tagsMap)
                            }
                        }.flatMapLatest { it }
                    }
                }
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    val message = throwable.toHomeErrorMessage()
                    _errorMessage.value = message
                    emit(
                        HomeUiState(
                            isLoading = false,
                            projectFilter = _projectFilter.value,
                            errorMessage = message,
                        ),
                    )
                }
                .onEach { state: HomeUiState ->
                    if (state.errorMessage == null) {
                        _errorMessage.value = null
                    }
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = HomeUiState(),
        )

    private fun buildHomeUiState(
        inputs: ListingInputs,
        currentInput: String,
        rows: List<Project>,
        tagsByProject: Map<String, List<Tag>> = emptyMap(),
    ): HomeUiState {
        val filtered = rows.filter { project ->
            when (inputs.filter) {
                HomeProjectFilter.Active -> !project.isCompleted
                HomeProjectFilter.Completed -> project.isCompleted
            }
        }
        val items = filtered.map { project ->
            ProjectCardItem(
                project = project,
                tags = tagsByProject[project.id]?.map { it.toChip() } ?: emptyList(),
            )
        }
        return HomeUiState(
            projects = items,
            availableTags = inputs.tagChips,
            searchQuery = currentInput,
            selectedTagId = inputs.prefs.selectedTagId,
            sortOrder = inputs.prefs.sortOrder,
            isLoading = false,
            projectFilter = inputs.filter,
        )
    }

    /** Snapshot namespaced das 4 fontes do pipeline de UI. */
    private data class ListingInputs(
        val prefs: ListingSnapshot,
        val query: String,
        val tagChips: List<TagChip>,
        val filter: HomeProjectFilter,
    )

    /** Cria projeto no nome do owner ativo. */
    fun createProject(name: String, description: String?, tagIds: List<String>) {
        viewModelScope.launch {
            try {
                val ownerId = activeUserProvider.observeActiveUserId().first() ?: return@launch
                createProject.invoke(
                    name = name,
                    ownerId = ownerId,
                    description = description,
                    tagIds = tagIds,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _errorMessage.value = e.toHomeActionErrorMessage()
            }
        }
    }

    /** Atualiza projeto existente (campos + tags associadas). */
    fun updateProject(project: Project, tagIds: List<String>) {
        viewModelScope.launch {
            try {
                updateProject.invoke(project, tagIds)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _errorMessage.value = e.toHomeActionErrorMessage()
            }
        }
    }

    /** Remove projeto (tarefas e `project_tags` em cascata). */
    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            try {
                deleteProject.invoke(projectId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _errorMessage.value = e.toHomeActionErrorMessage()
            }
        }
    }

    /** Cria tag para o owner ativo. */
    fun createTag(name: String, color: String) {
        viewModelScope.launch {
            try {
                val ownerId = activeUserProvider.observeActiveUserId().first() ?: return@launch
                createTag.invoke(ownerId, name, color)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _errorMessage.value = e.toHomeActionErrorMessage()
            }
        }
    }

    /**
     * Atualiza a query de busca e dispara a persistência no
     * DataStore (E2.6). O pipeline de UI usa [searchQuery]
     * imediatamente para feedback visual; o Room só é consultado
     * depois do `debounce(300)` configurado em
     * [debouncedSearchInput].
     */
    fun onSearchQueryChange(newQuery: String) {
        _searchInput.value = newQuery
        viewModelScope.launch {
            val ownerId = activeUserProvider.observeActiveUserId().first() ?: return@launch
            listingPreferences.setSearchQuery(ownerId, newQuery)
        }
    }

    /**
     * Atualiza a tag selecionada como filtro (E2.6). Persistido
     * imediatamente — sem debounce, pois é uma seleção pontual.
     */
    fun onTagFilterChange(tagId: String?) {
        viewModelScope.launch {
            val ownerId = activeUserProvider.observeActiveUserId().first() ?: return@launch
            listingPreferences.setSelectedTagId(ownerId, tagId)
        }
    }

    /** Atualiza a ordenação (E2.6). Persistido imediatamente. */
    fun onSortOrderChange(order: SortOrder) {
        viewModelScope.launch {
            val ownerId = activeUserProvider.observeActiveUserId().first() ?: return@launch
            listingPreferences.setSortOrder(ownerId, order)
        }
    }

    /**
     * Re-assina o pipeline de observação dos repositórios após uma
     * falha (E2.8). Incrementa [retryToken], o que dispara o
     * `flatMapLatest` em [uiState] e descarta a coleta atual.
     */
    fun retry() {
        _errorMessage.value = null
        _retryToken.value = _retryToken.value + 1
    }

    /** Limpa a mensagem de erro atual (E2.8). Chamado pela UI quando o usuário dispensa o banner. */
    fun clearError() {
        _errorMessage.value = null
    }

    private fun User?.toHomeUserState(): HomeUserState = when (this) {
        null -> HomeUserState.SignedOut
        else -> HomeUserState.SignedIn(
            displayName = name,
            initials = computeInitials(name),
            role = role.toHomeRole(),
        )
    }

    companion object {
        /**
         * Mensagem de erro canônica usada quando o carregamento da
         * lista falha (E2.8). A UI resolve esta chave para o recurso
         * localizado via `R.string.home_error_load_failed`.
         */
        const val ERROR_LOAD_FAILED: String = "Não foi possível carregar seus projetos"

        /**
         * Mensagem de erro canônica para falhas de CRUD no Home
         * (criar/deletar projeto/tag).
         */
        const val ERROR_ACTION_FAILED: String = "Não foi possível concluir a operação"

        /** Indica se a mensagem é a de falha de carga. */
        fun isLoadErrorMessage(message: String): Boolean = message == ERROR_LOAD_FAILED

        /**
         * Janela de debounce aplicada à busca textual antes de
         * consultar o Room (E2.6). 300ms é o padrão da diretriz
         * Android para "input que dispara I/O".
         */
        val SEARCH_DEBOUNCE: kotlin.time.Duration = 300.milliseconds
    }
}

/** Mapeia [pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole]
 *  para a enumeração visual [HomeUserRole]. */
internal fun pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole.toHomeRole(): HomeUserRole =
    when (this) {
        pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole.OWNER -> HomeUserRole.Owner
        pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole.MEMBER -> HomeUserRole.Member
    }

/**
 * Converte uma [Throwable] vinda do Room em uma mensagem canônica
 * para a UI (E2.8).
 */
internal fun Throwable.toHomeErrorMessage(): String = HomeViewModel.ERROR_LOAD_FAILED

/** Mesmo mapeamento para ações (CRUD) — placeholder para evolução futura. */
internal fun Throwable.toHomeActionErrorMessage(): String = HomeViewModel.ERROR_ACTION_FAILED

/**
 * Gera o monograma do nome. Quando o nome tem mais de uma palavra,
 * usa a primeira letra do primeiro e do último nome. Quando tem
 * apenas uma palavra, usa as duas primeiras letras. Se estiver vazio
 * (não deve acontecer — `User` valida `name` em `init`), devolve "?".
 */
internal fun computeInitials(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "?"
    val parts = trimmed.split(' ').filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> {
            val first = parts.first().first().uppercaseChar()
            val last = parts.last().first().uppercaseChar()
            "$first$last"
        }
        else -> trimmed.take(2).uppercase()
    }
}
