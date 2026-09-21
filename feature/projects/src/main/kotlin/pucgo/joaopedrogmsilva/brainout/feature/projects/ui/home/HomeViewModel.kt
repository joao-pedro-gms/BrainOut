// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
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
 * Estado da lista de projetos (E2.1/E2.6/E2.5/E2.8) consumido pela
 * [HomeScreen].
 *
 * - [projects] lista de cards visíveis após aplicar
 *   [projectFilter] — RN03 (E2.5) subdivide a lista em "Ativos"
 *   (padrão) e "Concluídos" via [HomeProjectFilter].
 * - [availableTags] todas as tags do owner; usadas no diálogo de
 *   criação para seleção múltipla.
 * - [isLoading] `true` enquanto os Flows do Room não emitem o primeiro
 *   snapshot após a troca de owner ou após um [HomeViewModel.retry].
 * - [errorMessage] mensagem da última falha ao carregar a lista do
 *   Room ou ao executar CRUD (criar/deletar projeto/tag). `null`
 *   quando não há erro pendente. Populado por E2.8.
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
 * - E2.8 — capturar falhas dos Flows do Room e dos use cases de
 *   CRUD; expor [HomeUiState.errorMessage] e oferecer [retry] /
 *   [clearError] para que a UI apresente banner de erro com botão
 *   "Tentar novamente" sem travar a tela.
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
     * Token de retry (E2.8). Cada chamada a [retry] incrementa este
     * valor; o pipeline [uiState] depende dele como chave de
     * `flatMapLatest`, então a mudança descarta o Flow anterior
     * (liberando o `WhileSubscribed` interno) e inicia uma nova
     * inscrição nos repositórios. Mantém o padrão de "cancelar antes
     * de tentar de novo" sem precisar manipular o `Job`
     * manualmente.
     */
    private val _retryToken: MutableStateFlow<Int> = MutableStateFlow(0)
    val retryToken: StateFlow<Int> = _retryToken.asStateFlow()

    /**
     * Mensagem de erro pendente para a UI (E2.8). Alimentada por
     * falhas dos Flows de repositório e dos use cases de CRUD.
     * Resetada por [clearError], por uma nova coleta bem-sucedida
     * ou por [retry].
     */
    private val _errorMessage: MutableStateFlow<String?> = MutableStateFlow(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Estado da lista de projetos — E2.1 + E2.5 + E2.8.
     *
     * Troca de owner reinicia o pipeline via [flatMapLatest], e cada
     * par `(projects, tags, filter)` é combinado em [HomeUiState].
     * O filtro é aplicado aqui no ViewModel para que a UI receba
     * apenas os cards visíveis (sem depender de lógica de filtragem
     * no composable). Se o owner for `null` (sem sessão), emite um
     * estado vazio com [HomeUiState.isLoading] `false`.
     *
     * O `.catch` é o ponto de captura E2.8: quando o Room emite um
     * erro (e.g. banco corrompido, I/O falho), a exceção é
     * convertida em [HomeUiState.errorMessage] + `isLoading = false`
     * — sem isso o [StateFlow] ficaria preso em "Loading" para
     * sempre. [CancellationException] é re-lançada para preservar o
     * cancelamento estruturado de corrotinas.
     */
    val uiState: StateFlow<HomeUiState> = _retryToken
        .flatMapLatest { _ ->
            activeUserProvider.observeActiveUserId()
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
                // Limpa erro pendente APENAS em emissões vindas do
                // `combine` (sucesso), não nas emitidas pelo `catch`
                // acima — identificadas pelo `errorMessage == null`.
                // Sem esse filtro, o `catch` emitiria um estado com
                // erro e o `onEach` seguinte o resetaria para `null`
                // imediatamente, "engolindo" a falha. (E2.8)
                .onEach { state ->
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

    private fun Tag.toChip(): TagChip = TagChip(id = id, name = name, color = color)

    companion object {
        /**
         * Mensagem de erro canônica usada quando o carregamento da
         * lista falha (E2.8). A UI resolve esta chave para o recurso
         * localizado via `R.string.home_error_load_failed`.
         */
        const val ERROR_LOAD_FAILED: String = "Não foi possível carregar seus projetos"

        /**
         * Mensagem de erro canônica para falhas de CRUD no Home
         * (criar/deletar projeto/tag). Diferencia da falha de carga
         * para que o banner possa usar texto mais específico se
         * desejado em iteração futura.
         */
        const val ERROR_ACTION_FAILED: String = "Não foi possível concluir a operação"

        /** Indica se a mensagem é a de falha de carga. */
        fun isLoadErrorMessage(message: String): Boolean = message == ERROR_LOAD_FAILED
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
 * para a UI (E2.8). Mantém a regra de não expor stack traces: a UI
 * só recebe a chave de recurso; o detalhe técnico fica em logcat.
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
