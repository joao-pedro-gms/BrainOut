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
 * reconciliação do lembrete de prazo introduzida no marco E3.6 e a
 * **cascata** de RN03 (E2.5) na transição para DONE e nas
 * reaberturas a partir de DONE.
 *
 * - Concluir (→ DONE) passa por
 *   [TaskRepository.completeAndCascade] para também marcar o
 *   projeto como concluído quando for a última ativa.
 * - Reabrir (DONE → DOING) passa por
 *   [TaskRepository.reopenAndCascade] para desmarcar a conclusão
 *   do projeto caso ele estivesse marcado.
 * - Transições intermediárias (TODO ↔ DOING) usam
 *   [TaskRepository.changeStatus] sem cascata.
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
    fun `transicao intermediaria DOING para TODO usa changeStatus`() = runTest {
        val task = activeTask(status = TaskStatus.DOING)
        val target = task.transitionTo(TaskStatus.TODO)
        // Caso o repositório reporte status atual = DOING, o caso
        // de uso decide usar changeStatus direto.
        coEvery { repository.findById(task.id) } returns task
        coEvery { repository.changeStatus(task.id, TaskStatus.TODO) } returns target

        val result = useCase(task.id, TaskStatus.TODO)

        assertThat(result).isEqualTo(target)
        coVerify(exactly = 1) { repository.changeStatus(task.id, TaskStatus.TODO) }
        coVerify(exactly = 0) { repository.completeAndCascade(any()) }
        coVerify(exactly = 0) { repository.reopenAndCascade(any(), any()) }
    }

    @Test
    fun `transicao para DONE usa completeAndCascade para cascata RN03`() = runTest {
        val task = activeTask(status = TaskStatus.DOING)
        val done = task.transitionTo(TaskStatus.DONE)
        coEvery { repository.completeAndCascade(task.id) } returns done

        val result = useCase(task.id, TaskStatus.DONE)

        assertThat(result).isEqualTo(done)
        coVerify(exactly = 1) { repository.completeAndCascade(task.id) }
        // Não pode ter ido pelo changeStatus antigo
        coVerify(exactly = 0) { repository.changeStatus(any(), any()) }
        // Lembrete cancelado por entrar em DONE
        verify(exactly = 1) { deadlineScheduler.cancel(task.id) }
    }

    @Test
    fun `concluir tarefa cancela lembrete de prazo via completeAndCascade`() = runTest {
        val task = activeTask(status = TaskStatus.DOING)
        val done = task.transitionTo(TaskStatus.DONE)
        coEvery { repository.completeAndCascade(task.id) } returns done

        useCase(task.id, TaskStatus.DONE)

        verify(exactly = 1) { deadlineScheduler.cancel(task.id) }
        verify(exactly = 0) { deadlineScheduler.schedule(any(), any()) }
    }

    @Test
    fun `reabrir DONE com prazo futuro reagenda lembrete via reopenAndCascade`() = runTest {
        val taskDone = activeTask(status = TaskStatus.DOING).transitionTo(TaskStatus.DONE)
        val reopened = taskDone.transitionTo(TaskStatus.DOING)
        // findById chamado pelo caso de uso para detectar que a tarefa
        // está em DONE — então usa reopenAndCascade.
        coEvery { repository.findById(taskDone.id) } returns taskDone
        coEvery { repository.reopenAndCascade(taskDone.id, TaskStatus.DOING) } returns reopened

        useCase(taskDone.id, TaskStatus.DOING)

        verify(exactly = 1) {
            deadlineScheduler.schedule(
                taskId = taskDone.id,
                triggerAt = match {
                    it == reopened.dueDate?.minus(DeadlineNotificationScheduler.REMINDER_LEAD)
                },
            )
        }
    }

    @Test
    fun `transicao invalida do repositorio propaga InvalidStateTransitionException`() = runTest {
        // transitionTo no domínio proíbe DOING -> TODO em
        // algumas cadeias — mas a fonte da verdade aqui é o
        // repositório. Aqui mockamos uma exceção para cobrir o
        // caminho de propagação.
        val task = activeTask(status = TaskStatus.DOING)
        coEvery { repository.findById(task.id) } returns task
        coEvery { repository.changeStatus(task.id, TaskStatus.TODO) } throws InvalidStateTransitionException(
            "Transição de status inválida: DOING -> TODO",
        )

        val ex = assertThrows(InvalidStateTransitionException::class.java) {
            kotlinx.coroutines.runBlocking { useCase(task.id, TaskStatus.TODO) }
        }
        assertThat(ex.message).contains("DOING")
        verify(exactly = 0) { deadlineScheduler.schedule(any(), any()) }
        verify(exactly = 0) { deadlineScheduler.cancel(any()) }
    }
}
