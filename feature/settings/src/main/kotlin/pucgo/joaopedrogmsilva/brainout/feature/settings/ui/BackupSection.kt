// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.settings.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pucgo.joaopedrogmsilva.brainout.feature.settings.R
import pucgo.joaopedrogmsilva.brainout.feature.settings.viewmodel.BackupErrorType
import pucgo.joaopedrogmsilva.brainout.feature.settings.viewmodel.BackupSuccessType
import pucgo.joaopedrogmsilva.brainout.feature.settings.viewmodel.BackupUiState
import pucgo.joaopedrogmsilva.brainout.feature.settings.viewmodel.BackupViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import pucgo.joaopedrogmsilva.brainout.core.ui.R as UiR

/** Tipo MIME do arquivo de backup — o conteúdo é binário cifrado. */
private const val BACKUP_MIME_TYPE: String = "application/octet-stream"

/** Extensão do arquivo de backup. */
private const val BACKUP_FILE_EXTENSION: String = "bnbak"

/**
 * Seção de backup local dentro da tela de Configurações
 * (issue #214 — LOCAL-03).
 *
 * A seção expõe duas ações:
 *
 * - **Exportar** — pede a senha de cifragem, abre o seletor de
 *   documentos do sistema (Storage Access Framework) e grava o
 *   arquivo escolhido.
 * - **Restaurar** — pede a confirmação de substituição, abre o
 *   seletor para escolher o arquivo e, com a senha, substitui o
 *   conteúdo local.
 *
 * O seletor de arquivos é do sistema: o app **nunca** recebe acesso
 * amplo ao armazenamento — o usuário escolhe um arquivo por vez.
 *
 * @param onRestoreCompleted invocado depois de uma restauração bem
 *  sucedida — o `NavHost` usa o gancho para voltar à tela de login,
 *  já que a sessão foi invalidada.
 */
@Composable
fun BackupSection(
    onRestoreCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val passphraseState = remember { BackupPassphraseUiState() }
    var showReplaceConfirm by remember { mutableStateOf(false) }

    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE),
        ) { uri ->
            if (uri != null) {
                viewModel.export(uri, passphraseState.passphrase)
            }
            passphraseState.clear()
        }
    val importLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) {
                passphraseState.pendingRestoreUri = uri
                passphraseState.mode = BackupPassphraseMode.Restore
            }
        }

    LaunchedEffect(state.successType) {
        if (state.successType == BackupSuccessType.Restored) {
            onRestoreCompleted()
        }
    }

    BackupContent(
        modifier = modifier,
        state = state,
        onExport = { passphraseState.start(BackupPassphraseMode.Export) },
        onRestore = {
            // A senha só é pedida depois da escolha do arquivo — a
            // senha pertence ao arquivo, não à confirmação.
            showReplaceConfirm = true
        },
        onDismissMessage = viewModel::dismissMessage,
    )

    BackupDialogs(
        passphraseState = passphraseState,
        showReplaceConfirm = showReplaceConfirm,
        onExportConfirmed = { confirmation ->
            if (passphraseState.validate(confirmation)) {
                passphraseState.mode = null
                exportLauncher.launch(defaultBackupFileName())
            }
        },
        onRestoreConfirmed = { confirmation ->
            val uri = passphraseState.pendingRestoreUri
            if (passphraseState.validate(confirmation) && uri != null) {
                passphraseState.mode = null
                passphraseState.pendingRestoreUri = null
                viewModel.restore(uri, passphraseState.passphrase)
                passphraseState.clear()
            }
        },
        onReplaceConfirmed = {
            showReplaceConfirm = false
            importLauncher.launch(arrayOf("*/*"))
        },
        onDismissReplace = { showReplaceConfirm = false },
    )
}

/** Cartão com as duas ações de backup e a mensagem do último resultado. */
@Composable
private fun BackupContent(
    state: BackupUiState,
    onExport: () -> Unit,
    onRestore: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(id = R.string.settings_section_backup).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsRow(
                    option = SettingsOption(SettingsActionType.ExportBackup, R.string.settings_option_export_backup),
                    onClick = onExport,
                    testTag = BackupTestTags.EXPORT_ROW,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingsRow(
                    option = SettingsOption(SettingsActionType.RestoreBackup, R.string.settings_option_restore_backup),
                    onClick = onRestore,
                    testTag = BackupTestTags.RESTORE_ROW,
                )
            }
        }
        Text(
            text = stringResource(id = R.string.settings_backup_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        BackupMessage(state = state, onDismiss = onDismissMessage)
    }
}

/**
 * Diálogos da seção de backup.
 *
 * A ordem da restauração é deliberada: primeiro a confirmação de
 * substituição, depois a escolha do arquivo e só então a senha.
 */
