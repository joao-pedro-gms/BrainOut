// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Construtores de pipeline da Home (E2.1/E2.6/E2.8). Reúnem os Flows
// que alimentam [HomeUiState]: preferências de listagem, busca
// debounceada, tags em chips e a combinação final Room + filtro
// estrutural. Extraídos do antigo HomeViewModel.kt (583 LoC).

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import pucgo.joaopedrogmsilva.brainout.core.data.session.ActiveUserProvider
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ListingPreferences
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ListingPreferencesRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TagRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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

/**
 * As dependências que o pipeline da Home consulta: a sessão ativa e os
 * três repositórios que respondem pela listagem.
 *
 * Agrupadas num objeto só para que a assinatura do pipeline não carregue
 * nove parâmetros — a lista completa estava no limiar do `LongParameterList`
 * do detekt e dependia de um `@Suppress`.
 */
internal data class ListingDependencies(
    val activeUserProvider: ActiveUserProvider,
    val listingPreferences: ListingPreferencesRepository,
    val projectRepository: ProjectRepository,
    val tagRepository: TagRepository,
)

/**
 * Os sinais que o pipeline lê e escreve: o token de retry, o texto cru e
 * o debounceado da busca, o filtro estrutural e o canal de erro.
 *
 * Separados das [ListingDependencies] porque têm natureza diferente —
 * são estado mutável, não collaborators de leitura.
 */
internal data class ListingSignals(
    val retryToken: MutableStateFlow<Int>,
    val searchInput: MutableStateFlow<String>,
    val projectFilter: MutableStateFlow<HomeProjectFilter>,
    val debouncedInput: StateFlow<String>,
    val errorSink: MutableStateFlow<HomeErrorType?>,
)

internal fun ListingPreferences.toSnapshot(): ListingSnapshot = ListingSnapshot(searchQuery, selectedTagId, sortOrder)

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
): StateFlow<String> =
    rawInput
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
    activeUserProvider
        .observeActiveUserId()
        .flatMapLatest { ownerId ->
            if (ownerId == null) {
                flowOf(ListingSnapshot.empty())
            } else {
                listingPreferences.observe(ownerId).map { it.toSnapshot() }
            }
        }.distinctUntilChanged()

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
    activeUserProvider
        .observeActiveUserId()
        .flatMapLatest { ownerId ->
            if (ownerId == null) {
                flowOf(emptyList())
            } else {
                tagRepository
                    .observeForOwner(ownerId)
                    .map { tags -> tags.map { it.toChip() } }
            }
        }.distinctUntilChanged()

/**
 * Os 4 sinais "instantâneos" que alimentam o pipeline: preferências,
 * busca debounceada, tags em chips e filtro estrutural.
 *
 * A sincronização do input visual com o snapshot persistido acontece
 * aqui, e **fora** do `combine`: o input cru muda a cada tecla e, se
 * participasse do gatilho de assinatura, refaria a consulta do Room a
 * cada tecla — o oposto do que o `debounce` promete.
 */
private fun listingInputsFlow(
    dependencies: ListingDependencies,
    signals: ListingSignals,
): Flow<ListingInputs> =
    combine(
        listingPrefsFlow(dependencies.activeUserProvider, dependencies.listingPreferences),
        signals.debouncedInput,
        tagChipsForOwnerFlow(dependencies.activeUserProvider, dependencies.tagRepository),
        signals.projectFilter,
    ) {
        prefs: ListingSnapshot,
        query: String,
        tagChips: List<TagChip>,
        filter: HomeProjectFilter,
        ->
        ListingInputs(prefs, query, tagChips, filter)
    }.onEach { inputs ->
        if (inputs.query.isEmpty() && signals.searchInput.value != inputs.prefs.searchQuery) {
            signals.searchInput.value = inputs.prefs.searchQuery
        }
    }

/**
 * A consulta ao Room e as tags por projeto, combinadas no estado final.
 */
