// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Empty states da Home (E2.6/E2.5): lista vazia sem filtro, lista
// vazia de concluídos e busca/filtro sem resultados. Só apresentação;
// a escolha entre eles continua no orchestrator do conteúdo.
//
// DEF-22 — os três cartões ganharam **fronteira** em vez de(alpha).
// O preenchimento `surfaceVariant.copy(alpha = 0.4f)` dava 1.06:1 no
// claro e 1.11:1 no escuro: uma diferença invisível na tela. Tirar o
// alpha sozinho não resolve — `surfaceVariant` puro marca 1.15:1 /
// 1.33:1, porque o papel é uma *superfície alternativa*, quase da mesma
// luminância do fundo por desenho. O que separa a região do fundo é a
// **fronteira**: `outline` puro a 16.10:1 / 15.95:1. O texto interno
// (`onSurfaceVariant`) fica em 6.56:1 / 9.26:1, acima de 4.5:1.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoBorders
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
    Surface(
        modifier = modifier.testTag(HomeTestTags.NO_MATCHES),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(NeoBorders.default, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text =
                    if (query.isNotBlank()) {
                        stringResource(id = R.string.home_no_matches_title_with_query, query)
                    } else {
                        stringResource(id = R.string.home_no_matches_title)
                    },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.home_no_matches_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun HomeCompletedEmptyState(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(NeoBorders.default, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.home_completed_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.home_completed_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun HomeEmptyState(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(NeoBorders.default, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.home_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.home_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