@Composable
private fun BackupDialogs(
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

/** Mensagem de erro ou sucesso da última operação de backup. */
@Composable
private fun BackupMessage(
    state: BackupUiState,
    onDismiss: () -> Unit,
) {
    val error = state.errorType
    val success = state.successType
    if (error == null && success == null) return
    val messageRes =
        when {
            error != null -> error.messageRes()
            success == BackupSuccessType.Exported -> R.string.settings_backup_success_exported
            else -> R.string.settings_backup_success_restored
        }
    val isError = error != null
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(BackupTestTags.MESSAGE),
        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(id = messageRes),
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (isError) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(BackupTestTags.MESSAGE_DISMISS),
            ) {
                Text(text = stringResource(id = R.string.settings_backup_cta_dismiss))
            }
        }
    }
}

/**
 * Estado transitório do diálogo de senha do backup.
 *
 * Fica em um objeto próprio (e não em variáveis soltas da tela) para
 * que a seção de backup e os diálogos compartilhem a mesma fonte de
 * verdade sem transformar a assinatura do composable em uma lista
 * longa de parâmetros.
 *
 * A senha **não** sobrevive à rotação: ela é descartada junto com o
 * diálogo, o que evita deixar credencial em estado de recomposição.
 */
@Stable
internal class BackupPassphraseUiState {
    /** Modo do diálogo — exportar ou restaurar. */
    var mode: BackupPassphraseMode? by mutableStateOf(null)

    /** Senha digitada. */
    var passphrase: String by mutableStateOf("")

    /** Erro de validação, quando houver. */
    var error: BackupPassphraseError? by mutableStateOf(null)

    /** Arquivo escolhido para a restauração. */
    var pendingRestoreUri: Uri? by mutableStateOf(null)

    /** Abre o diálogo no modo informado. */
    fun start(mode: BackupPassphraseMode) {
        this.mode = mode
        error = null
    }

    /** Registra a digitação e limpa o erro anterior. */
    fun onChange(value: String) {
        passphrase = value
        error = null
    }

    /**
     * Valida a senha contra a confirmação.
     *
     * @return `true` quando a senha é válida.
     */
    fun validate(confirmation: String): Boolean {
        error = validatePassphrase(passphrase, confirmation)
        return error == null
    }

    /** Limpa todo o estado transitório do diálogo. */
    fun clear() {
        mode = null
        passphrase = ""
        error = null
        pendingRestoreUri = null
    }
}

/** Resource da mensagem de erro correspondente ao tipo de falha. */
private fun BackupErrorType.messageRes(): Int =
    when (this) {
        BackupErrorType.InvalidFile -> R.string.settings_backup_error_invalid_file
        BackupErrorType.UnsupportedVersion -> R.string.settings_backup_error_unsupported_version
        BackupErrorType.Integrity -> R.string.settings_backup_error_integrity
        BackupErrorType.TooLarge -> R.string.settings_backup_error_too_large
        BackupErrorType.RestoreFailed -> R.string.settings_backup_error_restore_failed
        BackupErrorType.ExportFailed -> R.string.settings_backup_error_export_failed
        BackupErrorType.Unexpected -> R.string.settings_backup_error_unexpected
    }

/**
 * Valida a senha do backup.
 *
 * @param passphrase senha digitada.
 * @param confirmation confirmação digitada.
 * @return o erro encontrado, ou `null` quando a senha é válida.
 */
private fun validatePassphrase(
    passphrase: String,
    confirmation: String,
): BackupPassphraseError? =
    when {
        passphrase.length < MIN_PASSPHRASE_LENGTH -> BackupPassphraseError.TooShort
        passphrase != confirmation -> BackupPassphraseError.Mismatch
        else -> null
    }

/** Modo do diálogo de senha — exporta ou restaura. */
internal enum class BackupPassphraseMode {
    Export,
    Restore,
}

/** Erros de validação da senha do backup. */
internal enum class BackupPassphraseError(
    val messageRes: Int,
) {
    TooShort(R.string.settings_backup_error_short_pwd),
    Mismatch(R.string.settings_backup_error_pwd_mismatch),
}

/** Nome de arquivo sugerido na exportação. */
private fun defaultBackupFileName(): String {
    val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    return "brainout-backup-$stamp.$BACKUP_FILE_EXTENSION"
}

/** Tamanho mínimo da senha do backup — igual ao das senhas de conta. */
private const val MIN_PASSPHRASE_LENGTH: Int = 8

/** Identificadores usados por testes Compose. */
object BackupTestTags {
    const val EXPORT_ROW: String = "settings_option_export_backup"
    const val RESTORE_ROW: String = "settings_option_restore_backup"
    const val PASSPHRASE_FIELD: String = "settings_backup_pwd"
    const val PASSPHRASE_CONFIRM_FIELD: String = "settings_backup_pwd_confirm"
    const val PASSPHRASE_TOGGLE: String = "settings_backup_pwd_toggle"
    const val PASSPHRASE_ERROR: String = "settings_backup_pwd_error"
    const val CONFIRM: String = "settings_backup_confirm"
    const val CANCEL: String = "settings_backup_cancel"
    const val REPLACE_CONFIRM: String = "settings_backup_replace_confirm"
    const val MESSAGE: String = "settings_backup_message"
    const val MESSAGE_DISMISS: String = "settings_backup_message_dismiss"
}
