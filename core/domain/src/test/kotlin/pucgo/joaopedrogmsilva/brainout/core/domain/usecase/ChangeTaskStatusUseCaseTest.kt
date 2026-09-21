// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidStateTransitionException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository

/**
 * Testes do caso de uso [ChangeTaskStatusUseCase], incluindo a
 * reconciliação do lembrete de prazo introduzida no marco E3.6:
 * - Concluir (→ DONE) cancela o lembrete.
 * - Reabrir (→ DOING/TODO) reagenda o lembrete quando há prazo futuro.
 * - Transição inválida continua lançando [InvalidStateTransitionException].
 */
class ChangeTaskStatusUseCaseTest {

    private val repository: TaskRepository = mockk()
    private val deadlineScheduler: DeadlineNotificationScheduler = mockk(relaxed = true)
    private val useCase = ChangeTaskStatusUseCase(repository, deadlineScheduler)

    private fun activeTask(
        status: TaskStatus = TaskStatus.DOING,
        dueDate: Instant? = Instant.now().plus(java.time.Duration.ofHours(4)),
    ): Task = Task.create(
        projectId = "p1",
        title = "Tarefa",
        status = status,
        dueDate = dueDate,
    )

    @Test
    fun `transicao valida persiste e retorna task atualizada`() = runTest {
        val task = activeTask(status = TaskStatus.TODO)
        val done = task.transitionTo(TaskStatus.DOING)
        coEvery { repository.changeStatus(task.id, TaskStatus.DOING) } returns done

        val result = useCase(task.id, TaskStatus.DOING)

        assertThat(result).isEqualTo(done)
        coVerify { repository.changeStatus(task.id, TaskStatus.DOING) }
    }

    @Test
    fun `concluir tarefa cancela lembrete de prazo`() = runTest {
        val task = activeTask()
        val done = task.transitionTo(TaskStatus.DONE)
        coEvery { repository.changeStatus(task.id, TaskStatus.DONE) } returns done

        useCase(task.id, TaskStatus.DONE)

        verify(exactly = 1) { deadlineScheduler.cancel(task.id) }
        verify(exactly = 0) { deadlineScheduler.schedule(any(), any()) }
    }

    @Test
    fun `reabrir tarefa com prazo futuro reagenda lembrete`() = runTest {
        val done = activeTask(status = TaskStatus.DOING).transitionTo(TaskStatus.DONE)
        val reopened = done.transitionTo(TaskStatus.DOING)
        coEvery { repository.changeStatus(done.id, TaskStatus.DOING) } returns reopened

        useCase(done.id, TaskStatus.DOING)

        verify(exactly = 1) {
            deadlineScheduler.schedule(
                taskId = done.id,
                triggerAt = match { it == reopened.dueDate?.minus(DeadlineNotificationScheduler.REMINDER_LEAD) },
            )
        }
    }

    @Test
    fun `transicao invalida lanca InvalidStateTransitionException`() = runTest {
        val task = activeTask(status = TaskStatus.DOING)
        coEvery {
            repository.changeStatus(task.id, TaskStatus.TODO)
        } throws InvalidStateTransitionException("Transição de status inválida: DOING -> TODO")

        val ex = assertThrows(InvalidStateTransitionException::class.java) {
            kotlinx.coroutines.runBlocking { useCase(task.id, TaskStatus.TODO) }
        }

        assertThat(ex.message).contains("DOING")
        verify(exactly = 0) { deadlineScheduler.schedule(any(), any()) }
        verify(exactly = 0) { deadlineScheduler.cancel(any()) }
    }
}
