// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.data.session.AccountReminderSnapshot
import pucgo.joaopedrogmsilva.brainout.core.data.session.SessionStore
import pucgo.joaopedrogmsilva.brainout.core.domain.error.OwnershipException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler
import java.io.IOException
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class AccountReminderCoordinatorTest {
    @Test
    fun `falha transitoria e trocas de conta nao encerram a reconciliacao`() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val executor = SynchronousExecutor()
            WorkManagerTestInitHelper.initializeTestWorkManager(
                context,
                Configuration
                    .Builder()
                    .setExecutor(executor)
                    .setTaskExecutor(executor)
                    .build(),
            )
            val current = MutableStateFlow<String?>("u1")
            val session = mockk<SessionStore>()
            coEvery { session.observeUserId() } returns current
            coEvery { session.withUser<Unit>(any(), any()) } coAnswers {
                if (firstArg<String>() != current.value) throw OwnershipException()
                secondArg<suspend () -> Unit>().invoke()
            }
            val snapshot = mockk<AccountReminderSnapshot>()
            val first = Task.create("p1", "Primeira", dueDate = Instant.now().plusSeconds(7200)).copy(id = "t1")
            val second = first.copy(id = "t2")
            coEvery { snapshot.tasks("u1") } throws IOException("fixture failure") andThen listOf(first)
            coEvery { snapshot.tasks("u2") } returns listOf(second)
            val scheduled = mutableListOf<String>()
            val scheduler =
                object : DeadlineNotificationScheduler {
                    override fun schedule(
                        taskId: String,
                        triggerAt: Instant,
                    ) {
                        scheduled += taskId
                    }

                    override fun cancel(taskId: String) = Unit
                }
            AccountReminderCoordinator(context, session, snapshot, scheduler).startIn(backgroundScope)
            runCurrent()
            advanceTimeBy(500)
            runCurrent()
            assertThat(scheduled).containsExactly("t1")
            current.value = null
            runCurrent()
            current.value = "u2"
            runCurrent()
            assertThat(scheduled).containsExactly("t1", "t2").inOrder()
            coVerify(exactly = 2) { snapshot.tasks("u1") }
        }
}
