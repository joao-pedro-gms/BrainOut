// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
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

/**
 * A página Neo: o conteúdo na largura máxima da identidade, com a
 * margem lateral da janela e o respiro vertical.
 *
 * A receita estava escrita em seis telas — [neoContentWidth] seguido do
 * mesmo `padding(horizontal = margin, vertical = NeoSpacing.sm)` — e em
 * uma delas o `12.dp` do respiro estava escrito à mão, em vez do token.
 *
 * @param margin a margem lateral da janela. Fica no chamador porque a
 *  CompositionLocal só pode ser lida de um contexto `@Composable`, e
 *  este modifier não é um.
 * @param vertical o respiro vertical. A lista de tarefas não usa
 *  respiro — ela tem o próprio espaçamento entre linhas — e por isso
 *  o valor é explícito lá.
 * @param form `true` limita o conteúdo à largura de formulário
 *  (480 dp) em vez da de conteúdo (1200 dp).
 */
fun Modifier.neoPageGutter(
    margin: Dp,
    vertical: Dp = NeoSpacing.sm,
    form: Boolean = false,
): Modifier = neoContentWidth(form = form).padding(horizontal = margin, vertical = vertical)
