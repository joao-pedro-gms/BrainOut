// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.entity

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus

/**
 * Testes unitários do mapeamento Entity ↔ Domain.
 *
 * Garantem que o round-trip preserva todos os campos e que os
 * discriminadores textuais (status, priorityCode) sobrevivem a upgrades
 * do app.
 */
class EntityMappingTest {

    @Test
    fun `project entity round trip preserves nullable description`() {
        val withDesc = Project(
            id = "p-1",
            name = "Projeto",
            description = "Desc",
            ownerId = "u-1",
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            isCompleted = true,
        )
        assertThat(ProjectEntity.fromDomain(withDesc).toDomain()).isEqualTo(withDesc)

        val withoutDesc = withDesc.copy(description = null, isCompleted = false)
        assertThat(ProjectEntity.fromDomain(withoutDesc).toDomain()).isEqualTo(withoutDesc)
    }

    @Test
    fun `task entity round trip uses TaskPriority fromCodeOrThrow`() {
        val task = Task(
            id = "t-1",
            projectId = "p-1",
            title = "Fazer algo",
            priority = TaskPriority.CRITICAL,
            status = TaskStatus.DOING,
            assigneeId = "u-2",
            dueDate = Instant.parse("2026-12-31T23:59:59Z"),
            createdAt = Instant.parse("2026-01-15T10:00:00Z"),
        )

        val entity = TaskEntity.fromDomain(task)
        // Persistimos o priorityCode (4) e o name do status ("DOING").
        assertThat(entity.priorityCode).isEqualTo(TaskPriority.CRITICAL.priorityCode)
        assertThat(entity.status).isEqualTo("DOING")

        val back = entity.toDomain()
        assertThat(back).isEqualTo(task)
    }

    @Test
    fun `task entity round trip preserves null assignee and dueDate`() {
        val task = Task(
            id = "t-2",
            projectId = "p-1",
            title = "Sem responsável",
            priority = TaskPriority.LOW,
            status = TaskStatus.TODO,
            assigneeId = null,
            dueDate = null,
            createdAt = Instant.parse("2026-02-01T08:00:00Z"),
        )

        assertThat(TaskEntity.fromDomain(task).toDomain()).isEqualTo(task)
    }

    @Test
    fun `task entity round trip preserves completedAt for DONE tasks (RN03)`() {
        val completion = Instant.parse("2026-09-21T11:30:00Z")
        val task = Task(
            id = "t-done",
            projectId = "p-1",
            title = "Concluída",
            priority = TaskPriority.HIGH,
            status = TaskStatus.DONE,
            assigneeId = "u-1",
            dueDate = null,
            createdAt = Instant.parse("2026-09-20T10:00:00Z"),
            completedAt = completion,
        )

        val entity = TaskEntity.fromDomain(task)
        assertThat(entity.completedAt).isEqualTo(completion)

        val back = entity.toDomain()
        assertThat(back).isEqualTo(task)
        assertThat(back.completedAt).isEqualTo(completion)
    }

    @Test
    fun `tag entity round trip preserves color and owner`() {
        val tag = Tag(
            id = "tag-1",
            ownerId = "u-1",
            name = "Urgente",
            color = "#FF0000",
            createdAt = Instant.parse("2026-01-10T00:00:00Z"),
        )

        assertThat(TagEntity.fromDomain(tag).toDomain()).isEqualTo(tag)
    }
}
