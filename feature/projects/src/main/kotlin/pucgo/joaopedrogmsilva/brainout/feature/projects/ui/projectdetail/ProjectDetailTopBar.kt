// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// TopAppBar do detalhe do projeto (E2.2): título + voltar + excluir.
// Extraído de ProjectDetailScreen.kt para manter o orquestrador ≤200 LoC.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.neoTopAppBarColors
import pucgo.joaopedrogmsilva.brainout.feature.projects.R
import pucgo.joaopedrogmsilva.brainout.core.ui.R as UiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProjectDetailTopBar(
    onBackClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.project_detail_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClicked) {
                Icon(
                    painter = painterResource(UiR.drawable.neo_ic_arrow_back),
                    contentDescription = stringResource(id = R.string.common_back),
                )
            }
        },
        actions = {
            IconButton(
                onClick = onDeleteClicked,
                modifier = Modifier.testTag(ProjectDetailTestTags.DELETE_PROJECT_BUTTON),
            ) {
                Icon(
                    painter = painterResource(UiR.drawable.neo_ic_delete),
                    contentDescription = stringResource(id = R.string.project_detail_delete_project),
                )
            }
        },
        colors =
            neoTopAppBarColors(
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground,
            ),
    )
}
