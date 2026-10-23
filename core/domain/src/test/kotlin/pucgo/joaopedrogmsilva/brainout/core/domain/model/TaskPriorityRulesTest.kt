// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.error.BusinessRuleException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TaskPriorityChangeForbiddenException
import java.time.Instant

/**
 * Testes da regra de negócio **RN02** (E2.4) — prioridade obrigatória
 * e imutável em tarefas concluídas.
 *
 * Cobre:
 * - Códigos 0..4 são todos válidos (paridade com [TaskPriority.VALID_RANGE]).
 * - Alteração de prioridade em tarefas ativas (TODO/DOING) é
 *   permitida.
 * - Alteração de prioridade em tarefas concluídas (DONE) é
 *   bloqueada com [TaskPriorityChangeForbiddenException] (subclasse
 *   de [BusinessRuleException] — tratada como RN).
 *
 * Onde a regra é aplicada:
 * - no modelo, por [Task.changePriority] — o único caminho de
 *   domínio para mexer na prioridade;
 * - no atalho que o `data class` não bloqueia (`copy`), pelo
 *   [pucgo.joaopedrogmsilva.brainout.core.domain.usecase.UpdateTaskUseCase],
 *   que relê o registro persistido antes de gravar. O cenário do
 *   atalho vive em `UpdateTaskUseCaseTest`, junto do caso de uso que
 *   o barra.
 */
class TaskPriorityRulesTest {
    private val createdAt: Instant = Instant.parse("2026-09-21T10:00:00Z")

    private fun active(
        status: TaskStatus = TaskStatus.TODO,
        priority: TaskPriority = TaskPriority.MEDIUM,
    ): Task =
        Task(
            id = "t-${status.name.lowercase()}",
            projectId = "p-1",
            title = "Tarefa",
            priority = priority,
            status = status,
            assigneeId = null,
            dueDate = null,
            createdAt = createdAt,
        )

    @Test
    fun `every priority code from 0 to 4 is valid via fromCode`() {
        for (code in 0..4) {
            assertThat(TaskPriority.fromCode(code).priorityCode).isEqualTo(code)
        }
    }

    @Test
    fun `change priority from TODO is allowed`() {
        val task = active(status = TaskStatus.TODO, priority = TaskPriority.LOW)
        val updated = task.changePriority(TaskPriority.HIGH)
        assertThat(updated.priority).isEqualTo(TaskPriority.HIGH)
        assertThat(updated.status).isEqualTo(TaskStatus.TODO) // status preservado
    }

    @Test
    fun `change priority from DOING is allowed`() {
        val task = active(status = TaskStatus.DOING, priority = TaskPriority.MEDIUM)
        val updated = task.changePriority(TaskPriority.CRITICAL)
        assertThat(updated.priority).isEqualTo(TaskPriority.CRITICAL)
        assertThat(updated.status).isEqualTo(TaskStatus.DOING)
    }

    @Test
    fun `change priority from DONE is blocked with TaskPriorityChangeForbiddenException`() {
        val task = active(status = TaskStatus.DONE, priority = TaskPriority.HIGH)
        val ex =
            kotlin
                .runCatching {
                    task.changePriority(TaskPriority.LOW)
                }.exceptionOrNull()

        // RN02 — bloqueio específico para que a UI/VM possa distinguir
        // RN02 do RN01 (limite de tarefas) se necessário.
        assertThat(ex).isInstanceOf(TaskPriorityChangeForbiddenException::class.java)
        // E ainda é BusinessRuleException, mantendo a hierarquia
        // comum para captura agregada.
        assertThat(ex).isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `change priority to same value on DONE is still blocked (status check precedes no-op)`() {
        val task = active(status = TaskStatus.DONE, priority = TaskPriority.URGENT)
        val ex =
            kotlin
                .runCatching {
                    task.changePriority(TaskPriority.URGENT)
                }.exceptionOrNull()
        // A regra RN02 é absoluta: não importa se a prioridade é a
        // mesma — tarefa concluída não pode MEXER na prioridade, nem
        // para no-op.
        assertThat(ex).isInstanceOf(TaskPriorityChangeForbiddenException::class.java)
    }

    @Test
    fun `requer valid priority throws for out of range`() {
        // Cobertura extra: o mesmo helper estático deve barrar 5 e -1.
        val ex =
            kotlin
                .runCatching {
                    Task.requireValidPriority(TaskPriority.fromCode(4))
                }.exceptionOrNull()
        assertThat(ex).isNull() // 4 é válido

        // -1 não existe como enum constant; usamos fromCode para o teste.
        assertThat(kotlin.runCatching { TaskPriority.fromCode(-1) }.exceptionOrNull())
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThat(kotlin.runCatching { TaskPriority.fromCode(5) }.exceptionOrNull())
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `constructor completedAt em status ativo eh rejeitado`() {
        // RN03 — coerência: completedAt só faz sentido com DONE.
        val ex =
            kotlin
                .runCatching {
                    Task(
                        id = "t-x",
                        projectId = "p-x",
                        title = "Inválido",
                        priority = TaskPriority.LOW,
                        status = TaskStatus.TODO,
                        assigneeId = null,
                        dueDate = null,
                        createdAt = createdAt,
                        completedAt = Instant.parse("2026-12-01T00:00:00Z"),
                    )
                }.exceptionOrNull()
        assertThat(ex).isInstanceOf(InvalidModelException::class.java)
    }
}
