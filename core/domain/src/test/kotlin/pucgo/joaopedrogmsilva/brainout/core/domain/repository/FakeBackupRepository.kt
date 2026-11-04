// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.repository

import pucgo.joaopedrogmsilva.brainout.core.domain.model.BackupSummary
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant

/**
 * [BackupRepository] em memória para os testes do domínio.
 *
 * Registra as chamadas e devolve respostas programadas — permite
 * testar os casos de uso de backup sem tocar em Room, DataStore ou
 * criptografia.
 */
internal class FakeBackupRepository : BackupRepository {
    /** Resposta devolvida por [exportTo]. */
    var exportSummary: BackupSummary =
        BackupSummary(
            createdAt = Instant.EPOCH,
            users = 1,
            projects = 2,
            tasks = 3,
            tags = 4,
        )

    /** Resposta devolvida por [restoreFrom]. */
    var restoreResult: BackupRestoreResult =
        BackupRestoreResult(
            summary = exportSummary,
            taskIds = emptySet(),
            reminders = emptyList(),
        )

    /** Ids devolvidos por [currentTaskIds]. */
    var taskIds: Set<String> = emptySet()

    /** Erro lançado por [exportTo], quando definido. */
    var exportError: Throwable? = null

    /** Erro lançado por [restoreFrom], quando definido. */
    var restoreError: Throwable? = null

    /** Senha recebida na última exportação. */
    var exportedPassphrase: String? = null

    /** Senha recebida na última restauração. */
    var restoredPassphrase: String? = null

    /** `true` quando [exportTo] foi chamada. */
    var exportCalled: Boolean = false

    /** `true` quando [restoreFrom] foi chamada. */
    var restoreCalled: Boolean = false

    override suspend fun inspect(
        input: InputStream,
        passphrase: String,
    ): List<BackupAccountOption> = emptyList()

    override suspend fun exportTo(
        output: OutputStream,
        passphrase: String,
    ): BackupSummary {
        exportCalled = true
        exportedPassphrase = passphrase
        exportError?.let { throw it }
        return exportSummary
    }

    override suspend fun restoreFrom(
        input: InputStream,
        passphrase: String,
        sourceUserId: String?,
        expectedUserId: String?,
    ): BackupRestoreResult {
        restoreCalled = true
        restoredPassphrase = passphrase
        restoreError?.let { throw it }
        return restoreResult
    }

    override suspend fun currentTaskIds(): Set<String> = taskIds
}
