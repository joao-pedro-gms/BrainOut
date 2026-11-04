// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Diálogos de criação e edição de projeto com seleção de tags e o
// sub-diálogo `AddTagDialog`. Validação inline de nome obrigatório
// (E2.8) via chave canônica + `resolveNameError`.

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
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

/** Configuração que diferencia criar de editar sem duplicar o formulário. */
private data class ProjectFormConfig(
    val titleRes: Int,
    val confirmRes: Int,
    val testTagPrefix: String,
    val initialName: String,
    val initialDescription: String,
    val initialTagIds: List<String>,
)

/** Orquestrador de criação; a API e os identificadores existentes permanecem estáveis. */
@Composable
internal fun CreateProjectDialog(
    availableTags: List<TagChip>,
    onDismiss: () -> Unit,
    onCreateTag: (name: String, color: String) -> Unit,
    onCreateProject: (name: String, description: String?, tagIds: List<String>) -> Unit,
    errorMessage: String? = null,
) {
    ProjectFormDialog(
        config =
            ProjectFormConfig(
                titleRes = R.string.home_create_project_title,
                confirmRes = R.string.home_create_project_confirm,
                testTagPrefix = HomeTestTags.CREATE_PROJECT_FORM_PREFIX,
                initialName = "",
                initialDescription = "",
                initialTagIds = emptyList(),
            ),
        availableTags = availableTags,
        onDismiss = onDismiss,
        onCreateTag = onCreateTag,
        onSubmit = onCreateProject,
        errorMessage = errorMessage,
    )
}

/** Edição do projeto e de suas associações de tags, usando o formulário de criação. */
@Composable
internal fun EditProjectDialog(
    item: ProjectCardItem,
    availableTags: List<TagChip>,
    onDismiss: () -> Unit,
    onCreateTag: (name: String, color: String) -> Unit,
    onUpdateProject: (project: Project, tagIds: List<String>) -> Unit,
    errorMessage: String? = null,
) {
    ProjectFormDialog(
        config =
            ProjectFormConfig(
                titleRes = R.string.home_edit_project_title,
                confirmRes = R.string.home_edit_project_confirm,
                testTagPrefix = HomeTestTags.EDIT_PROJECT_FORM_PREFIX,
                initialName = item.project.name,
                initialDescription = item.project.description.orEmpty(),
                initialTagIds = item.tags.map { it.id },
            ),
        availableTags = availableTags,
        onDismiss = onDismiss,
        onCreateTag = onCreateTag,
        onSubmit = { name, description, tagIds ->
            onUpdateProject(
                item.project.copy(name = name, description = description),
                tagIds,
            )
        },
        errorMessage = errorMessage,
    )
}

@Composable
private fun rememberSelectedTagIds(initialTagIds: List<String>): SnapshotStateList<String> =
    rememberSaveable(
        saver =
            androidx.compose.runtime.saveable.listSaver(
                save = { it.toList() },
                restore = { it.toMutableStateList() },
            ),
    ) { initialTagIds.toMutableStateList() }

/** Campos, validação inline e criação de tag compartilhados por criar e editar. */
@Composable
private fun ProjectFormDialog(
    config: ProjectFormConfig,
    availableTags: List<TagChip>,
    onDismiss: () -> Unit,
    onCreateTag: (name: String, color: String) -> Unit,
    onSubmit: (name: String, description: String?, tagIds: List<String>) -> Unit,
    errorMessage: String? = null,
) {
    var name by rememberSaveable { mutableStateOf(config.initialName) }
    var description by rememberSaveable { mutableStateOf(config.initialDescription) }
    var nameError by rememberSaveable { mutableStateOf<HomeErrorType?>(null) }
    val selectedTagIds = rememberSelectedTagIds(config.initialTagIds)
    var showAddTag by rememberSaveable { mutableStateOf(false) }

    ProjectDialog(
        titleRes = config.titleRes,
        onDismiss = onDismiss,
        modifier = Modifier.testTag("${config.testTagPrefix}_dialog"),
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("${config.testTagPrefix}_error"),
                    )
                }

                CreateProjectDialogBody(
                    fields =
                        CreateProjectFields(
                            name = name,
                            nameError = nameError,
                            description = description,
                            // A leitura no dono do estado registra a dependência
                            // do snapshot e recompõe os chips ao marcar uma tag.
                            selectedTagIds = selectedTagIds.toList(),
                        ),
                    availableTags = availableTags,
                    callbacks =
                        CreateProjectCallbacks(
                            onNameChange = {
                                name = it
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
                    testTagPrefix = config.testTagPrefix,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedName = name.trim()
                    if (trimmedName.isEmpty()) {
                        nameError = HomeErrorType.NameRequired
                        return@TextButton
                    }
                    onSubmit(
                        trimmedName,
                        description.trim().takeIf { it.isNotEmpty() },
                        selectedTagIds.toList(),
                    )
                },
                modifier = Modifier.testTag("${config.testTagPrefix}_confirm"),
            ) {
                Text(text = stringResource(id = config.confirmRes))
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
private fun resolveNameError(error: HomeErrorType?): String? = error?.let { resolveHomeErrorMessage(it) }

@Composable
private fun CreateProjectDialogBody(
    fields: CreateProjectFields,
    availableTags: List<TagChip>,
    callbacks: CreateProjectCallbacks,
    testTagPrefix: String,
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
            modifier = Modifier.testTag("${testTagPrefix}_name"),
        )
        OutlinedTextField(
            value = fields.description,
            onValueChange = callbacks.onDescriptionChange,
            label = { Text(text = stringResource(id = R.string.home_create_project_description_label)) },
            singleLine = false,
            modifier = Modifier.testTag("${testTagPrefix}_description"),
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
                testTagPrefix = testTagPrefix,
            )
        }
        TextButton(
            onClick = callbacks.onShowAddTag,
            modifier = Modifier.testTag("${testTagPrefix}_add_tag"),
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
    testTagPrefix: String,
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
                        .testTag("${testTagPrefix}_tag_${chip.id}"),
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

@Preview(showBackground = true)
@Composable
private fun EditProjectDialogPreview() {
    val urgent = TagChip(id = "urgent", name = "Urgente", color = "#BA1A1A")
    val study = TagChip(id = "study", name = "Estudos", color = "#6750A4")
    BrainOutTheme {
        Surface {
            EditProjectDialog(
                item =
                    ProjectCardItem(
                        project =
                            Project.create(
                                name = "Projeto integrador",
                                description = "Entrega da etapa final",
                                ownerId = "preview",
                            ),
                        tags = listOf(urgent),
                    ),
                availableTags = listOf(urgent, study),
                onDismiss = {},
                onCreateTag = { _, _ -> },
                onUpdateProject = { _, _ -> },
            )
        }
    }
}
