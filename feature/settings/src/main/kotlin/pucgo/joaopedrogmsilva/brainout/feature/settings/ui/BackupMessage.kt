// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.domain.model.BackupSummary
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoRadii
import pucgo.joaopedrogmsilva.brainout.feature.settings.R
import pucgo.joaopedrogmsilva.brainout.feature.settings.viewmodel.BackupErrorType
import pucgo.joaopedrogmsilva.brainout.feature.settings.viewmodel.BackupSuccessType
import pucgo.joaopedrogmsilva.brainout.feature.settings.viewmodel.BackupUiState

/**
 * Mensagem de erro ou sucesso da última operação de backup.
 *
 * Vive em arquivo próprio porque é uma peça diferente do cartão: a
 * mensagem carrega o **resultado** (erro tipado ou sucesso com o
 * resumo do conteúdo), enquanto o cartão carrega as **ações**. O
 * `BackupSection` orquestra as duas.
 */
@Composable
internal fun BackupMessage(
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
        shape = RoundedCornerShape(NeoRadii.card),
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
            // Quantos projetos e tarefas foram para o arquivo — sem
            // isso, "backup exportado" não diz nada sobre o conteúdo.
            val summary = state.summary
            if (!isError && summary != null) {
                Text(
                    text = summary.describe(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.testTag(BackupTestTags.SUMMARY),
                )
            }
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
 * Resource da mensagem de erro correspondente ao tipo de falha.
 */
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
 * Resumo do conteúdo exportado/restaurado, em texto localizado.
 *
 * Usa recursos `plurals` porque a quantidade varia e "1 projetos" seria
 * errado.
 */
@Composable
private fun BackupSummary.describe(): String {
    val projects =
        pluralStringResource(
            R.plurals.settings_backup_projects,
            projects,
            projects,
        )
    val tasks =
        pluralStringResource(
            R.plurals.settings_backup_tasks,
            tasks,
            tasks,
        )
    return stringResource(id = R.string.settings_backup_summary, projects, tasks)
}
