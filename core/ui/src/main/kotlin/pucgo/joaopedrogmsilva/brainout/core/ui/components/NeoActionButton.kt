// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.LocalNeoColors
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoBorders
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoRadii
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSizes

/**
 * Variação de cor do bloco de ação.
 *
 * [Primary] é o bloco amarelo da identidade — confirmações de diálogo,
 * submissão de formulário. [Danger] é o bloco vermelho reservado à única
 * ação destrutiva do fluxo (excluir), que antes aparecia só como tinta de
 * texto e era a única coisa colorida num diálogo monocromático.
 */
enum class NeoActionVariant {
    Primary,
    Danger,
}

/**
 * O botão de ação Neo: bloco preenchido, nunca texto solto.
 *
 * A identidade declara `actionBackground`/`actionText` como o par da ação
 * principal, mas antes deste componente só o FAB o usava — todo
 * `Button`/`TextButton` default caía em `primary = link`, um texto ocre
 * sobre o fundo da tela. WCAG 2.4.7/1.4.11 e a própria gramática visual
 * (FAB, barra de ação) pedem área preenchida com borda forte, então a
 * receita vive aqui: raio [NeoRadii.button], borda [NeoBorders.default]
 * em `borderOnAccent` e altura mínima [NeoSizes.buttonMinHeight].
 *
 * Desabilitado usa os papéis `disabledBackground`/`disabledText` da
 * identidade — não alfa — porque a lavagem alfa do Material some com o
 * contraste no amarelo.
 */
@Composable
fun NeoActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NeoActionVariant = NeoActionVariant.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    loadingTestTag: String? = null,
) {
    val neoColors = LocalNeoColors.current
    val filled = enabled && variant == NeoActionVariant.Primary
    val dangerous = enabled && variant == NeoActionVariant.Danger
    val containerColor =
        when {
            filled -> neoColors.actionBackground
            dangerous -> neoColors.error
            else -> neoColors.disabledBackground
        }
    val contentColor =
        when {
            filled -> neoColors.actionText
            dangerous -> neoColors.onError
            else -> neoColors.disabledText
        }
    val borderColor = if (enabled) neoColors.borderOnAccent else neoColors.disabledText
    val shape = RoundedCornerShape(NeoRadii.button)
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = shape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = containerColor,
                disabledContentColor = contentColor,
            ),
        modifier =
            modifier
                .heightIn(min = NeoSizes.buttonMinHeight)
                .border(NeoBorders.default, borderColor, shape),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier =
                    Modifier
                        .height(NeoSizes.iconMeta)
                        .then(if (loadingTestTag != null) Modifier.testTag(loadingTestTag) else Modifier),
                strokeWidth = NeoBorders.default,
                color = contentColor,
            )
        } else {
            Text(text)
        }
    }
}

@Preview
@Composable
private fun NeoActionButtonPreview() {
    BrainOutTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(NeoBorders.focusGap)) {
            NeoActionButton("Salvar", {})
            NeoActionButton("Excluir", {}, variant = NeoActionVariant.Danger)
            NeoActionButton("Salvando", {}, enabled = false)
        }
    }
}

@Preview
@Composable
private fun NeoActionButtonDarkPreview() {
    BrainOutTheme(darkTheme = true) {
        Row(horizontalArrangement = Arrangement.spacedBy(NeoBorders.focusGap)) {
            NeoActionButton("Salvar", {})
            NeoActionButton("Excluir", {}, variant = NeoActionVariant.Danger)
        }
    }
}
