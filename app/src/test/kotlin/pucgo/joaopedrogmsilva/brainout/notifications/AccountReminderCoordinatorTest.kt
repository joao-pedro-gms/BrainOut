// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
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
    private fun initializeWorkManager(context: Context) {
        val executor = SynchronousExecutor()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration
                .Builder()
                .setExecutor(executor)
                .setTaskExecutor(executor)
                .build(),
        )
    }

    private fun coordinator(
        context: Context,
        current: MutableStateFlow<String?>,
    ): AccountReminderCoordinator {
        val session = mockk<SessionStore>()
        coEvery { session.observeUserId() } returns current
        coEvery { session.withUser<Unit>(any(), any()) } coAnswers {
            if (firstArg<String>() != current.value) throw OwnershipException()
            secondArg<suspend () -> Unit>().invoke()
        }
        val snapshot = mockk<AccountReminderSnapshot>()
        coEvery { snapshot.tasks(any()) } returns emptyList()
        val scheduler =
            object : DeadlineNotificationScheduler {
                override fun schedule(
                    taskId: String,
                    triggerAt: Instant,
                ) = Unit

                override fun cancel(taskId: String) = Unit
            }
        return AccountReminderCoordinator(context, session, snapshot, scheduler)
    }

    private fun publishNotification(context: Context) {
        NotificationManagerCompat
            .from(context)
            .notify(
                "account-reminder-test",
                1,
                NotificationCompat
                    .Builder(context, WorkManagerDeadlineScheduler.CHANNEL_ID)
                    .setContentTitle("Lembrete preservado")
                    .setContentText("Lembrete preservado")
                    .build(),
            )
    }

    @Test
    fun `reconciliar a mesma conta preserva notificacao publicada`() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            initializeWorkManager(context)
            val current = MutableStateFlow<String?>("u1")
            val coordinator = coordinator(context, current)
            publishNotification(context)

            val reconciliationJob = coordinator.startIn(backgroundScope)
            runCurrent()

            assertThat(NotificationManagerCompat.from(context).activeNotifications.map { it.tag })
                .contains("account-reminder-test")
            reconciliationJob.cancelAndJoin()
        }

    @Test
    fun `trocar de conta limpa notificacoes publicadas`() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            initializeWorkManager(context)
            val current = MutableStateFlow<String?>("u1")
            val coordinator = coordinator(context, current)
            val reconciliationJob = coordinator.startIn(backgroundScope)
            runCurrent()
            publishNotification(context)

            current.value = "u2"
            runCurrent()

            assertThat(NotificationManagerCompat.from(context).activeNotifications.map { it.tag })
                .doesNotContain("account-reminder-test")
            reconciliationJob.cancelAndJoin()
        }

    @Test
    fun `trabalho de acao sobrevive a reconciliacao da conta seguinte`() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            initializeWorkManager(context)
            val deadlineRequest =
                OneTimeWorkRequestBuilder<DeadlineWorker>()
                    .setConstraints(Constraints.Builder().setRequiresCharging(true).build())
                    .addTag(WorkManagerDeadlineScheduler.DEADLINE_TAG)
                    .build()
            val request =
                OneTimeWorkRequestBuilder<CompleteTaskWorker>()
                    .setConstraints(Constraints.Builder().setRequiresCharging(true).build())
                    .addTag(WorkManagerDeadlineScheduler.COMPLETE_TASK_TAG)
                    .build()
            WorkManager
                .getInstance(context)
                .enqueue(listOf(deadlineRequest, request))
                .result
                .get()
            val current = MutableStateFlow<String?>("u1")
            val coordinator = coordinator(context, current)
            val reconciliationJob = coordinator.startIn(backgroundScope)
            runCurrent()

            current.value = "u2"
            runCurrent()

            assertThat(
                WorkManager
                    .getInstance(context)
                    .getWorkInfoById(deadlineRequest.id)
                    .get()!!
                    .state,
            ).isEqualTo(WorkInfo.State.CANCELLED)
            assertThat(
                WorkManager
                    .getInstance(context)
                    .getWorkInfoById(request.id)
                    .get()!!
                    .state,
            ).isEqualTo(WorkInfo.State.ENQUEUED)
            reconciliationJob.cancelAndJoin()
        }

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
            val reconciliationJob =
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
            reconciliationJob.cancelAndJoin()
        }
}
