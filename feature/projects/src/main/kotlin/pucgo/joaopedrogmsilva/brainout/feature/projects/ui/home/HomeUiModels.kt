// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Modelos de estado da Home (E1.7/E2.1/E2.5/E2.6/E2.8) consumidos
// pela [HomeScreen] e produzidos pelo [HomeViewModel]. Extraídos do
// antigo HomeViewModel.kt (583 LoC) para manter o orquestrador enxuto.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder

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
