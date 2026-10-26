// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.settings.viewmodel

import pucgo.joaopedrogmsilva.brainout.core.domain.model.BackupSummary
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupAccountOption

/**
 * Estado da seção de backup local (issue #214 — LOCAL-03).
 *
 * @property isBusy operação de exportação/restauração em andamento.
 * @property errorType falha da última operação, já classificada para
 *  que a tela traduza a mensagem (pt/en) sem conhecer a exceção.
 * @property successType operação concluída com sucesso.
 * @property summary contagem do conteúdo exportado ou restaurado.
 */
data class BackupUiState(
    val restoreAccounts: List<BackupAccountOption> = emptyList(),
    val isBusy: Boolean = false,
    val errorType: BackupErrorType? = null,
    val successType: BackupSuccessType? = null,
    val summary: BackupSummary? = null,
)

/**
 * Falhas possíveis do backup local, classificadas para a camada de
 * apresentação.
 *
 * A classificação acontece no [BackupViewModel] (que conhece as
 * exceções de domínio) e a mensagem é escolhida na tela — é o que
 * garante que o erro apareça em pt/en.
 */
enum class BackupErrorType {
    /** O arquivo não é um backup do BrainOut (formato desconhecido ou JSON inválido). */
    InvalidFile,

    /** O backup foi criado por uma versão do formato que o app não lê. */
    UnsupportedVersion,

    /** Senha incorreta ou arquivo adulterado/truncado. */
    Integrity,

    /** O arquivo excede o limite de tamanho aceito. */
    TooLarge,

    /** A gravação dos dados restaurados falhou — nada foi alterado. */
    RestoreFailed,

    /** A exportação falhou (provedor de arquivos, espaço, permissão). */
    ExportFailed,

    /** Falha inesperada. */
    Unexpected,
}

/** Operações de backup concluídas com sucesso. */
enum class BackupSuccessType {
    /** O arquivo de backup foi gravado. */
    Exported,

    /** O conteúdo do arquivo substituiu os dados locais. */
    Restored,
}
