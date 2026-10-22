// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.settings.ui

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.MIN_PASSWORD_LENGTH
import pucgo.joaopedrogmsilva.brainout.feature.settings.R

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

/**
 * Valida a senha do backup.
 *
 * O mínimo é o mesmo das senhas de conta ([MIN_PASSWORD_LENGTH]) — o
 * arquivo restaurado é a conta, e uma regra só evita que o usuário
 * escolha uma senha que o app vai recusar depois.
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
        passphrase.length < MIN_PASSWORD_LENGTH -> BackupPassphraseError.TooShort
        passphrase != confirmation -> BackupPassphraseError.Mismatch
        else -> null
    }
