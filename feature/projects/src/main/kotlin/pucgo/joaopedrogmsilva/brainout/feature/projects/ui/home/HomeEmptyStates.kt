// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Empty states da Home (E2.6/E2.5): lista vazia sem filtro, lista
// vazia de concluídos e busca/filtro sem resultados. Só apresentação;
// a escolha entre eles continua no orchestrator do conteúdo.
//
// O esqueleto do cartão (fundo, fronteira, espaçamento e alinhamento)
// vive em `NeoEmptyState`, no `:core:ui` — as cinco telas que mostram
// "estado vazio" (Home ×3, Tarefas e Painel) usavam a mesma marca.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoEmptyState
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

/**
 * Empty state quando há busca/filtro ativo mas a query não
 * retornou nada (E2.6). Diferencia do
 * [HomeCompletedEmptyState] / [HomeEmptyState] ao citar o termo
 * buscado para que o usuário saiba que o filtro é a razão da
 * lista vazia (não a ausência de projetos).
 */
@Composable
internal fun HomeNoMatchesState(
    query: String,
    modifier: Modifier = Modifier,
) {
    val title =
        if (query.isNotBlank()) {
            stringResource(id = R.string.home_no_matches_title_with_query, query)
        } else {
            stringResource(id = R.string.home_no_matches_title)
        }
    NeoEmptyState(
        title = title,
        body = stringResource(id = R.string.home_no_matches_body),
        modifier = modifier.testTag(HomeTestTags.NO_MATCHES),
    )
}

/**
 * Estado vazio da lista de projetos.
 *
 * Serve aos dois casos — "nenhum projeto" e "nenhum projeto concluído" —
 * que diferem apenas no texto. Os textos ficam na chamada; o cartão é o
 * mesmo.
 *
 * @param titleRes recurso do título.
 * @param bodyRes recurso do texto de apoio.
 */
@Composable
internal fun HomeEmptyState(
    titleRes: Int,
    bodyRes: Int,
    modifier: Modifier = Modifier,
) {
    NeoEmptyState(
        title = stringResource(id = titleRes),
        body = stringResource(id = bodyRes),
        modifier = modifier,
    )
}
