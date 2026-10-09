// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Card de projeto da Home (E2.1) e chip de tag associado. O card
// renderiza nome + descrição opcional + até `MAX_TAG_CHIPS_PREVIEW`
// chips de tag. O parser manual `#RRGGBB` evita `Color.parseColor`
// (que lança em hex inválido) — validação já passou no domínio.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoInfoChip
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoBorders

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

@Composable
internal fun ProjectCard(
    item: ProjectCardItem,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(HomeTestTags.PROJECT_CARD)
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item.tags.take(MAX_TAG_CHIPS_PREVIEW).forEach { chip ->
                        TagChipView(chip = chip, selected = true)
                    }
                }
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
 * Faz parse manual de cor no formato `#RRGGBB` para evitar
 * `Color(android.graphics.Color.parseColor(...))` que lança em
 * hex inválido — neste ponto a validação já passou pelo domínio.
 */
internal fun parseHexColor(hex: String): Color {
    val cleaned = hex.removePrefix("#")
    val value = cleaned.toLong(HEX_COLOR_BASE)
    val r = ((value shr HEX_COLOR_RED_SHIFT) and HEX_COLOR_CHANNEL_MASK).toInt()
    val g = ((value shr HEX_COLOR_GREEN_SHIFT) and HEX_COLOR_CHANNEL_MASK).toInt()
    val b = (value and HEX_COLOR_CHANNEL_MASK).toInt()
    return Color(red = r, green = g, blue = b)
}
