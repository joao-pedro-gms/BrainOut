// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Diálogo de criação de projeto (E2.1) com seleção de tags e o
// sub-diálogo `AddTagDialog`. Validação inline de nome obrigatório
// (E2.8) via chave canônica + `resolveCreateProjectNameError`.
//
// `LongParameterList` é necessário: o orchestrator passa adiante
// callbacks do ViewModel para composição + validação + criação;
// manter a superfície pública fina facilita a chamada pelo pai.

@file:Suppress("LongParameterList", "LongMethod")

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalLayoutApi
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

/**
 * Chave canônica de validação inline (E2.8). Mesma estratégia do
 * `ERROR_LOAD_FAILED` — a UI resolve para o recurso localizado via
 * [resolveCreateProjectNameError]. Permanece como constante para
 * que os testes Compose possam comparar com a chave sem depender de
 * texto em pt/en.
 */
private const val CREATE_PROJECT_NAME_REQUIRED_KEY: String = "CREATE_PROJECT_NAME_REQUIRED"

@Composable
internal fun CreateProjectDialog(
    availableTags: List<TagChip>,
    onDismiss: () -> Unit,
    onCreateTag: (name: String, color: String) -> Unit,
    onCreateProject: (name: String, description: String?, tagIds: List<String>) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedTagIds = rememberSaveable(saver = androidx.compose.runtime.saveable.listSaver(
        save = { it.toList() },
        restore = { it.toMutableStateList() },
    )) { mutableStateListOf<String>() }
    var showAddTag by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.home_create_project_title)) },
        text = {
            CreateProjectDialogBody(
                name = name,
                onNameChange = {
                    name = it
                    // E2.8 — limpa o erro assim que o usuário
                    // começa a digitar, evitando mensagem fixa no
                    // campo enquanto a interação continua.
                    if (nameError != null) nameError = null
                },
                nameError = nameError,
                description = description,
                onDescriptionChange = { description = it },
                availableTags = availableTags,
                selectedTagIds = selectedTagIds,
                onShowAddTag = { showAddTag = true },
            )
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
                        nameError = CREATE_PROJECT_NAME_REQUIRED_KEY
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
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.common_close))
            }
        },
        modifier = Modifier.testTag(HomeTestTags.CREATE_PROJECT_DIALOG),
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
internal fun resolveCreateProjectNameError(key: String?): String? = when (key) {
    CREATE_PROJECT_NAME_REQUIRED_KEY ->
        stringResource(id = R.string.home_create_project_name_required)
    null -> null
    else -> key
}

@Composable
private fun CreateProjectDialogBody(
    name: String,
    onNameChange: (String) -> Unit,
    nameError: String?,
    description: String,
    onDescriptionChange: (String) -> Unit,
    availableTags: List<TagChip>,
    selectedTagIds: SnapshotStateList<String>,
    onShowAddTag: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // E2.8 — `isError` + `supportingText` quando `nameError`
        // estiver preenchido. A mensagem é resolvida via
        // [resolveCreateProjectNameError] para manter a string
        // localizada em pt/en (regra E4.6).
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(text = stringResource(id = R.string.home_create_project_name_label)) },
            singleLine = true,
            isError = nameError != null,
            supportingText = {
                val resolved = resolveCreateProjectNameError(nameError)
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
            value = description,
            onValueChange = onDescriptionChange,
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
                selectedTagIds = selectedTagIds,
            )
        }
        TextButton(
            onClick = onShowAddTag,
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
    selectedTagIds: SnapshotStateList<String>,
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        availableTags.forEach { chip ->
            val isSelected = selectedTagIds.contains(chip.id)
            FilterChip(
                selected = isSelected,
                onClick = {
                    if (isSelected) selectedTagIds.remove(chip.id)
                    else selectedTagIds.add(chip.id)
                },
                label = { Text(text = chip.name) },
                // E4.4: FilterChip padrão M3 tem ~32dp de altura.
                // Aplicamos 48dp para satisfazer WCAG 2.5.5.
                modifier = Modifier.heightIn(min = 48.dp),
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.home_create_project_add_tag_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.common_close))
            }
        },
    )
}
