// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
//
// `LongMethod`: o corpo é a composição da lista de projetos — grade
// responsiva, empty states, banner de erro e o botão flutuante da
// Home. Cada bloco já é uma função própria (`HomeEmptyStates`,
// `HomeErrorBanner`); quebrar mais ainda espalharia o estado do
// filtro entre chamadas.

@file:Suppress("LongMethod")

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.LocalNeoWindowInfo
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoMotion
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSpacing
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.neoContentWidth
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

/**
 * Estado consumido pela lista de projetos da Home.
 *
 * @property projects cards a exibir.
 * @property isLoading a primeira coleta do Room ainda não aconteceu.
 * @property currentFilter filtro ativo (ativos ou concluídos).
 * @property errorMessage erro de carga ou de ação, quando houver.
 * @property availableTags tags do usuário, para o filtro.
 * @property searchQuery texto de busca digitado.
 * @property selectedTagId tag que restringe a lista, se houver.
 * @property sortOrder ordenação escolhida.
 */
internal data class HomeProjectsState(
    val projects: List<ProjectCardItem>,
    val isLoading: Boolean,
    val currentFilter: HomeProjectFilter,
    val errorMessage: HomeErrorType?,
    val availableTags: List<TagChip>,
    val searchQuery: String,
    val selectedTagId: String?,
    val sortOrder: SortOrder,
)

/**
 * Ações disparadas pela lista de projetos da Home.
 */
internal data class HomeProjectsActions(
    val onOpenProject: (projectId: String) -> Unit,
    val onSelectFilter: (HomeProjectFilter) -> Unit,
    val onRetry: () -> Unit,
    val onDismissError: () -> Unit,
    val onSearchQueryChange: (String) -> Unit,
    val onTagFilterChange: (String?) -> Unit,
    val onSortOrderChange: (SortOrder) -> Unit,
)

@Composable
internal fun HomeProjectsContent(
    contentPadding: PaddingValues,
    state: HomeProjectsState,
    actions: HomeProjectsActions,
) {
    val window = LocalNeoWindowInfo.current
    LazyVerticalGrid(
        columns = if (window.forceSingleColumn) GridCells.Fixed(1) else GridCells.Adaptive(240.dp),
        modifier =
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .neoContentWidth()
                .padding(horizontal = LocalNeoWindowInfo.current.margin, vertical = NeoSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(NeoSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(NeoSpacing.sm),
        contentPadding = PaddingValues(bottom = 88.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(NeoSpacing.sm)) {
                // E2.6 — barra de busca textual. `OutlinedTextField` com ícone
                // de limpar (X) à direita quando o texto não está vazio. O
                // placeholder vem de `values/strings.xml` para manter a regra
                // E4.6 (pt/en).
                HomeSearchBar(
                    query = state.searchQuery,
                    onQueryChange = actions.onSearchQueryChange,
                )
                // E2.6 — chips horizontais com todas as tags do usuário +
                // "Todas" para o filtro por tag. Renderizado como `LazyRow`
                // para escalar quando o usuário tem muitas tags.
                HomeTagFilterRow(
                    availableTags = state.availableTags,
                    selectedTagId = state.selectedTagId,
                    onSelect = actions.onTagFilterChange,
                )
                Text(
                    text =
                        when (state.currentFilter) {
                            HomeProjectFilter.Active -> stringResource(id = R.string.home_section_title)
                            HomeProjectFilter.Completed ->
                                stringResource(id = R.string.home_section_completed_title)
                        },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                // Toggle de filtros Ativos / Concluídos (RN03 — E2.5).
                // Fica sempre visível para que o usuário possa alternar mesmo
                // quando uma das listas estiver vazia.
                HomeProjectFilterRow(
                    current = state.currentFilter,
                    onSelect = actions.onSelectFilter,
                    sortOrder = state.sortOrder,
                    onSortOrderChange = actions.onSortOrderChange,
                )
                // DEF-22 — divisor com o token cheio: `outline` puro marca
                // 16.10:1 (claro) / 15.95:1 (escuro); com `alpha = 0.3f` caía
                // para 1.93:1, abaixo do mínimo de 3:1 do WCAG §1.4.11.
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                // E2.8 — banner de erro com retry. Tem prioridade sobre o
                // estado vazio: se o Room falhou, oferecemos "Tentar
                // novamente" em vez do empty state (que mostraria uma
                // coleção vazia de forma enganosa).
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            val errorMessage = state.errorMessage
            if (errorMessage != null) {
                HomeErrorBanner(
                    error = errorMessage,
                    onRetry = actions.onRetry,
                    onDismiss = actions.onDismissError,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                )
            } else if (state.projects.isEmpty() && state.isLoading) {
                // E2.8 — loader enquanto o Flow do Room não emite a
                // primeira lista. Empty state só aparece após a primeira
                // coleta efetiva, evitando a sobreposição das duas
                // visualizações.
                HomeLoadingState(modifier = Modifier.fillMaxWidth())
            } else if (state.projects.isEmpty()) {
                // E2.6 — diferenciar "lista vazia sem filtro" de
                // "filtro/busca não retornou nada". No segundo caso, a
                // mensagem cita a query (ou a tag) que está restringindo
                // a visualização.
                if (state.searchQuery.isNotBlank() || state.selectedTagId != null) {
                    HomeNoMatchesState(
                        query = state.searchQuery,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 160.dp),
                    )
                } else if (state.currentFilter == HomeProjectFilter.Completed) {
                    HomeEmptyState(
                        titleRes = R.string.home_completed_empty_title,
                        bodyRes = R.string.home_completed_empty_body,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 160.dp),
                    )
                } else {
                    HomeEmptyState(
                        titleRes = R.string.home_empty_title,
                        bodyRes = R.string.home_empty_body,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 160.dp),
                    )
                }
            }
        }
        items(items = state.projects, key = { it.project.id }) { card ->
            androidx.compose.foundation.layout.Box(
                Modifier.animateItem(
                    fadeInSpec =
                        if (state.searchQuery.isBlank() &&
                            state.selectedTagId == null
                        ) {
                            tween(NeoMotion.selection)
                        } else {
                            null
                        },
                    fadeOutSpec = null,
                    placementSpec =
                        if (state.searchQuery.isBlank() &&
                            state.selectedTagId == null
                        ) {
                            tween(NeoMotion.selection)
                        } else {
                            null
                        },
                ),
            ) {
                ProjectCard(item = card, onClick = { actions.onOpenProject(card.project.id) })
            }
        }
    }
}
