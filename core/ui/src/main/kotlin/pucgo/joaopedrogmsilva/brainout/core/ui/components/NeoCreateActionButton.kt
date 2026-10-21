// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.components

import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.ui.R
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.LocalNeoWindowInfo

/** Ação principal compacta quando espaço ou fonte pedem; nome acessível permanece. */
@Composable
fun NeoCreateActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    val window = LocalNeoWindowInfo.current
    ExtendedFloatingActionButton(
        onClick = onClick,
        expanded = window.widthDp >= 360.dp && !window.forceSingleColumn && LocalDensity.current.fontScale <= 1.3f,
        icon = { Icon(painterResource(R.drawable.neo_ic_add), contentDescription = null) },
        text = { Text(label, Modifier.clearAndSetSemantics {}) },
        containerColor = containerColor,
        contentColor = contentColor,
        modifier = modifier.semantics { contentDescription = label },
    )
}

@Preview
@Composable
private fun NeoCreateActionButtonPreview() {
    BrainOutTheme { NeoCreateActionButton("Criar projeto", {}) }
}
