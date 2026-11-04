// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.repository

import pucgo.joaopedrogmsilva.brainout.core.domain.model.BackupSummary
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant

/**
 * Porta do backup local exportável (issue #214 — LOCAL-03).
 *
 * O contrato do arquivo (formato, versão, limite de tamanho,
 * integridade, política de contas) está documentado em
 * `docs/BACKUP.md`. O resumo das regras que a UI depende:
 *
 * - **Sem rede.** A exportação e a restauração acontecem inteiramente
 *   no aparelho, sobre `InputStream`/`OutputStream` fornecidos pela
 *   camada de apresentação (Storage Access Framework).
 * - **Senha obrigatória.** O conteúdo é cifrado com AES-256-GCM e a
 *   chave é derivada da senha do backup (PBKDF2-HMAC-SHA256). Nenhum
 *   material do Android Keystore (pepper) nem hash de senha viaja no
 *   arquivo.
 * - **Falha não destrutiva.** Arquivo inválido, truncado, com versão
 *   desconhecida ou acima do limite falha **sem tocar** nos dados já
 *   existentes.
 * - **Restauração atômica.** A gravação dos dados restaurados é
 *   feita em uma única transação; ou entra tudo, ou nada entra.
 *
 * @see ExportBackupUseCase
 * @see RestoreBackupUseCase
 */
interface BackupRepository {
    suspend fun inspect(
        input: InputStream,
        passphrase: String,
    ): List<BackupAccountOption>

    /**
     * Escreve um backup cifrado dos dados da conta ativa em [output].
     *
     * @param output destino do arquivo (o chamador decide onde — SAF,
     *  `FileOutputStream`, `ByteArrayOutputStream` em teste).
     * @param passphrase senha usada para cifrar o conteúdo.
     * @return o resumo do que foi gravado.
     */
    suspend fun exportTo(
        output: OutputStream,
        passphrase: String,
    ): BackupSummary

    /**
     * Lê um backup cifrado de [input] e substitui somente os dados da conta ativa.
     * A conta de origem é selecionada por [sourceUserId]; credenciais são preservadas.
     *
     * @param input origem do arquivo.
     * @param passphrase senha usada para decifrar o conteúdo.
     * @return o resumo do que foi restaurado, os ids das tarefas
     *  presentes e os prazos que precisam de lembrete reagendado.
     */
    suspend fun restoreFrom(
        input: InputStream,
        passphrase: String,
        sourceUserId: String? = null,
        expectedUserId: String? = null,
    ): BackupRestoreResult

    /**
     * Ids das tarefas da conta ativa.
     *
     * Usado pela reconciliação de lembretes: as tarefas que existiam
     * antes da restauração e não existem depois precisam ter o
     * lembrete cancelado, para não disparar notificações de tarefas
     * que não estão mais no banco.
     */
    suspend fun currentTaskIds(): Set<String>
}

/**
 * Resultado de uma restauração bem-sucedida.
 *
 * @property summary contagem do conteúdo restaurado.
 * @property taskIds ids de todas as tarefas restauradas.
 * @property reminders tarefas com prazo — o lembrete de cada uma
 *  precisa ser (re)agendado, senão o aparelho fica silencioso depois
 *  da restauração.
 */
data class BackupRestoreResult(
    val summary: BackupSummary,
    val taskIds: Set<String>,
    val reminders: List<BackupReminder>,
    val ownerId: String? = null,
)

/**
 * Tarefa restaurada que possui prazo.
 *
 * @property taskId id da tarefa.
 * @property dueDate instante do prazo.
 */
data class BackupReminder(
    val taskId: String,
    val dueDate: Instant,
)

/** Conta identificável no arquivo; a seleção nunca altera a identidade da conta de destino. */
data class BackupAccountOption(
    val id: String,
    val label: String,
)
