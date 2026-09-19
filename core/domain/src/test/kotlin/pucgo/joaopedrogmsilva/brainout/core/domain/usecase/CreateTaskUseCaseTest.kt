// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.error.ProjectTaskLimitReachedException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository

/**
 * Testes do caso de uso [CreateTaskUseCase] aplicando a regra
 * de negócio RN01 (limite de 50 tarefas ativas por projeto).
 *
 * Garante que:
 * - Quando o projeto está abaixo do limite, a tarefa é criada
 *   com `status = TODO` e `priority = MEDIUM` por padrão.
 * - Quando o projeto já atingiu [MAX_ACTIVE_TASKS_PER_PROJECT],
 *   a operação lança [ProjectTaskLimitReachedException] antes
 *   de qualquer escrita no repositório.
 */
class CreateTaskUseCaseTest {

    private val repository: TaskRepository = mockk()
    private val useCase = CreateTaskUseCase(repository)

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
}
