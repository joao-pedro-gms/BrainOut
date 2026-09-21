// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository

/**
 * Testes do caso de uso [DeleteTaskUseCase]: remoção deve sempre
 * cancelar o lembrete de prazo associado (marco E3.6), mesmo quando
 * a tarefa não tinha agendamento (cancel é no-op).
 */
class DeleteTaskUseCaseTest {

    private val repository: TaskRepository = mockk(relaxed = true)
    private val deadlineScheduler: DeadlineNotificationScheduler = mockk(relaxed = true)
    private val useCase = DeleteTaskUseCase(repository, deadlineScheduler)

    @Test
    fun `delete remove tarefa e cancela lembrete`() = runTest {
        useCase("t1")

        coVerify(exactly = 1) { repository.delete("t1") }
        verify(exactly = 1) { deadlineScheduler.cancel("t1") }
    }
}
