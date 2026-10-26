// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Identidade da conta pessoal conectada.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSpacing
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.neoTopAppBarColors
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

/** Saudação e monograma da conta conectada; sem sessão, mostra apenas a saudação genérica. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeTopBar(user: HomeUser?) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NeoSpacing.sm),
            ) {
                if (user != null) {
                    Box(
                        modifier =
                            Modifier
                                .size(40.dp * LocalDensity.current.fontScale.coerceAtLeast(1f))
                                .clearAndSetSemantics {}
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = user.initials,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Column {
                    Text(
                        text =
                            user?.let { stringResource(id = R.string.home_topbar_greeting, it.displayName) }
                                ?: stringResource(id = R.string.home_topbar_greeting_generic),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        },
        colors = neoTopAppBarColors(),
    )
}
