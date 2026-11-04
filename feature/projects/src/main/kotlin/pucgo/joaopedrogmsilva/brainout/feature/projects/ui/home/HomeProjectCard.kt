// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Card de projeto da Home (E2.1) e chip de tag associado. O card
// renderiza nome + descrição opcional + até `MAX_TAG_CHIPS_PREVIEW`
// chips de tag. O parser manual `#RRGGBB` evita `Color.parseColor`
// (que lança em hex inválido) — validação já passou no domínio.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoInfoChip
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoBorders
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoRadii
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSizes
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSpacing
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.neoPressFeedback
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

internal const val MAX_TAG_CHIPS_PREVIEW: Int = 4

/**
 * Constantes para o parser de cor no formato `#RRGGBB` — mantidas
 * no nível de arquivo para serem reutilizadas e ficarem fora do
 * detekt MagicNumber.
 */
private const val HEX_COLOR_BASE: Int = 16
private const val HEX_COLOR_RED_SHIFT: Int = 16
private const val HEX_COLOR_GREEN_SHIFT: Int = 8
private const val HEX_COLOR_CHANNEL_MASK: Long = 0xFFL

/** Cor usada quando o valor não é um `#RRGGBB` válido. */
private val HEX_COLOR_FALLBACK: Color = Color(0xFF9E9E9E)

@Composable
internal fun ProjectCard(
    item: ProjectCardItem,
    onClick: () -> Unit,
    onEditClick: () -> Unit = {},
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(HomeTestTags.PROJECT_CARD)
                .clickable(
                    interactionSource = interaction,
                    indication = androidx.compose.foundation.LocalIndication.current,
                    onClick = onClick,
                ).neoPressFeedback(interaction),
        shape = RoundedCornerShape(NeoRadii.card),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(NeoSpacing.md),
            verticalArrangement = Arrangement.spacedBy(NeoSpacing.xs),
        ) {
            Text(
                text = item.project.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            item.project.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(NeoSpacing.xs),
                ) {
                    item.tags.take(MAX_TAG_CHIPS_PREVIEW).forEach { chip ->
                        TagChipView(chip = chip, selected = true)
                    }
                }
            }
            TextButton(
                onClick = onEditClick,
                modifier =
                    Modifier
                        .align(Alignment.End)
                        .heightIn(min = NeoSizes.touchMin)
                        .testTag("${HomeTestTags.EDIT_PROJECT_BUTTON}_${item.project.id}"),
            ) {
                Text(text = stringResource(id = R.string.home_edit_project_action))
            }
        }
    }
}

@Composable
internal fun TagChipView(
    chip: TagChip,
    selected: Boolean,
) {
    val container = remember(chip.color) { parseHexColor(chip.color) }
    // DEF-22 — era `AssistChip(onClick = { })` **dentro de um card
    // clicável**. O alvo de toque do chip interceptava o toque do
    // usuário e não abria o projeto: o clique morria num callback
    // vazio. `NeoInfoChip` não tem área interativa, então o toque
    // chega ao `.clickable` do `ProjectCard`.
    NeoInfoChip(
        label = chip.name,
        containerColor =
            if (selected) {
                container.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        labelColor = MaterialTheme.colorScheme.onSurface,
        borderColor = container,
        borderWidth = NeoBorders.subtle,
    )
}

/**
 * Faz parse manual de cor no formato `#RRGGBB`.
 *
 * Diferente de `Color(android.graphics.Color.parseColor(...))`, que
 * lança em hex inválido, esta função **não** lança: um valor fora do
 * formato devolve o cinza de fallback. Na prática o domínio já valida
 * a cor (`^#[0-9A-Fa-f]{6}$`) antes de chegar aqui, então o fallback é
 * apenas uma rede de segurança para a UI não quebrar.
 */
internal fun parseHexColor(hex: String): Color {
    val cleaned = hex.removePrefix("#")
    val value = cleaned.toLongOrNull(HEX_COLOR_BASE) ?: return HEX_COLOR_FALLBACK
    val r = ((value shr HEX_COLOR_RED_SHIFT) and HEX_COLOR_CHANNEL_MASK).toInt()
    val g = ((value shr HEX_COLOR_GREEN_SHIFT) and HEX_COLOR_CHANNEL_MASK).toInt()
    val b = (value and HEX_COLOR_CHANNEL_MASK).toInt()
    return Color(red = r, green = g, blue = b)
}
