// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Ação de criar projeto para a conta pessoal conectada.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoCreateActionButton
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

@Composable
internal fun HomeFloatingActionButton(onCreateProjectClicked: () -> Unit) {
    NeoCreateActionButton(
        label = stringResource(R.string.home_fab_create),
        onClick = onCreateProjectClicked,
        containerColor =
            MaterialTheme.colorScheme.primaryContainer,
        contentColor =
            MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.testTag(HomeTestTags.FAB),
    )
}
