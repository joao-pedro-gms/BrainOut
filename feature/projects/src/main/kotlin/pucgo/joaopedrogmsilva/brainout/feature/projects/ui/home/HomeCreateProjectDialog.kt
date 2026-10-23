// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Diálogo de criação de projeto (E2.1) com seleção de tags e o
// sub-diálogo `AddTagDialog`. Validação inline de nome obrigatório
// (E2.8) via chave canônica + `resolveNameError`.
//
// `LongMethod` no orchestrator: o corpo é a sequência de campos do
// formulário (nome, descrição, tags) mais a validação inline. Extrair
// cada campo em sua própria função espalharia o estado entre chamadas
// — a coesão do formulário vale mais que a métrica.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSizes
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSpacing
import pucgo.joaopedrogmsilva.brainout.feature.projects.R
import pucgo.joaopedrogmsilva.brainout.feature.projects.ui.ProjectDialog

/**
 * O que o usuário preencheu no diálogo de novo projeto.
 *
 * Agrupar os campos num objeto tira o [CreateProjectDialogBody] da
 * lista de parâmetros: os valores e as ações chegam separados, e cada
 * um nomeia o que é.
 */
internal data class CreateProjectFields(
    val name: String,
    val nameError: HomeErrorType?,
    val description: String,
    val selectedTagIds: List<String>,
)

/** Os callbacks do diálogo de novo projeto. */
internal data class CreateProjectCallbacks(
    val onNameChange: (String) -> Unit,
    val onDescriptionChange: (String) -> Unit,
    val onTagToggle: (String) -> Unit,
    val onShowAddTag: () -> Unit,
)

/**
 * Orquestrador: os campos do formulário, a validação inline e os dois
 * diálogos.
 */
@Composable
internal fun CreateProjectDialog(
    availableTags: List<TagChip>,
    onDismiss: () -> Unit,
    onCreateTag: (name: String, color: String) -> Unit,
    onCreateProject: (name: String, description: String?, tagIds: List<String>) -> Unit,
    errorMessage: String? = null,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var nameError by rememberSaveable { mutableStateOf<HomeErrorType?>(null) }
    val selectedTagIds =
        rememberSaveable(
            saver =
                androidx.compose.runtime.saveable.listSaver(
                    save = { it.toList() },
                    restore = { it.toMutableStateList() },
                ),
        ) { mutableStateListOf<String>() }
    var showAddTag by rememberSaveable { mutableStateOf(false) }

    ProjectDialog(
        titleRes = R.string.home_create_project_title,
        onDismiss = onDismiss,
        modifier = Modifier.testTag(HomeTestTags.CREATE_PROJECT_DIALOG),
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (errorMessage != null) Text(errorMessage, color = MaterialTheme.colorScheme.error)

                CreateProjectDialogBody(
                    fields =
                        CreateProjectFields(
                            name = name,
                            nameError = nameError,
                            description = description,
                            // `toList()` materializa a lista **no escopo do
                            // diálogo**: é essa leitura que registra a
                            // dependência do snapshot, de modo que marcar uma
                            // tag recomponha os chips. As funções abaixo recebem
                            // só `List<String>` + um callback — o tipo de
                            // snapshot do Compose não vaza para a assinatura
                            // (P2 da auditoria de 24/09/2026).
                            selectedTagIds = selectedTagIds.toList(),
                        ),
                    availableTags = availableTags,
                    callbacks =
                        CreateProjectCallbacks(
                            onNameChange = {
                                name = it
                                // E2.8 — limpa o erro assim que o usuário
                                // começa a digitar, evitando mensagem fixa no
                                // campo enquanto a interação continua.
                                if (nameError != null) nameError = null
                            },
                            onDescriptionChange = { description = it },
                            onTagToggle = { tagId ->
                                if (selectedTagIds.contains(tagId)) {
                                    selectedTagIds.remove(tagId)
                                } else {
                                    selectedTagIds.add(tagId)
                                }
                            },
                            onShowAddTag = { showAddTag = true },
                        ),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedName = name.trim()
                    if (trimmedName.isEmpty()) {
                        // E2.8 — validação inline: não fecha o
                        // diálogo, marca `nameError` para o
                        // `TextField` exibir `isError` +
                        // `supportingText`.
                        nameError = HomeErrorType.NameRequired
                        return@TextButton
                    }
                    onCreateProject(
                        trimmedName,
                        description.trim().takeIf { it.isNotEmpty() },
                        selectedTagIds.toList(),
                    )
                },
                modifier = Modifier.testTag(HomeTestTags.CREATE_PROJECT_CONFIRM),
            ) {
                Text(text = stringResource(id = R.string.home_create_project_confirm))
            }
        },
    )

    if (showAddTag) {
        AddTagDialog(
            onDismiss = { showAddTag = false },
            onCreate = { tagName, color ->
                onCreateTag(tagName, color)
                showAddTag = false
            },
        )
    }
}

