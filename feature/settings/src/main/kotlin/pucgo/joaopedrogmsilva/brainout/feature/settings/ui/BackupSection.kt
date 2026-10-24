// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.settings.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pucgo.joaopedrogmsilva.brainout.feature.settings.R
import pucgo.joaopedrogmsilva.brainout.feature.settings.viewmodel.BackupSuccessType
import pucgo.joaopedrogmsilva.brainout.feature.settings.viewmodel.BackupUiState
import pucgo.joaopedrogmsilva.brainout.feature.settings.viewmodel.BackupViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
internal fun BackupContent(
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
        SettingsCard {
            SettingsRow(
                option =
                    SettingsOption(
                        SettingsRowKind.Local,
                        "export_backup",
                        R.string.settings_option_export_backup,
                    ),
                onClick = onExport,
                testTag = BackupTestTags.EXPORT_ROW,
                // Enquanto uma operação está em andamento as linhas
                // ficam inativas: sem isso, um segundo toque abriria
                // outro seletor de arquivos por cima do primeiro.
                enabled = !state.isBusy,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            SettingsRow(
                option =
                    SettingsOption(
                        SettingsRowKind.Local,
                        "restore_backup",
                        R.string.settings_option_restore_backup,
                    ),
                onClick = onRestore,
                testTag = BackupTestTags.RESTORE_ROW,
                enabled = !state.isBusy,
            )
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

/** Nome de arquivo sugerido na exportação. */
private fun defaultBackupFileName(): String {
    val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    return "brainout-backup-$stamp.$BACKUP_FILE_EXTENSION"
}

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
    const val SUMMARY: String = "settings_backup_summary"
}
