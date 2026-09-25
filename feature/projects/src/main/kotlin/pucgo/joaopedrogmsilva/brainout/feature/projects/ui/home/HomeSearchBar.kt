// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Busca textual e filtro por tag da Home (E2.6). A barra dispara o
// callback a cada keystroke para feedback imediato; o debounce de
// I/O vive no ViewModel. A linha de tags usa `LazyRow` para escalar
// quando o usuário tem muitas tags.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

/**
 * Chave estável do chip "Todas" no [HomeTagFilterRow]. Mantida como
 * constante no escopo do arquivo (em vez de string literal inline)
 * para que o `LazyRow` possa referenciar a mesma `key` na hora do
 * recompose e não destrua/recrie o chip quando o `availableTags`
 * muda.
 */
private const val TAG_FILTER_ALL_KEY: String = "__all__"

/**
 * Barra de busca textual (E2.6). `OutlinedTextField` com placeholder
 * localizado; ícone de limpar (X) surge apenas quando o campo tem
 * texto para reduzir ruído visual. O debounce de I/O é aplicado no
 * ViewModel — esta composable dispara o callback em cada keystroke
 * para feedback visual imediato do campo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(text = stringResource(id = R.string.home_search_placeholder)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Sort,
                contentDescription = null,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    // E4.4: 48dp mínimo para área de toque (WCAG 2.5.5).
                    modifier = Modifier
                        .testTag(HomeTestTags.SEARCH_CLEAR)
                        .heightIn(min = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = stringResource(id = R.string.home_search_clear),
                    )
                }
            }
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.SEARCH_FIELD),
    )
}

/**
 * Linha de chips para filtro por tag (E2.6). Primeiro chip é
 * "Todas" (`null` no filtro) e em seguida cada tag do owner.
 * `LazyRow` para escalar quando o usuário tem dezenas de tags.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeTagFilterRow(
    availableTags: List<TagChip>,
    selectedTagId: String?,
    onSelect: (String?) -> Unit,
) {
    if (availableTags.isEmpty()) return
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.TAG_FILTER_ROW),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        item(key = TAG_FILTER_ALL_KEY) {
            FilterChip(
                selected = selectedTagId == null,
                onClick = { onSelect(null) },
                label = {
                    Text(text = stringResource(id = R.string.home_tag_filter_all))
                },
                modifier = Modifier
                    .testTag(HomeTestTags.TAG_FILTER_ALL)
                    // E4.4: 48dp mínimo WCAG 2.5.5.
                    .heightIn(min = 48.dp),
            )
        }
        items(items = availableTags, key = { it.id }) { chip ->
            FilterChip(
                selected = chip.id == selectedTagId,
                onClick = {
                    onSelect(if (chip.id == selectedTagId) null else chip.id)
                },
                label = { Text(text = chip.name) },
                modifier = Modifier
                    .testTag(HomeTestTags.tagFilterChip(chip.id))
                    // E4.4: 48dp mínimo WCAG 2.5.5.
                    .heightIn(min = 48.dp),
            )
        }
    }
}