@Composable
internal fun resolveNameError(error: HomeErrorType?): String? = error?.let { resolveHomeErrorMessage(it) }

@Composable
private fun CreateProjectDialogBody(
    fields: CreateProjectFields,
    availableTags: List<TagChip>,
    callbacks: CreateProjectCallbacks,
) {
    Column(verticalArrangement = Arrangement.spacedBy(NeoSpacing.sm)) {
        // E2.8 — `isError` + `supportingText` quando `nameError`
        // estiver preenchido. A mensagem é resolvida via
        // [resolveNameError] para manter a string
        // localizada em pt/en (regra E4.6).
        OutlinedTextField(
            value = fields.name,
            onValueChange = callbacks.onNameChange,
            label = { Text(text = stringResource(id = R.string.home_create_project_name_label)) },
            singleLine = true,
            isError = fields.nameError != null,
            supportingText = {
                val resolved = resolveNameError(fields.nameError)
                if (resolved != null) {
                    Text(
                        text = resolved,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            modifier = Modifier.testTag(HomeTestTags.CREATE_PROJECT_NAME),
        )
        OutlinedTextField(
            value = fields.description,
            onValueChange = callbacks.onDescriptionChange,
            label = { Text(text = stringResource(id = R.string.home_create_project_description_label)) },
            singleLine = false,
            modifier = Modifier.testTag(HomeTestTags.CREATE_PROJECT_DESCRIPTION),
        )
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
            CreateProjectTagsFlow(
                availableTags = availableTags,
                selectedTagIds = fields.selectedTagIds,
                onTagToggle = callbacks.onTagToggle,
            )
        }
        TextButton(
            onClick = callbacks.onShowAddTag,
            modifier = Modifier.testTag(HomeTestTags.CREATE_PROJECT_ADD_TAG),
        ) {
            Text(text = stringResource(id = R.string.home_create_project_add_tag))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateProjectTagsFlow(
    availableTags: List<TagChip>,
    selectedTagIds: List<String>,
    onTagToggle: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(NeoSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(NeoSpacing.xxs),
    ) {
        availableTags.forEach { chip ->
            val isSelected = selectedTagIds.contains(chip.id)
            FilterChip(
                selected = isSelected,
                onClick = { onTagToggle(chip.id) },
                label = { Text(text = chip.name) },
                // E4.4: FilterChip padrão M3 tem ~32dp de altura.
                // Aplicamos 48dp para satisfazer WCAG 2.5.5.
                modifier =
                    Modifier
                        .heightIn(min = NeoSizes.touchMin)
                        .testTag("${HomeTestTags.CREATE_PROJECT_TAG}_${chip.id}"),
            )
        }
    }
}

@Composable
private fun AddTagDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, color: String) -> Unit,
) {
    var tagName by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf("#6750A4") }

    ProjectDialog(
        titleRes = R.string.home_create_project_add_tag_title,
        onDismiss = onDismiss,
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(NeoSpacing.sm),
            ) {
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text(text = stringResource(id = R.string.home_create_project_add_tag_name_label)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text(text = stringResource(id = R.string.home_create_project_add_tag_color_label)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmedName = tagName.trim()
                if (trimmedName.isEmpty()) return@TextButton
                onCreate(trimmedName, color.trim())
            }) {
                Text(text = stringResource(id = R.string.home_create_project_confirm))
            }
        },
    )
}