@OptIn(ExperimentalCoroutinesApi::class)
private fun projectsWithTagsFlow(
    ownerId: String,
    inputs: ListingInputs,
    dependencies: ListingDependencies,
): Flow<HomeUiState> {
    val projectsFromRoom: Flow<List<Project>> =
        dependencies.projectRepository.observeSearch(
            ownerId = ownerId,
            query = inputs.query,
            tagId = inputs.prefs.selectedTagId,
            sortOrder = inputs.prefs.sortOrder,
        )
    // E2.6/R5 — tags por projeto (não todas as tags do owner). Cada
    // card lista apenas as tags associadas ao seu projeto.
    //
    // `observeByProjectIds` respeita o contrato "conjunto vazio → mapa
    // vazio" (uma única consulta do Room), então o `onStart` aqui não é
    // remendo: é só o valor inicial enquanto a consulta não responde —
    // evita um piscar de "sem tags" em cada card.
    val tagsByProject: Flow<Map<String, List<Tag>>> =
        projectsFromRoom.flatMapLatest { projects ->
            dependencies.tagRepository
                .observeByProjectIds(projects.map { it.id }.toSet())
                .onStart { emit(emptyMap()) }
        }
    return combine(projectsFromRoom, tagsByProject) { projects, tagsMap ->
        buildHomeUiState(inputs, projects, tagsMap)
    }
}

/**
 * Pipeline central da Home (E2.1/E2.5/E2.6/E2.8). Combina os 4
 * sinais "instantâneos" (preferências, busca debounceada, tags em
 * chips, filtro estrutural), usa-os para chamar `observeSearch` no
 * Room e mapeia o resultado em [HomeUiState] via [buildHomeUiState].
 *
 * O `.catch` é o ponto de captura E2.8: quando o Room emite um erro
 * (e.g. banco corrompido, I/O falho), a exceção é registrada em
 * [ListingSignals.errorSink] e o estado sai do loading.
 *
 * O erro **não** viaja dentro do [HomeUiState]: o canal é lido pelo
 * ViewModel e combinado com o estado, de modo que "Dispensar" (que
 * limpa o canal) faça o banner sumir. Sem isso, o erro ficaria
 * congelado na última emissão do pipeline e o banner persistiria na
 * tela (DEF-47).
 *
 * A versão debounceada do input é sincronizada com o snapshot
 * persistido quando o owner muda (ex.: login).
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
internal fun homeUiStateFlow(
    dependencies: ListingDependencies,
    signals: ListingSignals,
): Flow<HomeUiState> =
    signals.retryToken.flatMapLatest { _ ->
        dependencies.activeUserProvider
            .observeActiveUserId()
            .flatMapLatest { ownerId ->
                if (ownerId == null) {
                    flowOf(HomeUiState(isLoading = false))
                } else {
                    val capturedOwnerId = ownerId
                    listingInputsFlow(dependencies, signals)
                        .flatMapLatest { inputs ->
                            projectsWithTagsFlow(capturedOwnerId, inputs, dependencies)
                        }.combine(signals.searchInput) { state: HomeUiState, currentInput: String ->
                            state.copy(searchQuery = currentInput)
                        }
                }
            }.onEach {
                // Leitura bem-sucedida: o erro pendente (de carga ou de
                // ação) deixa de valer. O `catch` abaixo reescreve o
                // canal quando a leitura falha, então os dois passos
                // não se anulam — por isso o `onEach` fica **antes**
                // do `catch`.
                signals.errorSink.value = null
            }.catch { throwable ->
                if (throwable is CancellationException) throw throwable
                signals.errorSink.value = HomeErrorType.LoadFailed
                emit(
                    HomeUiState(
                        isLoading = false,
                        projectFilter = signals.projectFilter.value,
                    ),
                )
            }
    }

/**
 * Constrói o [HomeUiState] final a partir das fontes combinadas
 * (E2.5/E2.6). Filtra por status estrutural e mapeia projetos em
 * [ProjectCardItem], anexando as tags associadas por projeto.
 */
internal fun buildHomeUiState(
    inputs: ListingInputs,
    rows: List<Project>,
    tagsByProject: Map<String, List<Tag>> = emptyMap(),
): HomeUiState {
    val filtered =
        rows.filter { project ->
            when (inputs.filter) {
                HomeProjectFilter.Active -> !project.isCompleted
                HomeProjectFilter.Completed -> project.isCompleted
            }
        }
    val items =
        filtered.map { project ->
            ProjectCardItem(
                project = project,
                tags = tagsByProject[project.id]?.map { it.toChip() } ?: emptyList(),
            )
        }
    return HomeUiState(
        projects = items,
        availableTags = inputs.tagChips,
        // O `searchQuery` do estado é o texto **cru** (o que o usuário
        // digitou); o pipeline o sobrescreve logo em seguida, no
        // `combine` com o input cru.
        searchQuery = inputs.query,
        selectedTagId = inputs.prefs.selectedTagId,
        sortOrder = inputs.prefs.sortOrder,
        isLoading = false,
        projectFilter = inputs.filter,
    )
}
