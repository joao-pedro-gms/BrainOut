// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.feature.settings.R
import pucgo.joaopedrogmsilva.brainout.core.ui.R as UiR

/**
 * Diálogos da seção de backup.
 *
 * A ordem da restauração é deliberada: primeiro a confirmação de
 * substituição, depois a escolha do arquivo e só então a senha.
 */
@Composable
internal fun BackupDialogs(
    passphraseState: BackupPassphraseUiState,
    showReplaceConfirm: Boolean,
    onExportConfirmed: (String) -> Unit,
    onRestoreConfirmed: (String) -> Unit,
    onReplaceConfirmed: () -> Unit,
    onDismissReplace: () -> Unit,
) {
    if (showReplaceConfirm) {
        RestoreConfirmDialog(
            onConfirm = onReplaceConfirmed,
            onDismiss = onDismissReplace,
        )
    }
    val mode = passphraseState.mode
    when (mode) {
        BackupPassphraseMode.Export ->
            BackupPassphraseDialog(
                titleRes = R.string.settings_backup_export_dialog_title,
                messageRes = R.string.settings_backup_export_dialog_message,
                passphraseState = passphraseState,
                onConfirm = onExportConfirmed,
                onDismiss = passphraseState::clear,
            )
        BackupPassphraseMode.Restore ->
            BackupPassphraseDialog(
                titleRes = R.string.settings_backup_restore_dialog_title,
                messageRes = R.string.settings_backup_restore_dialog_message,
                passphraseState = passphraseState,
                onConfirm = onRestoreConfirmed,
                onDismiss = passphraseState::clear,
            )
        null -> Unit
    }
}

/**
 * Diálogo de confirmação de substituição.
 *
 * Separate do diálogo de senha porque a ordem importa: primeiro o
 * usuário confirma que entende que os dados locais serão trocados,
 * depois escolhe o arquivo e só então informa a senha.
 */
@Composable
private fun RestoreConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.settings_backup_replace_title)) },
        text = { Text(text = stringResource(id = R.string.settings_backup_replace_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(BackupTestTags.REPLACE_CONFIRM),
            ) {
                Text(text = stringResource(id = R.string.settings_backup_cta_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(BackupTestTags.CANCEL),
            ) {
                Text(text = stringResource(id = R.string.settings_backup_cta_cancel))
            }
        },
    )
}

/**
 * Diálogo que pede a senha de cifragem do backup.
 *
 * @param titleRes título do diálogo.
 * @param messageRes texto explicativo.
 * @param passphraseState estado transitório do diálogo.
 * @param onConfirm callback com a senha de confirmação.
 * @param onDismiss callback de cancelamento.
 */
@Composable
private fun BackupPassphraseDialog(
    titleRes: Int,
    messageRes: Int,
    passphraseState: BackupPassphraseUiState,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmation by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = titleRes)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(id = messageRes))
                BackupPasswordField(
                    value = passphraseState.passphrase,
                    labelRes = R.string.settings_backup_pwd_label,
                    testTag = BackupTestTags.PASSPHRASE_FIELD,
                    visible = visible,
                    onVisibilityChange = { visible = !visible },
                    onValueChange = passphraseState::onChange,
                )
                BackupPasswordField(
                    value = confirmation,
                    labelRes = R.string.settings_backup_pwd_confirm_label,
                    testTag = BackupTestTags.PASSPHRASE_CONFIRM_FIELD,
                    visible = visible,
                    onVisibilityChange = null,
                    onValueChange = { confirmation = it },
                )
                val error = passphraseState.error
                if (error != null) {
                    Text(
                        text = stringResource(id = error.messageRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(BackupTestTags.PASSPHRASE_ERROR),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(confirmation) },
                modifier = Modifier.testTag(BackupTestTags.CONFIRM),
            ) {
                Text(text = stringResource(id = R.string.settings_backup_cta_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(BackupTestTags.CANCEL),
            ) {
                Text(text = stringResource(id = R.string.settings_backup_cta_cancel))
            }
        },
    )
}

/**
 * Campo de senha do diálogo de backup.
 *
 * @param value texto digitado.
 * @param labelRes rótulo do campo.
 * @param testTag tag de teste.
 * @param visible se a senha está revelada.
 * @param onVisibilityChange alterna a revelação; `null` omite o botão.
 * @param onValueChange callback de digitação.
 */
@Composable
private fun BackupPasswordField(
    value: String,
    labelRes: Int,
    testTag: String,
    visible: Boolean,
    onVisibilityChange: (() -> Unit)?,
    onValueChange: (String) -> Unit,
) {
    val showLabel = stringResource(id = R.string.settings_backup_visibility_show)
    val hideLabel = stringResource(id = R.string.settings_backup_visibility_hide)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(testTag),
        label = { Text(text = stringResource(id = labelRes)) },
        singleLine = true,
        visualTransformation =
            if (visible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon =
            if (onVisibilityChange == null) {
                null
            } else {
                {
                    IconButton(
                        onClick = onVisibilityChange,
                        modifier =
                            Modifier
                                .testTag(BackupTestTags.PASSPHRASE_TOGGLE)
                                .semantics {
                                    contentDescription = if (visible) hideLabel else showLabel
                                },
                    ) {
                        Icon(
                            painter =
                                if (visible) {
                                    painterResource(
                                        UiR.drawable.neo_ic_visibility_off,
                                    )
                                } else {
                                    painterResource(UiR.drawable.neo_ic_visibility)
                                },
                            contentDescription = if (visible) hideLabel else showLabel,
                        )
                    }
                }
            },
    )
}
