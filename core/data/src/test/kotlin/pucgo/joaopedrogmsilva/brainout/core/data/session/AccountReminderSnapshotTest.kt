// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.session

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.data.local.RoomLocalTransaction
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.sampleProject
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.sampleTask
import pucgo.joaopedrogmsilva.brainout.core.data.local.inMemoryDatabase
import pucgo.joaopedrogmsilva.brainout.core.data.repository.testSession
import pucgo.joaopedrogmsilva.brainout.core.domain.error.OwnershipException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class AccountReminderSnapshotTest {
    @Test
    fun `snapshot consulta somente tarefas dos projetos da conta ativa`() =
        runTest {
            val database = inMemoryDatabase()
            try {
                database.projectDao().insert(sampleProject(id = "p1", ownerId = "u1"))
                database.projectDao().insert(sampleProject(id = "p2", ownerId = "u2"))
                database.taskDao().insert(sampleTask(id = "t1", projectId = "p1"))
                database.taskDao().insert(sampleTask(id = "t2", projectId = "p2"))
                val access = AccountAccess(testSession("u1"), RoomLocalTransaction(database))
                val snapshot = AccountReminderSnapshot(database.taskDao(), access)

                assertThat(snapshot.tasks("u1").map { it.id }).containsExactly("t1")
            } finally {
                database.close()
            }
        }

    @Test
    fun `snapshot rejeita conta que nao corresponde a sessao ativa`() =
        runTest {
            val database = inMemoryDatabase()
            try {
                val access = AccountAccess(testSession("u2"), RoomLocalTransaction(database))
                val snapshot = AccountReminderSnapshot(database.taskDao(), access)

                val error = runCatching { snapshot.tasks("u1") }.exceptionOrNull()

                assertThat(error).isInstanceOf(OwnershipException::class.java)
            } finally {
                database.close()
            }
        }
}
