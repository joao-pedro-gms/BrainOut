// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Construtores de pipeline da Home (E2.1/E2.6/E2.8). Reúnem os Flows
// que alimentam [HomeUiState]: preferências de listagem, busca
// debounceada, tags em chips e a combinação final Room + filtro
// estrutural. Extraídos do antigo HomeViewModel.kt (583 LoC).

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import pucgo.joaopedrogmsilva.brainout.core.data.session.ActiveUserProvider
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ListingPreferences
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ListingPreferencesRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TagRepository

/** Snapshot namespaced das preferências de listagem do owner atual. */
internal data class ListingSnapshot(
    val searchQuery: String,
    val selectedTagId: String?,
    val sortOrder: SortOrder,
) {
    companion object {
        fun empty(): ListingSnapshot = ListingSnapshot("", null, SortOrder.CreatedDesc)
    }
}

/** Snapshot namespaced das 4 fontes do pipeline de UI. */
internal data class ListingInputs(
    val prefs: ListingSnapshot,
    val query: String,
    val tagChips: List<TagChip>,
    val filter: HomeProjectFilter,
)

internal fun ListingPreferences.toSnapshot(): ListingSnapshot =
    ListingSnapshot(searchQuery, selectedTagId, sortOrder)

/**
 * Versão debounced do input de busca que efetivamente dispara a
 * query no Room (E2.6). `debounce(300)` + `distinctUntilChanged`
 * garantem que cada "rajada" de digitação só acarreta uma nova
 * consulta SQL. Quando o texto é vazio, usamos `0.milliseconds`
 * para que o "limpar busca" reaja imediatamente.
 */
@OptIn(FlowPreview::class)
internal fun debouncedSearchInput(
    scope: CoroutineScope,
    rawInput: MutableStateFlow<String>,
    debounceWindow: Duration,
): StateFlow<String> = rawInput
    .debounce { value -> if (value.isEmpty()) 0.milliseconds else debounceWindow }
    .distinctUntilChanged()
    .stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = "",
    )

/**
 * Snapshot namespaced das preferências de listagem do owner atual.
 * Hidrata a partir do `ListingPreferencesRepository.observe(ownerId)`
 * e propaga para o pipeline de UI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun listingPrefsFlow(
    activeUserProvider: ActiveUserProvider,
    listingPreferences: ListingPreferencesRepository,
): Flow<ListingSnapshot> =
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
 * para que o `combine` possa trabalhar com `Flow<TagChip>` em vez de
 * `Flow<Tag>` (mais simples para o compilador inferir).
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun tagChipsForOwnerFlow(
    activeUserProvider: ActiveUserProvider,
    tagRepository: TagRepository,
): Flow<List<TagChip>> =
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

/**
 * Pipeline central da Home (E2.1/E2.5/E2.6/E2.8). Combina os 4
 * sinais "instantâneos" (preferências, busca debounceada, tags em
 * chips, filtro estrutural), usa-os para chamar `observeSearch` no
 * Room e mapeia o resultado em [HomeUiState] via [buildHomeUiState].
 *
 * O `.catch` é o ponto de captura E2.8: quando o Room emite um erro
 * (e.g. banco corrompido, I/O falho), a exceção é convertida em
 * [HomeUiState.errorMessage] + `isLoading = false`. A versão
 * debounceada do input é sincronizada com o snapshot persistido
 * quando o owner muda (ex.: login).
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Suppress("LongMethod", "LongParameterList")
internal fun homeUiStateFlow(
    retryToken: MutableStateFlow<Int>,
    activeUserProvider: ActiveUserProvider,
    listingPreferences: ListingPreferencesRepository,
    tagRepository: TagRepository,
    projectRepository: ProjectRepository,
    searchInput: MutableStateFlow<String>,
    projectFilter: MutableStateFlow<HomeProjectFilter>,
    debouncedInput: StateFlow<String>,
    errorMessage: MutableStateFlow<String?>,
): Flow<HomeUiState> = retryToken.flatMapLatest { _ ->
    activeUserProvider.observeActiveUserId()
        .flatMapLatest { ownerId ->
            if (ownerId == null) {
                flowOf(HomeUiState(isLoading = false))
            } else {
                val capturedOwnerId = ownerId
                // Combina os 4 sinais "instantâneos" (preferências,
                // busca debounceada, tags em chips, filtro
                // estrutural) e usa os valores para chamar
                // `observeSearch` no Room. O `observeSearch` +
                // `_searchInput` são combinados no `combine` interno
                // (5 fontes no total: projects + searchInput +
                // listingPrefs + tagChips + projectFilter).
                val listingInputs: Flow<ListingInputs> = combine(
                    listingPrefsFlow(activeUserProvider, listingPreferences),
                    debouncedInput,
                    tagChipsForOwnerFlow(activeUserProvider, tagRepository),
                    projectFilter,
                ) { prefs: ListingSnapshot, query: String,
                    tagChips: List<TagChip>, filter: HomeProjectFilter ->
                    // Sincroniza o input visual com o snapshot
                    // persistido quando o owner muda (ex.: login).
                    if (query.isEmpty() && searchInput.value != prefs.searchQuery) {
                        searchInput.value = prefs.searchQuery
                    }
                    ListingInputs(prefs, query, tagChips, filter)
                }
                combine(listingInputs, searchInput) { inputs: ListingInputs, currentInput: String ->
                    val projectsFromRoom: Flow<List<Project>> = projectRepository.observeSearch(
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
            errorMessage.value = message
            emit(
                HomeUiState(
                    isLoading = false,
                    projectFilter = projectFilter.value,
                    errorMessage = message,
                ),
            )
        }
        .onEach { state: HomeUiState ->
            if (state.errorMessage == null) {
                errorMessage.value = null
            }
        }
}

/**
 * Constrói o [HomeUiState] final a partir das fontes combinadas
 * (E2.5/E2.6). Filtra por status estrutural e mapeia projetos em
 * [ProjectCardItem], anexando as tags associadas por projeto.
 */
internal fun buildHomeUiState(
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
