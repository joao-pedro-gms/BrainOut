// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pucgo.joaopedrogmsilva.brainout.core.data.session.ActiveUserProvider
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TagRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CreateProjectUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CreateTagUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.DeleteProjectUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.UpdateProjectUseCase

/**
 * Estado da sessão ativa usado pela [HomeScreen] para saudação,
 * monograma e badge de papel.
 *
 * Mantido como `sealed interface` distinto de [HomeUiState] porque a
 * lista de projetos tem cardinalidade dinâmica (vazia / com itens /
 * carregando / erro), enquanto o estado de sessão é uma enumeração
 * finita (Loading / SignedOut / SignedIn).
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
 * Estado da lista de projetos (E2.1/E2.6/E2.5) consumido pela [HomeScreen].
 *
 * - [projects] lista de cards visíveis após aplicar
 *   [projectFilter] — RN03 (E2.5) subdivide a lista em "Ativos"
 *   (padrão) e "Concluídos" via [HomeProjectFilter].
 * - [availableTags] todas as tags do owner; usadas no diálogo de
 *   criação para seleção múltipla.
 * - [isLoading] `true` enquanto os Flows do Room não emitem o primeiro
 *   snapshot após a troca de owner.
 * - [errorMessage] mensagem da última falha de domínio ao criar /
 *   deletar projeto / tag — `null` quando não há erro pendente.
 * - [projectFilter] filtro ativo selecionado pelo usuário. A UI
 *   pode chamar [HomeViewModel.setProjectFilter] para alternar.
 */
data class HomeUiState(
    val projects: List<ProjectCardItem> = emptyList(),
    val availableTags: List<TagChip> = emptyList(),
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
 * - E2.1 — observar projetos + tags do owner em [uiState], combinando
 *   os Flows de [ProjectRepository] e [TagRepository].
 * - E2.6 — disparar `createProject` / `deleteProject` / `createTag`
 *   através dos use cases de domínio correspondentes.
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val tagRepository: TagRepository,
    private val activeUserProvider: ActiveUserProvider,
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
     * Filtro ativo da lista de projetos (E2.5 — RN03). Mantido
     * em [MutableStateFlow] para que a UI possa alternar sem
     * precisar pedir ao ViewModel para recarregar.
     */
    private val _projectFilter: MutableStateFlow<HomeProjectFilter> =
        MutableStateFlow(HomeProjectFilter.Active)
    val projectFilter: StateFlow<HomeProjectFilter> = _projectFilter.asStateFlow()

    /** Define o filtro ativo. Chamado pela UI nos toggles chips. */
    fun setProjectFilter(filter: HomeProjectFilter) {
        _projectFilter.value = filter
    }

    /**
     * Estado da lista de projetos — E2.1 + E2.5.
     *
     * Troca de owner reinicia o pipeline via [flatMapLatest], e cada
     * par `(projects, tags, filter)` é combinado em [HomeUiState].
     * O filtro é aplicado aqui no ViewModel para que a UI receba
     * apenas os cards visíveis (sem depender de lógica de filtragem
     * no composable). Se o owner for `null` (sem sessão), emite um
     * estado vazio com [HomeUiState.isLoading] `false`.
     */
    val uiState: StateFlow<HomeUiState> = activeUserProvider
        .observeActiveUserId()
        .flatMapLatest { ownerId ->
            if (ownerId == null) {
                flowOf(HomeUiState(isLoading = false))
            } else {
                combine(
                    projectRepository.observeAllForOwner(ownerId),
                    tagRepository.observeForOwner(ownerId),
                    _projectFilter,
                ) { projects, tags, filter ->
                    val tagChips = tags.map { it.toChip() }
                    val filtered = projects.filter { project ->
                        when (filter) {
                            HomeProjectFilter.Active -> !project.isCompleted
                            HomeProjectFilter.Completed -> project.isCompleted
                        }
                    }
                    val items = filtered.map { project ->
                        ProjectCardItem(
                            project = project,
                            tags = tagChips,
                        )
                    }
                    HomeUiState(
                        projects = items,
                        availableTags = tagChips,
                        isLoading = false,
                        projectFilter = filter,
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = HomeUiState(),
        )

    /** Cria projeto no nome do owner ativo. */
    fun createProject(name: String, description: String?, tagIds: List<String>) {
        viewModelScope.launch {
            val ownerId = activeUserProvider.observeActiveUserId().first() ?: return@launch
            createProject.invoke(
                name = name,
                ownerId = ownerId,
                description = description,
                tagIds = tagIds,
            )
        }
    }

    /** Atualiza projeto existente (campos + tags associadas). */
    fun updateProject(project: Project, tagIds: List<String>) {
        viewModelScope.launch { updateProject.invoke(project, tagIds) }
    }

    /** Remove projeto (tarefas e `project_tags` em cascata). */
    fun deleteProject(projectId: String) {
        viewModelScope.launch { deleteProject.invoke(projectId) }
    }

    /** Cria tag para o owner ativo. */
    fun createTag(name: String, color: String) {
        viewModelScope.launch {
            val ownerId = activeUserProvider.observeActiveUserId().first() ?: return@launch
            createTag.invoke(ownerId, name, color)
        }
    }

    private fun User?.toHomeUserState(): HomeUserState = when (this) {
        null -> HomeUserState.SignedOut
        else -> HomeUserState.SignedIn(
            displayName = name,
            initials = computeInitials(name),
            role = role.toHomeRole(),
        )
    }

    private fun Tag.toChip(): TagChip = TagChip(id = id, name = name, color = color)
}

/** Mapeia [pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole]
 *  para a enumeração visual [HomeUserRole]. */
internal fun pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole.toHomeRole(): HomeUserRole =
    when (this) {
        pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole.OWNER -> HomeUserRole.Owner
        pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole.MEMBER -> HomeUserRole.Member
    }

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
