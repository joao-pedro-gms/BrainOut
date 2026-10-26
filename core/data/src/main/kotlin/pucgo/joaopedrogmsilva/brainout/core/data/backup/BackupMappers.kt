// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.backup

import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectTagCrossRef
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TagEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TaskEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.UserEntity
import java.time.Instant

// Conversão entre as entidades do Room e o formato do backup.
//
// O payload e a entidade guardam os **mesmos** campos; o que muda é a
// representação da data (epoch millis no arquivo, `Instant` no banco).
// A conversão estava escrita dentro do `BackupRepositoryImpl`, nos dois
// sentidos — o que fazia a lista de campos aparecer **duas vezes**: um
// campo novo no payload podia ser mapeado num sentido e esquecido no
// outro, e o dado se perdia em silêncio.
//
// Aqui cada conversão mora uma vez, e o repositório fica só com o I/O.

/** Identidade descritiva; nunca transporta a senha. */
internal fun UserEntity.toBackup(): BackupUser =
    BackupUser(id = id, username = username, createdAtEpochMillis = createdAt.toEpochMilli())

/** Entidade → payload. */
internal fun ProjectEntity.toBackup(): BackupProject =
    BackupProject(
        id = id,
        ownerId = ownerId,
        name = name,
        description = description,
        createdAtEpochMillis = createdAt.toEpochMilli(),
        isCompleted = isCompleted,
    )

/** Payload → entidade. */
internal fun BackupProject.toEntity(): ProjectEntity =
    ProjectEntity(
        id = id,
        ownerId = ownerId,
        name = name,
        description = description,
        createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
        isCompleted = isCompleted,
    )

/** Entidade → payload. */
internal fun TaskEntity.toBackup(): BackupTask =
    BackupTask(
        id = id,
        projectId = projectId,
        title = title,
        priorityCode = priorityCode,
        status = status,
        assigneeId = assigneeId,
        dueDateEpochMillis = dueDate?.toEpochMilli(),
        createdAtEpochMillis = createdAt.toEpochMilli(),
        completedAtEpochMillis = completedAt?.toEpochMilli(),
    )

/** Payload → entidade. */
internal fun BackupTask.toEntity(): TaskEntity =
    TaskEntity(
        id = id,
        projectId = projectId,
        title = title,
        priorityCode = priorityCode,
        status = status,
        assigneeId = assigneeId,
        dueDate = dueDateEpochMillis?.let(Instant::ofEpochMilli),
        createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
        completedAt = completedAtEpochMillis?.let(Instant::ofEpochMilli),
    )

/** Entidade → payload. */
internal fun TagEntity.toBackup(): BackupTag =
    BackupTag(
        id = id,
        ownerId = ownerId,
        name = name,
        color = color,
        createdAtEpochMillis = createdAt.toEpochMilli(),
    )

/** Payload → entidade. */
internal fun BackupTag.toEntity(): TagEntity =
    TagEntity(
        id = id,
        ownerId = ownerId,
        name = name,
        color = color,
        createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    )

/** Entidade → payload. */
internal fun ProjectTagCrossRef.toBackup(): BackupProjectTag = BackupProjectTag(projectId = project_id, tagId = tag_id)

/** Payload → entidade. */
internal fun BackupProjectTag.toEntity(): ProjectTagCrossRef =
    ProjectTagCrossRef(project_id = projectId, tag_id = tagId)
