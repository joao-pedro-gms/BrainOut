// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Superfície de gerenciamento de tags do formulário de projeto.
//
// Substitui o antigo `AddTagDialog` (dois campos de texto livre, nome +
// hex digitado): as tags existentes aparecem primeiro como chips
// selecionáveis com a cor em um ponto — o caminho comum é marcar uma
// tag que já existe, não criar outra. Digitar só é necessário na seção
// "Nova tag", onde a cor sai de `NeoColorSwatches` (doze amostras
// curadas), nunca de um hex livre.
//
// Contrato preservado: os chips usam o testTag `"${testTagPrefix}_tag_<id>"`
// e o toggle inclui/remove o id na lista que o formulário envia — os
// testes antigos de criar/editar projeto dependem exatamente disso.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NEO_TAG_COLORS
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoColorSwatches
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.LocalNeoColors
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoBorders
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoRadii
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSizes
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSpacing
import pucgo.joaopedrogmsilva.brainout.feature.projects.R
import pucgo.joaopedrogmsilva.brainout.feature.projects.ui.ProjectDialog

/** Limite de caracteres do nome de uma nova tag, alinhado ao da conta. */
private const val TAG_NAME_MAX_LENGTH = 30

/**
 * Diálogo de gerenciamento de tags aberto pelo botão "Adicionar tag".
 *
 * Lista [availableTags] primeiro, como chips selecionáveis que alternam
 * a presença do id em [selectedTagIds] via [onTagToggle]; em seguida, a
 * seção "Nova tag" cria uma tag com nome validado e cor escolhida nas
 * amostras de [NeoColorSwatches] — o campo de hex livre não existe mais.
 *
 * @param testTagPrefix prefixo do formulário hospedeiro; os chips mantêm
 *  o testTag `"${testTagPrefix}_tag_<id>"` e os campos novos usam
 *  `"${testTagPrefix}_tag_manage_*"`.
 */
@Composable
internal fun TagManageDialog(
    availableTags: List<TagChip>,
    selectedTagIds: List<String>,
    onTagToggle: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreate: (name: String, color: String) -> Unit,
    testTagPrefix: String,
) {
    var newName by rememberSaveable { mutableStateOf("") }
    var newColor by rememberSaveable { mutableStateOf(NEO_TAG_COLORS.first()) }
    var nameRequired by rememberSaveable { mutableStateOf(false) }

    ProjectDialog(
        titleRes = R.string.home_tag_manage_title,
        onDismiss = onDismiss,
        modifier = Modifier.testTag("${testTagPrefix}_tag_manage_dialog"),
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(NeoSpacing.sm),
            ) {
                Text(
                    text = stringResource(id = R.string.home_create_project_tags_label),
                    style = MaterialTheme.typography.labelMedium,
                )
                if (availableTags.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.home_create_project_tags_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    TagManageChipsFlow(
                        availableTags = availableTags,
                        selectedTagIds = selectedTagIds,
                        onTagToggle = onTagToggle,
                        testTagPrefix = testTagPrefix,
                    )
                }
                Text(
                    text = stringResource(id = R.string.home_create_project_add_tag_title),
                    style = MaterialTheme.typography.labelMedium,
                )
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it.take(TAG_NAME_MAX_LENGTH)
                        if (nameRequired) nameRequired = false
                    },
                    label = {
                        Text(text = stringResource(id = R.string.home_create_project_add_tag_name_label))
                    },
                    singleLine = true,
                    isError = nameRequired,
                    supportingText = {
                        if (nameRequired) {
                            Text(
                                text = stringResource(id = R.string.home_tag_manage_name_required),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                    modifier = Modifier.testTag("${testTagPrefix}_tag_manage_name"),
                )
                Text(
                    text = stringResource(id = R.string.home_tag_manage_color_label),
                    style = MaterialTheme.typography.labelMedium,
                )
                NeoColorSwatches(selected = newColor, onSelect = { newColor = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedName = newName.trim()
                    if (trimmedName.isEmpty()) {
                        nameRequired = true
                        return@TextButton
                    }
                    onCreate(trimmedName, newColor)
                },
                // E4.4: 48dp mínimo para área de toque (WCAG 2.5.5) —
                // o `TextButton` padrão do M3 marca 40dp.
                modifier =
                    Modifier
                        .heightIn(min = NeoSizes.touchMin)
                        .testTag("${testTagPrefix}_tag_manage_confirm"),
            ) {
                Text(text = stringResource(id = R.string.home_create_project_confirm))
            }
        },
    )
}

@Composable
private fun TagManageChipsFlow(
    availableTags: List<TagChip>,
    selectedTagIds: List<String>,
    onTagToggle: (String) -> Unit,
    testTagPrefix: String,
) {
    val selectedLabel = stringResource(id = R.string.home_tag_manage_tag_selected)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(NeoSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(NeoSpacing.xxs),
    ) {
        availableTags.forEach { chip ->
            TagManageChip(
                chip = chip,
                selected = selectedTagIds.contains(chip.id),
                selectedLabel = selectedLabel,
                onToggle = { onTagToggle(chip.id) },
                modifier = Modifier.testTag("${testTagPrefix}_tag_${chip.id}"),
            )
        }
    }
}

/**
 * Chip de tag existente: ponto com a cor da tag (o mesmo parse de
 * [parseHexColor] do card de projeto) + nome, 48 dp de alvo e anel de
 * foco quando selecionado, com `stateDescription` para o TalkBack.
 */
@Composable
private fun TagManageChip(
    chip: TagChip,
    selected: Boolean,
    selectedLabel: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val neoColors = LocalNeoColors.current
    val dotColor = remember(chip.color) { parseHexColor(chip.color) }
    Row(
        modifier =
            modifier
                .heightIn(min = NeoSizes.touchMin)
                .clip(RoundedCornerShape(NeoRadii.chip))
                .background(if (selected) neoColors.surfaceAlt else neoColors.surface)
                .border(
                    BorderStroke(
                        width = if (selected) NeoBorders.focus else NeoBorders.subtle,
                        color = if (selected) neoColors.focus else neoColors.borderDefault,
                    ),
                    RoundedCornerShape(NeoRadii.chip),
                ).selectable(
                    selected = selected,
                    onClick = onToggle,
                    role = Role.Checkbox,
                ).semantics {
                    if (selected) {
                        stateDescription = selectedLabel
                    }
                }.padding(horizontal = NeoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NeoSpacing.xs),
    ) {
        Box(
            modifier =
                Modifier
                    .size(NeoSpacing.sm)
                    .clip(CircleShape)
                    .background(dotColor),
        )
        Text(
            text = chip.name,
            style = MaterialTheme.typography.labelLarge,
            color = neoColors.textPrimary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TagManageDialogPreview() {
    val tags =
        listOf(
            TagChip(id = "urgent", name = "Urgente", color = "#A51D2D"),
            TagChip(id = "study", name = "Estudos", color = "#174F7B"),
            TagChip(id = "home", name = "Casa", color = "#165C47"),
        )
    BrainOutTheme {
        Surface {
            TagManageDialog(
                availableTags = tags,
                selectedTagIds = listOf("urgent"),
                onTagToggle = {},
                onDismiss = {},
                onCreate = { _, _ -> },
                testTagPrefix = "preview",
            )
        }
    }
}
