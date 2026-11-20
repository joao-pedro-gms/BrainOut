// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Seletor de cor de tag por amostras (swatches) da identidade Neo.
//
// A cor da tag (`Tag.color`, `:core:domain`) é uma string `#RRGGBB`
// livre. Oferecer um campo de texto para ela convidaria a valores fora
// da paleta e a erros de digitação; a amostra curada resolve os dois:
// o usuário escolhe entre doze cores já harmonizadas com os primitivos
// Neo e o valor gravado é sempre válido.
//
// Acessibilidade: cada amostra é um alvo de 48 dp (`NeoSizes.touchMin`)
// com papel de radio, `contentDescription` com o nome da cor e, quando
// selecionada, anel de foco do tema + `stateDescription`. O grupo inteiro
// segue a regra de seletores do `DESIGN.md` §6 (`selectable` + estado,
// nunca chip informativo com callback).

package pucgo.joaopedrogmsilva.brainout.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import pucgo.joaopedrogmsilva.brainout.core.ui.R
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.LocalNeoColors
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoBorders
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoRadii
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSizes
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSpacing
import android.graphics.Color as AndroidColor

/**
 * As doze cores de tag curadas, em formato `#RRGGBB`.
 *
 * Derivadas dos primitivos da paleta A (`NeoColor.kt`): os matizes de
 * ação (amarelo), marca (violeta), secundária (roxo), sucesso (verde/
 * menta), informação (azuis), aviso (laranja/âmbar) e erro (vermelho),
 * mais variantes saturadas (rosa e grafite) para cobrir contraste em
 * superfície clara e escura. A primeira entrada é também o fallback de
 * preenchimento quando um hex não pode ser interpretado.
 */
val NEO_TAG_COLORS: List<String> =
    listOf(
        "#B5A1F5",
        "#51368F",
        "#A51D2D",
        "#FFBA8C",
        "#F9D94A",
        "#665000",
        "#A8E6CF",
        "#165C47",
        "#A5D8FF",
        "#174F7B",
        "#FFB4B4",
        "#54544C",
    )

/**
 * Grade de amostras para escolher a cor de uma tag.
 *
 * Renderiza um `FlowRow` de caixas de [NeoSizes.touchMin] com cantos de
 * [NeoRadii.button], preenchidas com a cor de cada entrada de
 * [NEO_TAG_COLORS]. A amostra selecionada ganha um anel na cor de foco
 * do tema e anuncia `stateDescription`; cada amostra anuncia o nome da
 * cor como `contentDescription` e expõe o testTag
 * `neo_color_swatch_<hex sem #>`.
 *
 * O preenchimento é defensivo: um hex que [android.graphics.Color] não
 * consiga interpretar cai para a primeira cor da lista, em vez de
 * quebrar a composição.
 *
 * @param selected hex (`#RRGGBB`) atualmente selecionado, comparado sem
 *  distinção de maiúsculas.
 * @param onSelect callback com o hex exato da amostra tocada.
 * @param modifier mods do `FlowRow`.
 */
@Composable
fun NeoColorSwatches(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fallbackHex = NEO_TAG_COLORS.first()
    val selectedLabel = stringResource(R.string.neo_color_swatch_selected)
    val ringColor = LocalNeoColors.current.focus
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(NeoSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(NeoSpacing.xs),
    ) {
        NEO_TAG_COLORS.forEach { hex ->
            val fill =
                remember(hex) {
                    runCatching { Color(AndroidColor.parseColor(hex)) }
                        .getOrElse { Color(AndroidColor.parseColor(fallbackHex)) }
                }
            val name = stringResource(neoTagColorNameRes(hex))
            val isSelected = hex.equals(selected, ignoreCase = true)
            val shape = RoundedCornerShape(NeoRadii.button)
            Box(
                modifier =
                    Modifier
                        .size(NeoSizes.touchMin)
                        .clip(shape)
                        .background(fill)
                        .then(
                            if (isSelected) {
                                Modifier.border(BorderStroke(NeoBorders.focus, ringColor), shape)
                            } else {
                                Modifier
                            },
                        ).selectable(
                            selected = isSelected,
                            onClick = { onSelect(hex) },
                            role = Role.RadioButton,
                        ).semantics {
                            contentDescription = name
                            if (isSelected) {
                                stateDescription = selectedLabel
                            }
                        }.testTag(neoColorSwatchTag(hex)),
            )
        }
    }
}

/** TestTag estável da amostra de [hex]: `neo_color_swatch_<hex sem #>`. */
fun neoColorSwatchTag(hex: String): String = "neo_color_swatch_${hex.removePrefix("#")}"

private fun neoTagColorNameRes(hex: String): Int =
    when (hex.uppercase()) {
        "#B5A1F5" -> R.string.neo_tag_color_violet
        "#51368F" -> R.string.neo_tag_color_purple
        "#A51D2D" -> R.string.neo_tag_color_red
        "#FFBA8C" -> R.string.neo_tag_color_orange
        "#F9D94A" -> R.string.neo_tag_color_yellow
        "#665000" -> R.string.neo_tag_color_amber
        "#A8E6CF" -> R.string.neo_tag_color_mint
        "#165C47" -> R.string.neo_tag_color_green
        "#A5D8FF" -> R.string.neo_tag_color_light_blue
        "#174F7B" -> R.string.neo_tag_color_blue
        "#FFB4B4" -> R.string.neo_tag_color_pink
        else -> R.string.neo_tag_color_graphite
    }

@Preview(showBackground = true)
@Composable
private fun NeoColorSwatchesPreview() {
    BrainOutTheme {
        NeoColorSwatches(selected = NEO_TAG_COLORS[1], onSelect = {})
    }
}
