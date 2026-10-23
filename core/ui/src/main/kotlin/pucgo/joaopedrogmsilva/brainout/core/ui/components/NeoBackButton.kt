// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import pucgo.joaopedrogmsilva.brainout.core.ui.R
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme

/**
 * O botão de voltar da barra superior Neo.
 *
 * A receita — o ícone de seta e o rótulo "Voltar", anunciado pelo
 * TalkBack — estava copiada em duas features: o detalhe do projeto e a
 * lista de tarefas. Aqui ela mora uma vez, junto das outras receitas da
 * identidade.
 *
 * O rótulo é o `neo_back` do `:core:ui`: era `common_back`, declarado
 * **duas vezes** — neste módulo e no de tarefas — com o mesmo texto.
 */
@Composable
fun NeoBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(R.drawable.neo_ic_arrow_back),
            contentDescription = stringResource(id = R.string.neo_back),
        )
    }
}

@Preview
@Composable
private fun NeoBackButtonPreview() {
    BrainOutTheme { NeoBackButton(onClick = {}) }
}
