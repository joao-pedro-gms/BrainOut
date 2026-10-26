// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupAccountOption
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupRepository
import java.io.InputStream
import javax.inject.Inject

/** Decifra e valida o arquivo sem gravar; permite selecionar uma conta de arquivos v1. */
class InspectBackupUseCase
    @Inject
    constructor(
        private val repository: BackupRepository,
    ) {
        suspend operator fun invoke(
            input: InputStream,
            passphrase: String,
        ): List<BackupAccountOption> {
            requireValidPassword(passphrase, subject = "A senha do backup")
            return repository.inspect(input, passphrase)
        }
    }
