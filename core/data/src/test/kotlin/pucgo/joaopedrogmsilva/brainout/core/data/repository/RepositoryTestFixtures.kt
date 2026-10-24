// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import java.time.Instant

// Modelos de domínio de exemplo dos testes de repositório.
//
// As fábricas estavam espalhadas por três suítes do pacote, e a de tag
// estava **duplicada**: as duas versões declaravam os mesmos campos com
// os mesmos valores padrão. Aqui elas moram uma vez e cada suíte
// declara só o que o seu cenário varia.

/** Um projeto de exemplo. */
internal fun sampleProject(
    id: String = "p1",
    name: String = "Projeto",
    ownerId: String = "u1",
    description: String? = null,
    createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    isCompleted: Boolean = false,
): Project =
    Project(
        id = id,
        name = name,
        description = description,
        ownerId = ownerId,
        createdAt = createdAt,
        isCompleted = isCompleted,
    )

/** Uma tag de exemplo. */
internal fun sampleTag(
    id: String = "t1",
    ownerId: String = "u1",
    name: String = "Urgente",
    color: String = "#6750A4",
    createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
): Tag =
    Tag(
        id = id,
        ownerId = ownerId,
        name = name,
        color = color,
        createdAt = createdAt,
    )

/** Uma tarefa de exemplo. */
internal fun sampleTask(
    projectId: String = "p1",
    title: String = "Tarefa",
    priority: TaskPriority = TaskPriority.MEDIUM,
    status: TaskStatus = TaskStatus.TODO,
    createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
): Task =
    Task.create(
        projectId = projectId,
        title = title,
        priority = priority,
        status = status,
        now = createdAt,
    )
