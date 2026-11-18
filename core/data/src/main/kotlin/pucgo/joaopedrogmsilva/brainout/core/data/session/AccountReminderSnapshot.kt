// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.session

import kotlinx.coroutines.flow.first
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.TaskDao
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import javax.inject.Inject

class AccountReminderSnapshot
    @Inject
    constructor(
        private val taskDao: TaskDao,
        private val access: AccountAccess,
    ) {
        suspend fun tasks(ownerId: String): List<Task> {
            access.requireOwner(ownerId)
            return taskDao.observeAllForOwner(ownerId).first().map { it.toDomain() }
        }
    }
