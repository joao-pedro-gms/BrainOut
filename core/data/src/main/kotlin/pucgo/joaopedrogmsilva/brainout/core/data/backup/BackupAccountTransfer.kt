// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.backup

import pucgo.joaopedrogmsilva.brainout.core.domain.error.BackupRestoreException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.BackupSummary
import java.time.Instant
import java.util.UUID

/** Validação de relações antes de qualquer substituição dos dados locais. */
internal fun BackupPayload.validateAccounts() {
    fun unique(ids: List<String>) = ids.none { it.isBlank() } && ids.toSet().size == ids.size
    if (!unique(users.map { it.id }) ||
        !unique(projects.map { it.id }) ||
        !unique(tasks.map { it.id }) ||
        !unique(tags.map { it.id })
    ) {
        throw BackupRestoreException("Identificadores inválidos ou duplicados")
    }
    val owners = users.map { it.id }.toSet()
    val projectsById = projects.associateBy { it.id }
    val tagsById = tags.associateBy { it.id }
    val orphanOwner = projects.any { it.ownerId !in owners } || tags.any { it.ownerId !in owners }
    val orphanTask = tasks.any { it.projectId !in projectsById }
    val invalidLink =
        projectTags.any { link ->
            val project = projectsById[link.projectId]
            val tag = tagsById[link.tagId]
            project == null || tag == null || project.ownerId != tag.ownerId
        }
    if (orphanOwner || orphanTask || invalidLink) throw BackupRestoreException("Referências inválidas no backup")
    projects.forEach { it.toEntity().toDomain() }
    tasks.forEach { it.toEntity().toDomain() }
    tags.forEach { it.toEntity().toDomain() }
}

internal fun BackupPayload.forAccount(source: String): BackupPayload {
    val user = users.singleOrNull { it.id == source } ?: throw BackupRestoreException("Conta ausente do arquivo")
    val ownedProjects = projects.filter { it.ownerId == source }
    val projectIds = ownedProjects.map { it.id }.toSet()
    val ownedTags = tags.filter { it.ownerId == source }
    val tagIds = ownedTags.map { it.id }.toSet()
    return copy(
        users = listOf(user),
        projects = ownedProjects,
        tasks = tasks.filter { it.projectId in projectIds },
        tags = ownedTags,
        projectTags = projectTags.filter { it.projectId in projectIds && it.tagId in tagIds },
    )
}

internal fun BackupPayload.remapTo(owner: String): BackupPayload {
    val projectIds = projects.associate { it.id to UUID.randomUUID().toString() }
    val tagIds = tags.associate { it.id to UUID.randomUUID().toString() }
    return copy(
        projects = projects.map { it.copy(id = projectIds.getValue(it.id), ownerId = owner) },
        tasks =
            tasks.map {
                it.copy(
                    id = UUID.randomUUID().toString(),
                    projectId =
                        projectIds.getValue(
                            it.projectId,
                        ),
                    assigneeId =
                        it.assigneeId?.let {
                            owner
                        },
                )
            },
        tags = tags.map { it.copy(id = tagIds.getValue(it.id), ownerId = owner) },
        projectTags =
            projectTags.map {
                BackupProjectTag(
                    projectIds.getValue(it.projectId),
                    tagIds.getValue(it.tagId),
                )
            },
    )
}

internal fun BackupPayload.summary(now: Instant): BackupSummary =
    BackupSummary(now, users.size, projects.size, tasks.size, tags.size)
