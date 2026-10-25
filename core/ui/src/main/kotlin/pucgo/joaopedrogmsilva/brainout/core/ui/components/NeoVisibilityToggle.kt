// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import pucgo.joaopedrogmsilva.brainout.core.ui.R

/**
 * Botão que alterna a revelação de um campo de senha.
 *
 * A receita — ícone de olho, descrição que acompanha o estado e
 * `contentDescription` no botão (não só no ícone) — estava escrita
 * duas vezes: no campo de senha do login e no do backup. O
 * `:core:ui` é o dono das receitas visuais, então ela mora aqui uma
 * única vez.
 *
 * @param visible se a senha está revelada.
 * @param showLabel descrição do botão quando a senha está oculta.
 * @param hideLabel descrição do botão quando a senha está revelada.
 * @param onToggle disparado ao tocar o botão.
 * @param modifier modificador do botão — o `testTag` entra por aqui.
 */
@Composable
fun NeoVisibilityToggle(
    visible: Boolean,
    showLabel: String,
    hideLabel: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = if (visible) hideLabel else showLabel
    IconButton(
        onClick = onToggle,
        modifier = modifier.semantics { contentDescription = description },
    ) {
        Icon(
            painter =
                painterResource(
                    if (visible) R.drawable.neo_ic_visibility_off else R.drawable.neo_ic_visibility,
                ),
            contentDescription = description,
        )
    }
}
