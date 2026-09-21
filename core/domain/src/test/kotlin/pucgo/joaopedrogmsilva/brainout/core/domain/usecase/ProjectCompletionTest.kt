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
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidStateTransitionException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository

/**
 * Testes da regra de negócio **RN03** (E2.5) — conclusão cascata.
 *
 * Cobre:
 * - Concluir a **última** tarefa ativa dispara
 *   [TaskRepository.completeAndCascade] (não `changeStatus` puro).
 * - Reabrir uma tarefa (DONE → DOING) dispara
 *   [TaskRepository.reopenAndCascade] para desmarcar o projeto.
 * - Transição para DONE inválida (a camada repository lança
 *   [InvalidStateTransitionException]) é propagada sem efeitos no
 *   agendamento de lembretes.
 * - TODO → DOING (transição sem cascata) usa o caminho simples de
 *   `changeStatus`.
 */
class ProjectCompletionTest {

    private val repository: TaskRepository = mockk()
    private val deadlineScheduler: DeadlineNotificationScheduler = mockk(relaxed = true)
    private val useCase = ChangeTaskStatusUseCase(repository, deadlineScheduler)

    private fun task(
        id: String = "t-1",
        status: TaskStatus = TaskStatus.DOING,
    ): Task = Task(
        id = id,
        projectId = "p-1",
        title = "Tarefa",
        priority = TaskPriority.MEDIUM,
        status = status,
        assigneeId = null,
        dueDate = null,
        createdAt = Instant.parse("2026-09-21T10:00:00Z"),
    )

    @Test
    fun `concluir tarefa ultima ativa delega para completeAndCascade`() = runTest {
        val current = task(id = "last", status = TaskStatus.DOING)
        val done = current.transitionTo(TaskStatus.DONE)
        coEvery { repository.completeAndCascade("last") } returns done

        val result = useCase("last", TaskStatus.DONE)

        assertThat(result).isEqualTo(done)
        coVerify(exactly = 1) { repository.completeAndCascade("last") }
        coVerify(exactly = 0) { repository.changeStatus(any(), any()) }
        verify(exactly = 1) { deadlineScheduler.cancel("last") }
    }

    @Test
    fun `concluir tarefa penultima tambem passa pela cascata`() = runTest {
        // Toda transição para DONE passa por completeAndCascade —
        // a contagem de "última ativa" é decidida dentro do
        // repositório com base no estado do banco.
        val current = task(id = "penultimate", status = TaskStatus.DOING)
        val done = current.transitionTo(TaskStatus.DONE)
        coEvery { repository.completeAndCascade("penultimate") } returns done

        val result = useCase("penultimate", TaskStatus.DONE)

        assertThat(result).isEqualTo(done)
        coVerify(exactly = 1) { repository.completeAndCascade("penultimate") }
        coVerify(exactly = 0) { repository.changeStatus(any(), any()) }
    }

    @Test
    fun `reabrir DONE para DOING delega para reopenAndCascade`() = runTest {
        val current = task(id = "reopen-me", status = TaskStatus.DONE)
        coEvery { repository.findById("reopen-me") } returns current
        val reopened = current.transitionTo(TaskStatus.DOING)
        coEvery { repository.reopenAndCascade("reopen-me", TaskStatus.DOING) } returns reopened

        val result = useCase("reopen-me", TaskStatus.DOING)

        assertThat(result).isEqualTo(reopened)
        coVerify(exactly = 1) { repository.reopenAndCascade("reopen-me", TaskStatus.DOING) }
        coVerify(exactly = 0) { repository.changeStatus(any(), any()) }
    }

    @Test
    fun `transicao intermedia DOING para TODO nao passa pela cascata`() = runTest {
        // DOING -> TODO não muda a contagem de tarefas ativas;
        // RN03 não exige cascata.
        val current = task(id = "t", status = TaskStatus.DOING)
        val target = current.transitionTo(TaskStatus.TODO)
        coEvery { repository.findById("t") } returns current
        coEvery { repository.changeStatus("t", TaskStatus.TODO) } returns target

        val result = useCase("t", TaskStatus.TODO)

        assertThat(result).isEqualTo(target)
        coVerify(exactly = 1) { repository.changeStatus("t", TaskStatus.TODO) }
        coVerify(exactly = 0) { repository.completeAndCascade(any()) }
        coVerify(exactly = 0) { repository.reopenAndCascade(any(), any()) }
    }

    @Test
    fun `transicao TODO para DOING nao passa pela cascata`() = runTest {
        val current = task(id = "t", status = TaskStatus.TODO)
        val target = current.transitionTo(TaskStatus.DOING)
        coEvery { repository.findById("t") } returns current
        coEvery { repository.changeStatus("t", TaskStatus.DOING) } returns target

        val result = useCase("t", TaskStatus.DOING)

        assertThat(result).isEqualTo(target)
        coVerify(exactly = 1) { repository.changeStatus("t", TaskStatus.DOING) }
        coVerify(exactly = 0) { repository.completeAndCascade(any()) }
        coVerify(exactly = 0) { repository.reopenAndCascade(any(), any()) }
    }

    @Test
    fun `transicao invalida propaga sem agendar lembrete`() = runTest {
        // Cenário: tarefa em DONE, caller pede TODO.
        // O caso de uso roteia para reopenAndCascade (saída de
        // DONE), e a implementação do repositório lança a exceção
        // porque DONE → TODO não é permitido pela matriz.
        val current = task(status = TaskStatus.DONE)
        coEvery { repository.findById("t-1") } returns current
        coEvery { repository.reopenAndCascade("t-1", TaskStatus.TODO) } throws InvalidStateTransitionException(
            "Transição de status inválida: DONE -> TODO",
        )

        val ex = assertThrows(InvalidStateTransitionException::class.java) {
            runBlocking { useCase("t-1", TaskStatus.TODO) }
        }
        assertThat(ex.message).contains("DONE")
        verify(exactly = 0) { deadlineScheduler.schedule(any(), any()) }
        verify(exactly = 0) { deadlineScheduler.cancel(any()) }
    }
}
