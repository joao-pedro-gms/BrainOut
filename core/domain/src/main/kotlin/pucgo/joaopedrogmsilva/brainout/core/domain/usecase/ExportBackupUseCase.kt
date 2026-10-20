// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import pucgo.joaopedrogmsilva.brainout.core.domain.model.BackupSummary
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupRepository
import java.io.OutputStream
import javax.inject.Inject

/**
 * Exporta o conteúdo local para um arquivo de backup cifrado.
 *
 * A senha ([passphrase]) é a mesma que o usuário digita ao restaurar:
 * ela cifra o arquivo (AES-256-GCM) e, para as contas que ainda não
 * existirem no aparelho de destino, passa a ser a senha delas. O
 * detalhamento está em `docs/BACKUP.md`.
 *
 * @property backupRepository porta de backup (implementação em
 *  `:core:data`).
 */
class ExportBackupUseCase
    @Inject
    constructor(
        private val backupRepository: BackupRepository,
    ) {
        /**
         * @param output destino do arquivo.
         * @param passphrase senha de cifragem do backup.
         * @return o resumo do conteúdo gravado.
         * @throws IllegalArgumentException se a senha for curta demais
         *  (mesmo mínimo de [MIN_PASSWORD_LENGTH], já que ela pode
         *  virar a senha de uma conta restaurada).
         */
        suspend operator fun invoke(
            output: OutputStream,
            passphrase: String,
        ): BackupSummary {
            require(passphrase.length >= MIN_PASSWORD_LENGTH) {
                "A senha do backup deve ter pelo menos $MIN_PASSWORD_LENGTH caracteres"
            }
            return backupRepository.exportTo(output, passphrase)
        }
    }
