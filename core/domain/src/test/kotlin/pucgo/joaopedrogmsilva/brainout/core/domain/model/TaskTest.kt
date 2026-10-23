// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidStateTransitionException
import java.time.Instant

/**
 * Testes de invariantes e transições de [Task].
 *
 * Cobre:
 * - título não vazio com até 200 caracteres;
 * - prioridade sempre no intervalo válido 0..4;
 * - matriz de transições de [TaskStatus];
 * - RN03 — carimbo de conclusão ([Task.completedAt]) ao entrar em
 *   [TaskStatus.DONE] e o seu apagamento na reabertura;
 * - imutabilidade (copy e métodos retornam novas instâncias).
 */
class TaskTest {
    private val fixedInstant: Instant = Instant.parse("2026-09-18T12:00:00Z")
    private val due: Instant = Instant.parse("2026-12-31T23:59:59Z")

    private fun sample(
        title: String = "Tarefa inicial",
        priority: TaskPriority = TaskPriority.MEDIUM,
        status: TaskStatus = TaskStatus.TODO,
        assigneeId: String? = null,
        dueDate: Instant? = null,
    ): Task =
        Task(
            id = "task-1",
            projectId = "project-1",
            title = title,
            priority = priority,
            status = status,
            assigneeId = assigneeId,
            dueDate = dueDate,
            createdAt = fixedInstant,
        )

    @Test
    fun `priority codes cover full 0 to 4 range`() {
        val codes = TaskPriority.entries.map { it.priorityCode }.sorted()

        assertThat(codes).isEqualTo(listOf(0, 1, 2, 3, 4))
        assertThat(TaskPriority.LOW.priorityCode).isEqualTo(0)
        assertThat(TaskPriority.CRITICAL.priorityCode).isEqualTo(4)
    }

    @Test
    fun `fromCode resolves every valid priority`() {
        for (code in 0..4) {
            val priority = TaskPriority.fromCode(code)
            assertThat(priority.priorityCode).isEqualTo(code)
        }
    }

