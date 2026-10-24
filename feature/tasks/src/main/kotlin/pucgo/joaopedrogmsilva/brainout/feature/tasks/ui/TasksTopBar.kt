// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// TopAppBar das abas "Tarefas" e "Painel" (DEF-22).
//
// Antes estas telas eram becos sem saída: nenhum Scaffold, nenhum
// TopAppBar, nenhum botão de voltar — o usuário chegava pelo icon da
// bottom bar da Home e, sem o gesto do sistema, ficava preso na tela.
// O `Scaffold` aqui devolve título + voltar + a barra de status respeitada.

package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoBackButton
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.neoTopAppBarColors

/**
 * Barra superior das abas globais com `navigationIcon` de voltar.
 *
 * O rótulo "Voltar" é announced pelo TalkBack via `neo_back` (do
 * `:core:ui`), e a linha é exposta como nó de navegação
 * (`navigationIcon` do
 * `TopAppBar`) — é isso que faz o gesto do sistema e o botão
 * anunciarem a mesma ação.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TasksTopBar(
    titleRes: Int,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = stringResource(id = titleRes),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        navigationIcon = {
            NeoBackButton(
                onClick = onBackClicked,
                modifier = Modifier.testTag(TasksTestTags.BACK_BUTTON),
            )
        },
        colors =
            neoTopAppBarColors(
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            ),
    )
}
