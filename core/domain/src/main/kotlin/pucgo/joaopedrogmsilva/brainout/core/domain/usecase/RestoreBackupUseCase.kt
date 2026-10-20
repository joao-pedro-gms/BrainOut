// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupRestoreResult
import java.io.InputStream
import javax.inject.Inject

/**
 * Restaura um backup cifrado, substituindo o conteúdo local.
 *
 * Além de escrever os dados, este caso de uso **reconcilia os
 * lembretes de prazo** (issue #214 — LOCAL-03):
 *
 * 1. O ids das tarefas existentes **antes** da restauração são
 *    capturados.
 * 2. A restauração atômica acontece na camada de dados.
 * 3. As tarefas que sumiram têm o lembrete **cancelado** — senão o
 *    aparelho dispararia a notificação de uma tarefa que não existe
 *    mais.
 * 4. As tarefas restauradas com prazo têm o lembrete **reagendado**
 *    com a antecedência de [DeadlineNotificationScheduler.REMINDER_LEAD].
 *    Reagendar (em vez de apenas agendar as novas) é o que impede
 *    notificações duplicadas e corrige o horário quando o prazo
 *    mudou entre a exportação e a restauração.
 *
 * @property backupRepository porta de backup (implementação em
 *  `:core:data`).
 * @property deadlineNotificationScheduler porta de agendamento de
 *  lembretes (implementação WorkManager em `:app`).
 */
class RestoreBackupUseCase
    @Inject
    constructor(
        private val backupRepository: BackupRepository,
        private val deadlineNotificationScheduler: DeadlineNotificationScheduler,
    ) {
        /**
         * @param input origem do arquivo de backup.
         * @param passphrase senha de decifragem do backup.
         * @return o resumo do conteúdo restaurado.
         * @throws IllegalArgumentException se a senha for curta demais.
         * @throws pucgo.joaopedrogmsilva.brainout.core.domain.error.BackupException
         *  se o arquivo for inválido, truncado, com versão desconhecida
         *  ou acima do limite — nesse caso os dados existentes não são
         *  alterados.
         */
        suspend operator fun invoke(
            input: InputStream,
            passphrase: String,
        ): BackupRestoreResult {
            require(passphrase.length >= MIN_PASSWORD_LENGTH) {
                "A senha do backup deve ter pelo menos $MIN_PASSWORD_LENGTH caracteres"
            }
            val previousTaskIds = backupRepository.currentTaskIds()
            val result = backupRepository.restoreFrom(input, passphrase)
            reconcileReminders(previousTaskIds, result)
            return result
        }

        /**
         * Cancela os lembretes das tarefas que saíram do banco e
         * reagenda os das tarefas restauradas que têm prazo.
         */
        private fun reconcileReminders(
            previousTaskIds: Set<String>,
            result: BackupRestoreResult,
        ) {
            val restoredTaskIds = result.taskIds
            (previousTaskIds - restoredTaskIds).forEach { taskId ->
                deadlineNotificationScheduler.cancel(taskId)
            }
            result.reminders.forEach { reminder ->
                deadlineNotificationScheduler.schedule(
                    taskId = reminder.taskId,
                    triggerAt = reminder.dueDate.minus(DeadlineNotificationScheduler.REMINDER_LEAD),
                )
            }
        }
    }
