// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.maintenance

import androidx.work.WorkManager

/** Cancela exclusivamente o sync de instalações anteriores à versão local. */
internal object LegacySyncCleanup {
    fun cancel(workManager: WorkManager) {
        workManager.cancelUniqueWork("brainout-sync")
        workManager.cancelUniqueWork("brainout-sync-now")
        workManager.cancelAllWorkByTag("brainout-sync-onetime")
    }
}
