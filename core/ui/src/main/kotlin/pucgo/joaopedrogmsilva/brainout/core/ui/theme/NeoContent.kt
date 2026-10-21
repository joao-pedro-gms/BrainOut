// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/** Dimensões da janela atual, atualizadas também em multiwindow e rotação. */
@Composable
fun NeoWindowProvider(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val info = NeoWindowInfo.fromSize(configuration.screenWidthDp.dp, configuration.screenHeightDp.dp)
    CompositionLocalProvider(LocalNeoWindowInfo provides info, content = content)
}

fun Modifier.neoContentWidth(form: Boolean = false): Modifier =
    wrapContentWidth().widthIn(max = if (form) NeoLayout.formMaxWidth else NeoLayout.contentMaxWidth).fillMaxWidth()
