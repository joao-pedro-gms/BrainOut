// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository

/**
 * Testes do caso de uso [UpdateTaskUseCase], cobrindo a reconciliação
 * do lembrete de prazo do marco E3.6:
 * - Prazo alterado para futuro → lembrete reagendado.
 * - Prazo removido → lembrete cancelado.
 * - Status DONE → lembrete cancelado.
 */
class UpdateTaskUseCaseTest {

    private val repository: TaskRepository = mockk()
    private val deadlineScheduler: DeadlineNotificationScheduler = mockk(relaxed = true)
    private val useCase = UpdateTaskUseCase(repository, deadlineScheduler)

    @Test
    fun `update com prazo futuro reagende lembrete`() = runTest {
        val dueDate = Instant.now().plus(java.time.Duration.ofHours(6))
        val task = Task.create(projectId = "p1", title = "Tarefa", dueDate = dueDate)
        coEvery { repository.update(any()) } answers { firstArg() }

        useCase(task)

        coVerify { repository.update(task) }
        verify(exactly = 1) {
            deadlineScheduler.schedule(task.id, dueDate.minus(DeadlineNotificationScheduler.REMINDER_LEAD))
        }
    }

    @Test
    fun `update removendo prazo cancela lembrete`() = runTest {
        val task = Task.create(projectId = "p1", title = "Tarefa")
        coEvery { repository.update(any()) } answers { firstArg() }

        useCase(task)

        verify(exactly = 1) { deadlineScheduler.cancel(task.id) }
        verify(exactly = 0) { deadlineScheduler.schedule(any(), any()) }
    }

    @Test
    fun `update para DONE cancela lembrete`() = runTest {
        val task = Task.create(
            projectId = "p1",
            title = "Tarefa",
            status = TaskStatus.DONE,
            dueDate = Instant.now().plus(java.time.Duration.ofHours(6)),
        )
        coEvery { repository.update(any()) } answers { firstArg() }

        useCase(task)

        verify(exactly = 1) { deadlineScheduler.cancel(task.id) }
        verify(exactly = 0) { deadlineScheduler.schedule(any(), any()) }
    }

    @Test
    fun `update com prazo no passado cancela lembrete`() = runTest {
        val task = Task.create(
            projectId = "p1",
            title = "Tarefa",
            dueDate = Instant.now().minus(java.time.Duration.ofHours(2)),
        )
        coEvery { repository.update(any()) } answers { firstArg() }

        useCase(task)

        verify(exactly = 1) { deadlineScheduler.cancel(task.id) }
        verify(exactly = 0) { deadlineScheduler.schedule(any(), any()) }
    }

    @Test
    fun `update preserva tarefa retornada pelo repositorio`() = runTest {
        val task = Task.create(projectId = "p1", title = "Tarefa")
        coEvery { repository.update(any()) } answers { firstArg() }

        val result = useCase(task)

        assertThat(result).isEqualTo(task)
    }
}
