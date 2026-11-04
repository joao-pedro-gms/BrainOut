// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import pucgo.joaopedrogmsilva.brainout.maintenance.LegacySyncCleanup
import pucgo.joaopedrogmsilva.brainout.notifications.AccountReminderCoordinator
import pucgo.joaopedrogmsilva.brainout.notifications.WorkManagerDeadlineScheduler
import javax.inject.Inject

/** Inicializa Hilt, os lembretes locais e cancela trabalhos legados de sincronização. */
@HiltAndroidApp
class BrainOutApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var accountReminders: AccountReminderCoordinator

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Inicializando BrainOutApplication...")
        WorkManagerDeadlineScheduler.ensureChannel(this)
        LegacySyncCleanup.cancel(WorkManager.getInstance(this))
        accountReminders.start()
    }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

    companion object {
        private const val TAG = "BrainOut:Application"
    }
}
