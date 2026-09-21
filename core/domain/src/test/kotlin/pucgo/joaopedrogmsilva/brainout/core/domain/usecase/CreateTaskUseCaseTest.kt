// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.error.ProjectTaskLimitReachedException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository

/**
 * Testes do caso de uso [CreateTaskUseCase] aplicando a regra
 * de negócio RN01 (limite de 50 tarefas ativas por projeto) e o
 * agendamento do lembrete de prazo (marco E3.6).
 *
 * Garante que:
 * - Quando o projeto está abaixo do limite, a tarefa é criada
 *   com `status = TODO` e `priority = MEDIUM` por padrão.
 * - Quando o projeto já atingiu [MAX_ACTIVE_TASKS_PER_PROJECT],
 *   a operação lança [ProjectTaskLimitReachedException] antes
 *   de qualquer escrita no repositório.
 * - Tarefa criada com prazo futuro agenda lembrete para
 *   `dueDate - 1h`; sem prazo ou com prazo vencido, não agenda.
 */
class CreateTaskUseCaseTest {

    private val repository: TaskRepository = mockk()
    private val deadlineScheduler: DeadlineNotificationScheduler = mockk(relaxed = true)
    private val useCase = CreateTaskUseCase(repository, deadlineScheduler)

    @Test
    fun `cria task quando limite nao foi atingido`() = runTest {
        coEvery { repository.countActiveByProject("p1") } returns 10
        val task = Task.create(projectId = "p1", title = "Tarefa")
        coEvery { repository.create(any()) } returns task

        val result = useCase(projectId = "p1", title = "Tarefa")

        assertThat(result).isEqualTo(task)
        assertThat(result.status).isEqualTo(TaskStatus.TODO)
        assertThat(result.priority).isEqualTo(TaskPriority.MEDIUM)
        coVerify { repository.create(any()) }
    }

    @Test
    fun `lanca ProjectTaskLimitReachedException quando atinge limite de 50 tarefas ativas`() = runTest {
        coEvery { repository.countActiveByProject("p1") } returns MAX_ACTIVE_TASKS_PER_PROJECT

        // `assertThrows` é síncrono; embrulha a chamada suspend em
        // runBlocking para manter a API do JUnit e exercitar o mesmo
        // caminho de exceção do caso de uso.
        val ex = assertThrows(ProjectTaskLimitReachedException::class.java) {
            runBlocking { useCase(projectId = "p1", title = "Tarefa 51") }
        }

        assertThat(ex.message).contains("p1")
        assertThat(ex.message).contains("50")
        coVerify(exactly = 0) { repository.create(any()) }
    }

    @Test
    fun `tarefa com prazo futuro agenda lembrete 1 hora antes`() = runTest {
        val dueDate = Instant.now().plus(java.time.Duration.ofHours(5))
        coEvery { repository.countActiveByProject("p1") } returns 0
        coEvery { repository.create(any()) } answers { firstArg() }

        useCase(projectId = "p1", title = "Com prazo", dueDate = dueDate)

        verify {
            deadlineScheduler.schedule(
                taskId = any(),
                triggerAt = match { it == dueDate.minus(DeadlineNotificationScheduler.REMINDER_LEAD) },
            )
        }
    }

    @Test
    fun `tarefa sem prazo nao agenda lembrete`() = runTest {
        coEvery { repository.countActiveByProject("p1") } returns 0
        coEvery { repository.create(any()) } answers { firstArg() }

        useCase(projectId = "p1", title = "Sem prazo")

        verify(exactly = 0) { deadlineScheduler.schedule(any(), any()) }
    }

    @Test
    fun `tarefa com prazo no passado nao agenda lembrete retroativo`() = runTest {
        val dueDate = Instant.now().minus(java.time.Duration.ofHours(3))
        coEvery { repository.countActiveByProject("p1") } returns 0
        coEvery { repository.create(any()) } answers { firstArg() }

        useCase(projectId = "p1", title = "Prazo vencido", dueDate = dueDate)

        verify(exactly = 0) { deadlineScheduler.schedule(any(), any()) }
    }
}