    @Test
    fun `fromCode throws for out of range codes`() {
        kotlin.runCatching { TaskPriority.fromCode(-1) }.also {
            assertThat(it.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        }
        kotlin.runCatching { TaskPriority.fromCode(5) }.also {
            assertThat(it.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun `title with only spaces fails validation`() {
        val ex = kotlin.runCatching { Task.requireValidTitle("  ") }.exceptionOrNull()
        assertThat(ex).isInstanceOf(InvalidModelException::class.java)
    }

    @Test
    fun `title longer than max length fails validation`() {
        val tooLong = "t".repeat(Task.MAX_TITLE_LENGTH + 1)
        val ex = kotlin.runCatching { Task.requireValidTitle(tooLong) }.exceptionOrNull()
        assertThat(ex).isInstanceOf(InvalidModelException::class.java)
        assertThat(ex).hasMessageThat().contains("no máximo ${Task.MAX_TITLE_LENGTH}")
    }

    @Test
    fun `task constructor rejects empty title`() {
        kotlin.runCatching { sample(title = "") }.also {
            assertThat(it.exceptionOrNull()).isInstanceOf(InvalidModelException::class.java)
        }
    }

    @Test
    fun `task constructor rejects blank projectId`() {
        kotlin
            .runCatching {
                sample().copy(projectId = " ")
            }.also {
                assertThat(it.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
            }
    }

    @Test
    fun `transition TODO to DOING is allowed`() {
        val task = sample(status = TaskStatus.TODO)
        val updated = task.transitionTo(TaskStatus.DOING)

        assertThat(updated.status).isEqualTo(TaskStatus.DOING)
        assertThat(task.status).isEqualTo(TaskStatus.TODO) // imutabilidade
    }

    @Test
    fun `transition DOING to DONE is allowed`() {
        val task = sample(status = TaskStatus.DOING)
        val updated = task.transitionTo(TaskStatus.DONE)

        assertThat(updated.status).isEqualTo(TaskStatus.DONE)
    }

    @Test
    fun `transition DOING to TODO is allowed as partial reopen`() {
        val task = sample(status = TaskStatus.DOING)
        val updated = task.transitionTo(TaskStatus.TODO)

        assertThat(updated.status).isEqualTo(TaskStatus.TODO)
    }

    @Test
    fun `transition DONE to DOING is allowed as reopen`() {
        val task = sample(status = TaskStatus.DONE)
        val updated = task.transitionTo(TaskStatus.DOING)

        assertThat(updated.status).isEqualTo(TaskStatus.DOING)
    }

    @Test
    fun `transition DONE to TODO is forbidden`() {
        val task = sample(status = TaskStatus.DONE)
        val ex = kotlin.runCatching { task.transitionTo(TaskStatus.TODO) }.exceptionOrNull()

        assertThat(ex).isInstanceOf(InvalidStateTransitionException::class.java)
        assertThat(ex).hasMessageThat().contains("DONE -> TODO")
    }

    @Test
    fun `transition TODO to DONE is forbidden`() {
        val task = sample(status = TaskStatus.TODO)
        val ex = kotlin.runCatching { task.transitionTo(TaskStatus.DONE) }.exceptionOrNull()

        assertThat(ex).isInstanceOf(InvalidStateTransitionException::class.java)
    }

    @Test
    fun `transition to same status is a no-op`() {
        val task = sample(status = TaskStatus.DOING)
        val same = task.transitionTo(TaskStatus.DOING)

        assertThat(same).isEqualTo(task)
        assertThat(same).isSameInstanceAs(task)
    }

    /**
     * RN03 (E2.5) — o carimbo de conclusão.
     *
     * O instante é injetado em [Task.transitionTo] (como o `now` de
     * [Task.create]), então o teste afirma o valor exato em vez de um
     * intervalo — sem isso, RN03 só era verificável por aproximação.
     */
    @Test
    fun `transicao para DONE carimba completedAt`() {
        val task = sample(status = TaskStatus.DOING)

        val updated = task.transitionTo(TaskStatus.DONE, now = fixedInstant)

        assertThat(updated.completedAt).isEqualTo(fixedInstant)
        assertThat(task.completedAt).isNull()
    }

    @Test
    fun `reabertura de tarefa concluida limpa completedAt`() {
        val done = sample(status = TaskStatus.DOING).transitionTo(TaskStatus.DONE, now = fixedInstant)

        val reopened = done.transitionTo(TaskStatus.DOING)

        assertThat(reopened.status).isEqualTo(TaskStatus.DOING)
        assertThat(reopened.completedAt).isNull()
    }

    @Test
    fun `transicao que nao conclui mantem completedAt vazio`() {
        val task = sample(status = TaskStatus.TODO)

        val updated = task.transitionTo(TaskStatus.DOING)

        assertThat(updated.completedAt).isNull()
    }

    @Test
    fun `tarefa concluida legada sem completedAt eh aceita`() {
        // Migração: tarefas concluídas antes da coluna `completed_at`
        // seguem sem carimbo. O modelo aceita o registro como está —
        // inventar um instante aqui falsificaria a data de conclusão.
        val legada =
            Task(
                id = "task-legada",
                projectId = "project-1",
                title = "Tarefa antiga",
                priority = TaskPriority.MEDIUM,
                status = TaskStatus.DONE,
                assigneeId = null,
                dueDate = null,
                createdAt = fixedInstant,
            )

        assertThat(legada.completedAt).isNull()
    }

    @Test
    fun `completedAt em tarefa ativa eh rejeitado`() {
        val ex =
            kotlin
                .runCatching {
                    sample(status = TaskStatus.TODO).copy(completedAt = fixedInstant)
                }.exceptionOrNull()

        assertThat(ex).isInstanceOf(InvalidModelException::class.java)
    }

    @Test
    fun `changePriority validates range and returns new instance`() {
        val task = sample(priority = TaskPriority.LOW)
        val updated = task.changePriority(TaskPriority.HIGH)

        assertThat(updated.priority).isEqualTo(TaskPriority.HIGH)
        assertThat(task.priority).isEqualTo(TaskPriority.LOW)
    }

    @Test
    fun `changePriority to same value is a no-op`() {
        val task = sample(priority = TaskPriority.LOW)
        val same = task.changePriority(TaskPriority.LOW)
        assertThat(same).isEqualTo(task)
        assertThat(same).isSameInstanceAs(task)
    }

    /**
     * DEF-57 — o título é aparado antes de ser gravado.
     *
     * A validação já olhava o valor aparado; guardar o texto cru
     * deixava o banco com os espaços que o usuário digitou.
     */
    @Test
    fun `rename apara o titulo antes de gravar`() {
        val task = sample(title = "Old")
        val updated = task.rename("  New name  ")

        assertThat(updated.title).isEqualTo("New name")
    }

    @Test
    fun `rename para o mesmo titulo aparado e no-op`() {
        val task = sample(title = "Mesmo")
        val updated = task.rename("  Mesmo  ")

        assertThat(updated).isSameInstanceAs(task)
    }

    @Test
    fun `create apara o titulo`() {
        val task = Task.create(projectId = "p1", title = "  Compras  ")

        assertThat(task.title).isEqualTo("Compras")
    }

    @Test
    fun `rename rejects empty title`() {
        val task = sample()
        kotlin.runCatching { task.rename("   ") }.also {
            assertThat(it.exceptionOrNull()).isInstanceOf(InvalidModelException::class.java)
        }
    }

    @Test
    fun `Task create factory assigns UUID and defaults`() {
        val task =
            Task.create(
                projectId = "project-1",
                title = "Compras",
                priority = TaskPriority.HIGH,
                status = TaskStatus.DOING,
                assigneeId = "user-1",
                dueDate = due,
                now = fixedInstant,
            )

        assertThat(task.id).isNotEmpty()
        assertThat(task.projectId).isEqualTo("project-1")
        assertThat(task.title).isEqualTo("Compras")
        assertThat(task.priority).isEqualTo(TaskPriority.HIGH)
        assertThat(task.status).isEqualTo(TaskStatus.DOING)
        assertThat(task.assigneeId).isEqualTo("user-1")
        assertThat(task.dueDate).isEqualTo(due)
        assertThat(task.createdAt).isEqualTo(fixedInstant)
    }
}
