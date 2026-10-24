// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.dao

import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TagEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TaskEntity
import java.time.Instant

// Entidades de exemplo dos testes de DAO.
//
// As fábricas estavam copiadas em três suítes — os mesmos campos, os
// mesmos valores padrão, três listas para manter em dia. Aqui elas
// moram uma vez e as suítes passam a declarar só o que o teste
// realmente varia (o id, o status, o carimbo de conclusão).

/** Um projeto de exemplo. */
internal fun sampleProject(
    id: String = "p-1",
    ownerId: String = "u-1",
    name: String = "Projeto $id",
    description: String? = null,
    createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    isCompleted: Boolean = false,
): ProjectEntity =
    ProjectEntity(
        id = id,
        ownerId = ownerId,
        name = name,
        description = description,
        createdAt = createdAt,
        isCompleted = isCompleted,
    )

/** Uma tarefa de exemplo. */
internal fun sampleTask(
    id: String = "t-1",
    projectId: String = "p-1",
    title: String = "Tarefa $id",
    priorityCode: Int = 1,
    status: String = "TODO",
    assigneeId: String? = null,
    dueDate: Instant? = null,
    createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    completedAt: Instant? = null,
): TaskEntity =
    TaskEntity(
        id = id,
        projectId = projectId,
        title = title,
        priorityCode = priorityCode,
        status = status,
        assigneeId = assigneeId,
        dueDate = dueDate,
        createdAt = createdAt,
        completedAt = completedAt,
    )

/** Uma tag de exemplo. */
internal fun sampleTag(
    id: String = "tag-1",
    ownerId: String = "u-1",
    name: String = "tag $id",
    color: String = "#FF0000",
    createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
): TagEntity =
    TagEntity(
        id = id,
        ownerId = ownerId,
        name = name,
        color = color,
        createdAt = createdAt,
    )
