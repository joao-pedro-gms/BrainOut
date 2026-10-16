// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.maintenance

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class LegacySyncCleanupTest {
    @Test
    fun `upgrade cancela sync antigo sem cancelar lembretes ou outros trabalhos`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        val manager = WorkManager.getInstance(context)
        val constraints = Constraints.Builder().setRequiresCharging(true).build()
        val periodic = PeriodicWorkRequestBuilder<IdleWorker>(15, TimeUnit.MINUTES).setConstraints(constraints).build()
        val immediate = OneTimeWorkRequestBuilder<IdleWorker>().setConstraints(constraints).build()
        val tagged =
            OneTimeWorkRequestBuilder<IdleWorker>()
                .setConstraints(
                    constraints,
                ).addTag("brainout-sync-onetime")
                .build()
        val reminder = OneTimeWorkRequestBuilder<IdleWorker>().setConstraints(constraints).build()
        manager.enqueueUniquePeriodicWork("brainout-sync", ExistingPeriodicWorkPolicy.KEEP, periodic).result.get()
        manager.enqueueUniqueWork("brainout-sync-now", ExistingWorkPolicy.KEEP, immediate).result.get()
        manager.enqueue(tagged).result.get()
        manager.enqueueUniqueWork("deadline-t1", ExistingWorkPolicy.KEEP, reminder).result.get()

        LegacySyncCleanup.cancel(manager)
        LegacySyncCleanup.cancel(manager)

        for (id in listOf(periodic.id, immediate.id, tagged.id)) {
            assertThat(manager.getWorkInfoById(id).get()?.state).isEqualTo(WorkInfo.State.CANCELLED)
        }
        assertThat(manager.getWorkInfoById(reminder.id).get()?.state).isEqualTo(WorkInfo.State.ENQUEUED)
    }

    class IdleWorker(
        context: Context,
        parameters: WorkerParameters,
    ) : Worker(context, parameters) {
        override fun doWork(): Result = Result.success()
    }
}
