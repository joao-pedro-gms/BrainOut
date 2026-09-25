// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Filtros estruturais da Home (RN03 — E2.5) e menu de ordenação
// (E2.6). O valor ativo do menu recebe leading check para que o
// usuário saiba qual opção está selecionada.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeProjectFilterRow(
    current: HomeProjectFilter,
    onSelect: (HomeProjectFilter) -> Unit,
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.FILTER_GROUP),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeProjectFilter.entries.forEach { option ->
            FilterChip(
                selected = option == current,
                onClick = { onSelect(option) },
                label = {
                    Text(
                        text = stringResource(id = option.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                modifier = Modifier
                    .testTag(
                        when (option) {
                            HomeProjectFilter.Active -> HomeTestTags.FILTER_ACTIVE
                            HomeProjectFilter.Completed -> HomeTestTags.FILTER_COMPLETED
                        },
                    )
                    // E4.4: 48dp mínimo para área de toque (WCAG 2.5.5).
                    .heightIn(min = 48.dp),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        // Rótulo de acessibilidade oculto para o agrupamento (TalkBack).
        Text(
            text = stringResource(id = R.string.home_filter_aria_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // E2.6 — menu de ordenação (Nome A→Z / Z→A / Mais recentes /
        // Mais antigas). Dispara um `DropdownMenu` ancorado no
        // `IconButton` de sort. O valor ativo fica marcado com
        // leading checkmark via `leadingIcon`.
        HomeSortMenu(
            current = sortOrder,
            onSelect = onSortOrderChange,
        )
    }
}

/**
 * Menu dropdown de ordenação (E2.6). Acionado pelo ícone de sort
 * (`Icons.Outlined.Sort`) à direita da linha de filtros estruturais.
 * O item ativo recebe um leading check via `leadingIcon`; sem isso,
 * o usuário teria que adivinhar qual opção está selecionada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeSortMenu(
    current: SortOrder,
    onSelect: (SortOrder) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .testTag(HomeTestTags.SORT_MENU_BUTTON)
                // E4.4: 48dp mínimo WCAG 2.5.5.
                .heightIn(min = 48.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Sort,
                contentDescription = stringResource(id = R.string.home_sort_aria_label),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.testTag(HomeTestTags.SORT_MENU),
        ) {
            SortOrder.entries.forEach { order ->
                val isSelected = order == current
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = sortOrderLabelRes(order))) },
                    onClick = {
                        onSelect(order)
                        expanded = false
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                            )
                        }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .testTag(HomeTestTags.sortMenuItem(order))
                        .heightIn(min = 48.dp),
                )
            }
        }
    }
}

/**
 * Resolve o rótulo localizado de uma [SortOrder] para a UI
 * (E2.6 do ROADMAP).
 *
 * Mantida fora do enum em `:core:domain` porque o enum não tem
 * acesso ao `R.string` da feature. A correspondência é exaustiva
 * (`when` sem `else`) — adições/remoções em [SortOrder] serão
 * sinalizadas em tempo de compilação aqui.
 */
@androidx.annotation.StringRes
internal fun sortOrderLabelRes(order: SortOrder): Int = when (order) {
    SortOrder.NameAsc -> R.string.home_sort_name_asc
    SortOrder.NameDesc -> R.string.home_sort_name_desc
    SortOrder.CreatedDesc -> R.string.home_sort_created_desc
    SortOrder.CreatedAsc -> R.string.home_sort_created_asc
}
