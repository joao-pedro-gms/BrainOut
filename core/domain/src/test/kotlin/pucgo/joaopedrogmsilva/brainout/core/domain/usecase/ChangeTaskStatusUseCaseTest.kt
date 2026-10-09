// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidStateTransitionException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TaskNotFoundException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TaskPriorityChangeForbiddenException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository
import java.time.Instant

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
    ): Task =
        Task.create(
            projectId = "p1",
            title = "Tarefa",
            status = status,
            dueDate = dueDate,
        )

    @Test
    fun `transicao intermediaria DOING para TODO usa changeStatus`() =
        runTest {
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
    fun `transicao para DONE usa completeAndCascade para cascata RN03`() =
        runTest {
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
    fun `concluir tarefa cancela lembrete de prazo via completeAndCascade`() =
        runTest {
            val task = activeTask(status = TaskStatus.DOING)
            val done = task.transitionTo(TaskStatus.DONE)
            coEvery { repository.completeAndCascade(task.id) } returns done

            useCase(task.id, TaskStatus.DONE)

            verify(exactly = 1) { deadlineScheduler.cancel(task.id) }
            verify(exactly = 0) { deadlineScheduler.schedule(any(), any()) }
        }

    @Test
    fun `reabrir DONE com prazo futuro reagenda lembrete via reopenAndCascade`() =
        runTest {
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
                    triggerAt =
                        match {
                            it == reopened.dueDate?.minus(DeadlineNotificationScheduler.REMINDER_LEAD)
                        },
                )
            }
        }

    @Test
    fun `transicao invalida do repositorio propaga InvalidStateTransitionException`() =
        runTest {
            // transitionTo no domínio proíbe DOING -> TODO em
            // algumas cadeias — mas a fonte da verdade aqui é o
            // repositório. Aqui mockamos uma exceção para cobrir o
            // caminho de propagação.
            val task = activeTask(status = TaskStatus.DOING)
            coEvery { repository.findById(task.id) } returns task
            coEvery { repository.changeStatus(task.id, TaskStatus.TODO) } throws
                InvalidStateTransitionException(
                    "Transição de status inválida: DOING -> TODO",
                )

            val ex =
                assertThrows(InvalidStateTransitionException::class.java) {
                    kotlinx.coroutines.runBlocking { useCase(task.id, TaskStatus.TODO) }
                }
            assertThat(ex.message).contains("DOING")
            verify(exactly = 0) { deadlineScheduler.schedule(any(), any()) }
            verify(exactly = 0) { deadlineScheduler.cancel(any()) }
        }

    @Test
    fun `task inexistente lanca TaskNotFoundException (R7 idempotencia fronteira)`() =
        runTest {
            // Caminho de reabertura ou transição intermediária:
            // findById retorna null. Antes era `error(...)` que lançava
            // IllegalStateException cru (P0-8). Agora é a exceção de
            // domínio, capturável pela UI sem hack.
            coEvery { repository.findById("ausente") } returns null

            val ex =
                assertThrows(TaskNotFoundException::class.java) {
                    kotlinx.coroutines.runBlocking { useCase("ausente", TaskStatus.DOING) }
                }
            assertThat(ex.message).contains("ausente")
            coVerify(exactly = 0) { repository.completeAndCascade(any()) }
            coVerify(exactly = 0) { repository.changeStatus(any(), any()) }
            coVerify(exactly = 0) { repository.reopenAndCascade(any(), any()) }
            verify(exactly = 0) { deadlineScheduler.schedule(any(), any()) }
            verify(exactly = 0) { deadlineScheduler.cancel(any()) }
        }

    @Test
    fun `RN02 update integral bloqueia tarefa persistida em DONE`() =
        runTest {
            // Regressão do bypass público: o overload `invoke(Task)`
            // escrevia direto via `repository.update` sem consultar o
            // registro persistido, permitindo alterar a prioridade de
            // uma tarefa concluída via `Task.copy`.
            val persistida =
                Task.create(
                    projectId = "p1",
                    title = "Tarefa",
                    status = TaskStatus.DONE,
                    priority = TaskPriority.LOW,
                )
            val tentativa = persistida.copy(priority = TaskPriority.CRITICAL)
            coEvery { repository.findById(persistida.id) } returns persistida

            val ex =
                assertThrows(TaskPriorityChangeForbiddenException::class.java) {
                    kotlinx.coroutines.runBlocking { useCase(tentativa) }
                }
            assertThat(ex.message).contains("RN02")
            coVerify(exactly = 0) { repository.update(any()) }
            verify(exactly = 0) { deadlineScheduler.schedule(any(), any()) }
            verify(exactly = 0) { deadlineScheduler.cancel(any()) }
        }

    @Test
    fun `update integral de tarefa ativa persiste e reconcilia lembrete`() =
        runTest {
            // Contraprova do guard: tarefa ativa segue o caminho normal
            // de escrita (o guard só rejeita o estado persistido DONE).
            val persistida =
                Task.create(
                    projectId = "p1",
                    title = "Tarefa",
                    status = TaskStatus.DOING,
                    priority = TaskPriority.LOW,
                )
            val renamed = persistida.rename("Título novo")
            coEvery { repository.findById(persistida.id) } returns persistida
            coEvery { repository.update(any()) } answers { firstArg() }

            val result = useCase(renamed)

            assertThat(result).isEqualTo(renamed)
            coVerify(exactly = 1) { repository.update(renamed) }
            // Sem prazo: nenhum lembrete agendado.
            verify(exactly = 0) { deadlineScheduler.schedule(any(), any()) }
        }

    @Test
    fun `update integral de tarefa inexistente ainda escreve (primeira gravacao)`() =
        runTest {
            // `findById == null` não pode ser tratado como violação de
            // RN02 — assim como no UpdateTaskUseCase, a ausência do
            // registro apenas significa "sem estado persistido para
            // revalidar" e a escrita segue para o repositório decidir.
            val nova = Task.create(projectId = "p1", title = "Nova", status = TaskStatus.TODO)
            coEvery { repository.findById(nova.id) } returns null
            coEvery { repository.update(any()) } answers { firstArg() }

            val result = useCase(nova)

            assertThat(result).isEqualTo(nova)
            coVerify(exactly = 1) { repository.update(nova) }
        }
}
