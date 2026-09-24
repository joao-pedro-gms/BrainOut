// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Conteúdo da aba "Projetos" da Home: banner offline, busca, filtro
// por tag, seção estrutural, filtro Ativos/Concluídos com menu de
// ordenação, e a lista de cards. A escolha entre erro/loader/empty
// state/lista é feita aqui (E2.8) — o orchestrator só decide qual
// aba está ativa.
//
// `LongParameterList` é necessário: o conteúdo consome todos os
// campos do `HomeUiState` + callbacks do ViewModel; quebrar em
// sub-estados obrigaria novo `data class` só para a UI.

@file:Suppress("LongParameterList", "LongMethod")

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

@Composable
internal fun HomeProjectsContent(
    contentPadding: PaddingValues,
    onOpenProject: (projectId: String) -> Unit,
    projects: List<ProjectCardItem>,
    isLoading: Boolean,
    currentFilter: HomeProjectFilter,
    onSelectFilter: (HomeProjectFilter) -> Unit,
    errorMessage: String?,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    availableTags: List<TagChip>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedTagId: String?,
    onTagFilterChange: (String?) -> Unit,
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    syncState: HomeSyncState,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // E3.4 — banner persistente de conectividade + contagem da
        // fila de sincronização. Fica acima da busca: é estado global
        // da tela, não da lista. Sempre visível quando offline.
        HomeOfflineBanner(
            syncState = syncState,
        )
        // E2.6 — barra de busca textual. `OutlinedTextField` com ícone
        // de limpar (X) à direita quando o texto não está vazio. O
        // placeholder vem de `values/strings.xml` para manter a regra
        // E4.6 (pt/en).
        HomeSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
        )
        // E2.6 — chips horizontais com todas as tags do usuário +
        // "Todas" para o filtro por tag. Renderizado como `LazyRow`
        // para escalar quando o usuário tem muitas tags.
        HomeTagFilterRow(
            availableTags = availableTags,
            selectedTagId = selectedTagId,
            onSelect = onTagFilterChange,
        )
        Text(
            text = when (currentFilter) {
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
            current = currentFilter,
            onSelect = onSelectFilter,
            sortOrder = sortOrder,
            onSortOrderChange = onSortOrderChange,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        // E2.8 — banner de erro com retry. Tem prioridade sobre o
        // estado vazio: se o Room falhou, oferecemos "Tentar
        // novamente" em vez do empty state (que mostraria uma
        // coleção vazia de forma enganosa).
        if (errorMessage != null) {
            HomeErrorBanner(
                message = errorMessage,
                onRetry = onRetry,
                onDismiss = onDismissError,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
            )
        } else if (projects.isEmpty() && isLoading) {
            // E2.8 — loader enquanto o Flow do Room não emite a
            // primeira lista. Empty state só aparece após a primeira
            // coleta efetiva, evitando a sobreposição das duas
            // visualizações.
            HomeLoadingState(modifier = Modifier.fillMaxWidth())
        } else if (projects.isEmpty()) {
            // E2.6 — diferenciar "lista vazia sem filtro" de
            // "filtro/busca não retornou nada". No segundo caso, a
            // mensagem cita a query (ou a tag) que está restringindo
            // a visualização.
            if (searchQuery.isNotBlank() || selectedTagId != null) {
                HomeNoMatchesState(
                    query = searchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                )
            } else if (currentFilter == HomeProjectFilter.Completed) {
                HomeCompletedEmptyState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                )
            } else {
                HomeEmptyState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = projects, key = { it.project.id }) { card ->
                    ProjectCard(
                        item = card,
                        onClick = { onOpenProject(card.project.id) },
                    )
                }
            }
        }
    }
}
