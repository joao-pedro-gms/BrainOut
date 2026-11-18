// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import pucgo.joaopedrogmsilva.brainout.core.domain.model.BackupSummary
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupRepository
import java.io.OutputStream
import javax.inject.Inject

/**
 * Exporta o conteúdo local para um arquivo de backup cifrado.
 *
 * A senha ([passphrase]) cifra o arquivo e será solicitada para decifrá-lo; ela
 * não se torna senha de login. A restauração preserva as credenciais da conta
 * conectada, conforme o contrato em `docs/BACKUP.md`.
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
         *  (o arquivo exige no mínimo [MIN_PASSWORD_LENGTH] caracteres; veja `docs/BACKUP.md`).
         */
        suspend operator fun invoke(
            output: OutputStream,
            passphrase: String,
        ): BackupSummary {
            requireValidPassword(passphrase, subject = "A senha do backup")
            return backupRepository.exportTo(output, passphrase)
        }
    }
