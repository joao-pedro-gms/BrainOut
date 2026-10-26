// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.session

import pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import javax.inject.Inject

class AccountReminderSnapshot
    @Inject
    constructor(
        private val database: BrainOutDatabase,
    ) {
        suspend fun tasks(ownerId: String): List<Task> {
            val projects =
                database
                    .backupDao()
                    .getAllProjects()
                    .filter { it.ownerId == ownerId }
                    .map { it.id }
                    .toSet()
            return database
                .backupDao()
                .getAllTasks()
                .filter { it.projectId in projects }
                .map { it.toDomain() }
        }
    }
