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
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TaskPriorityChangeForbiddenException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository

/**
 * Testes do caso de uso [UpdateTaskUseCase].
 *
 * Cobre a regra **RN02** (E2.4) — alteração de prioridade em tarefas
 * concluídas é bloqueada. Como o caso de uso recarrega o estado
 * persistido para detectar tentativas de bypass via `copy`/objeto
 * velho, todos os testes que invocam `invoke(Task)` precisam mockar
 * `repository.findById` retornando um Task **ativo** (caso a operação
 * seja legítima) ou nada/ativo (quando for a primeira escrita).
 *
 * Garante ainda a reconciliação do lembrete de prazo (E3.6).
 */
class UpdateTaskUseCaseTest {

    private val repository: TaskRepository = mockk()
    private val deadlineScheduler: DeadlineNotificationScheduler = mockk(relaxed = true)
    private val useCase = UpdateTaskUseCase(repository, deadlineScheduler)

    @Test
    fun `update com prazo futuro reagende lembrete`() = runTest {
        val dueDate = Instant.now().plus(java.time.Duration.ofHours(6))
        val task = Task.create(projectId = "p1", title = "Tarefa", dueDate = dueDate)
        // Estado persistido é o ativo original (RN02 não bloqueia).
        coEvery { repository.findById(task.id) } returns task
        coEvery { repository.update(any()) } answers { firstArg() }

        useCase(task)

        coVerify { repository.update(task) }
        verify(exactly = 1) {
            deadlineScheduler.schedule(task.id, dueDate.minus(DeadlineNotificationScheduler.REMINDER_LEAD))
        }
    }

    @Test
    fun `update removendo prazo cancela lembrete`() = runTest {
        val task = Task.create(
            projectId = "p1",
            title = "Tarefa",
            dueDate = Instant.now().plus(java.time.Duration.ofHours(6)),
        )
        val taskSemPrazo = task.changeDueDate(null)
        coEvery { repository.findById(task.id) } returns task
        coEvery { repository.update(any()) } answers { firstArg() }

        useCase(taskSemPrazo)

        verify(exactly = 1) { deadlineScheduler.cancel(task.id) }
        verify(exactly = 0) { deadlineScheduler.schedule(any(), any()) }
    }

    @Test
    fun `update para DONE cancela lembrete`() = runTest {
        val taskDoing = Task.create(
            projectId = "p1",
            title = "Tarefa",
            status = TaskStatus.DOING,
            dueDate = Instant.now().plus(java.time.Duration.ofHours(6)),
        )
        val taskDone = taskDoing.transitionTo(TaskStatus.DONE)
        coEvery { repository.findById(taskDoing.id) } returns taskDoing
        coEvery { repository.update(any()) } answers { firstArg() }

        useCase(taskDone)

        verify(exactly = 1) { deadlineScheduler.cancel(taskDoing.id) }
        verify(exactly = 0) { deadlineScheduler.schedule(any(), any()) }
    }

    @Test
    fun `update com prazo no passado cancela lembrete`() = runTest {
        val taskPersistida = Task.create(
            projectId = "p1",
            title = "Tarefa",
            dueDate = Instant.now().plus(java.time.Duration.ofHours(6)),
        )
        val taskNoPassado = taskPersistida.changeDueDate(
            Instant.now().minus(java.time.Duration.ofHours(2)),
        )
        coEvery { repository.findById(taskPersistida.id) } returns taskPersistida
        coEvery { repository.update(any()) } answers { firstArg() }

        useCase(taskNoPassado)

        verify(exactly = 1) { deadlineScheduler.cancel(taskPersistida.id) }
        verify(exactly = 0) { deadlineScheduler.schedule(any(), any()) }
    }

    @Test
    fun `update preserva tarefa retornada pelo repositorio`() = runTest {
        val task = Task.create(projectId = "p1", title = "Tarefa")
        coEvery { repository.findById(task.id) } returns task
        coEvery { repository.update(any()) } answers { firstArg() }

        val result = useCase(task)

        assertThat(result).isEqualTo(task)
    }

    @Test
    fun `RN02 update de tarefa concluida eh bloqueado`() = runTest {
        // Cenário coberto pela nova regra: tarefa persistida está
        // em DONE; qualquer tentativa de update (mudando prioridade,
        // título, etc.) deve ser rejeitada com
        // TaskPriorityChangeForbiddenException.
        val taskPersistida = Task.create(
            projectId = "p1",
            title = "Tarefa",
            status = TaskStatus.DONE,
            priority = TaskPriority.LOW,
        )
        val tentativa = taskPersistida.copy(priority = TaskPriority.HIGH)
        coEvery { repository.findById(taskPersistida.id) } returns taskPersistida

        val ex = assertThrows(TaskPriorityChangeForbiddenException::class.java) {
            kotlinx.coroutines.runBlocking { useCase(tentativa) }
        }
        assertThat(ex.message).contains("RN02")
        // Update nunca é chamado quando a persistência indica DONE.
        coVerify(exactly = 0) { repository.update(any()) }
    }
}
