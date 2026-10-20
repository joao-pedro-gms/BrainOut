// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.model.BackupSummary
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.FakeDeadlineNotificationScheduler
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupReminder
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupRestoreResult
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.FakeBackupRepository
import java.io.ByteArrayInputStream
import java.time.Duration
import java.time.Instant

/**
 * Testes do [RestoreBackupUseCase] — restauração do backup local e
 * reconciliação dos lembretes de prazo (issue #214 — LOCAL-03).
 */
class RestoreBackupUseCaseTest {
    private val repository = FakeBackupRepository()
    private val scheduler = FakeDeadlineNotificationScheduler()
    private val useCase = RestoreBackupUseCase(repository, scheduler)

    @Test
    fun `restaura e devolve o resultado da operacao`() =
        runTest {
            val result = useCase(ByteArrayInputStream(ByteArray(0)), PASSPHRASE)

            assertThat(repository.restoreCalled).isTrue()
            assertThat(result.summary.tasks).isEqualTo(3)
        }

    @Test
    fun `cancela o lembrete das tarefas que sumiram do aparelho`() =
        runTest {
            repository.taskIds = setOf("t-1", "t-2", "t-3")
            repository.restoreResult =
                BackupRestoreResult(
                    summary = SUMMARY,
                    taskIds = setOf("t-1"),
                    reminders = emptyList(),
                )

            useCase(ByteArrayInputStream(ByteArray(0)), PASSPHRASE)

            assertThat(scheduler.cancelled).containsExactly("t-2", "t-3")
        }

    @Test
    fun `reagenda o lembrete das tarefas restauradas com uma hora de antecedencia`() =
        runTest {
            val dueDate = Instant.parse("2026-10-20T18:00:00Z")
            repository.taskIds = emptySet()
            repository.restoreResult =
                BackupRestoreResult(
                    summary = SUMMARY,
                    taskIds = setOf("t-1", "t-2"),
                    reminders = listOf(BackupReminder(taskId = "t-1", dueDate = dueDate)),
                )

            useCase(ByteArrayInputStream(ByteArray(0)), PASSPHRASE)

            assertThat(scheduler.scheduled).hasSize(1)
            val (taskId, triggerAt) = scheduler.scheduled.first()
            assertThat(taskId).isEqualTo("t-1")
            assertThat(triggerAt).isEqualTo(dueDate.minus(DeadlineNotificationScheduler.REMINDER_LEAD))
            assertThat(Duration.between(triggerAt, dueDate)).isEqualTo(Duration.ofHours(1))
        }

    @Test
    fun `nao cancela lembrete de tarefa que continua no aparelho`() =
        runTest {
            repository.taskIds = setOf("t-1")
            repository.restoreResult =
                BackupRestoreResult(
                    summary = SUMMARY,
                    taskIds = setOf("t-1"),
                    reminders = emptyList(),
                )

            useCase(ByteArrayInputStream(ByteArray(0)), PASSPHRASE)

            assertThat(scheduler.cancelled).isEmpty()
        }

    @Test
    fun `nao reagenda lembrete de tarefa restaurada sem prazo`() =
        runTest {
            repository.restoreResult =
                BackupRestoreResult(
                    summary = SUMMARY,
                    taskIds = setOf("t-1"),
                    reminders = emptyList(),
                )

            useCase(ByteArrayInputStream(ByteArray(0)), PASSPHRASE)

            assertThat(scheduler.scheduled).isEmpty()
        }

    @Test
    fun `rejeita senha curta antes de tocar no repositorio`() =
        runTest {
            val error =
                runCatching {
                    useCase(ByteArrayInputStream(ByteArray(0)), "curta")
                }.exceptionOrNull()

            assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(repository.restoreCalled).isFalse()
        }

    @Test
    fun `falha na restauracao nao mexe nos lembretes`() =
        runTest {
            val failure = IllegalStateException("arquivo corrompido")
            repository.restoreError = failure
            repository.taskIds = setOf("t-1")

            val error =
                runCatching {
                    useCase(ByteArrayInputStream(ByteArray(0)), PASSPHRASE)
                }.exceptionOrNull()

            assertThat(error).isSameInstanceAs(failure)
            assertThat(scheduler.cancelled).isEmpty()
            assertThat(scheduler.scheduled).isEmpty()
        }

    private companion object {
        const val PASSPHRASE: String = "senha-segura"

        val SUMMARY: BackupSummary =
            BackupSummary(
                createdAt = Instant.EPOCH,
                users = 1,
                projects = 1,
                tasks = 1,
                tags = 0,
            )
    }
}
