// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Agendador do [SyncWorker] (E3.3).
 *
 * - **Periódico**: `enqueueUniquePeriodicWork(SyncWorker.UNIQUE_PERIODIC_NAME,
 *   KEEP, 15 min)` — KEEP evita reenfileirar a cada boot; com
 *   constraint de rede ativa.
 * - **OneTime**: usado para reconciliar assim que a rede volta
 *   (requisito do E3.3/E3.4) e após qualquer escrita local enfileirar
 *   uma op — drena imediatamente quando há rede; offline fica
 *   pendente até a constraint ser satisfeita.
 *
 * O backoff exponencial ([BackoffPolicy.EXPONENTIAL], 30 s inicial)
 * cobre os retries do worker.
 */
@Singleton
class SyncScheduler @Inject constructor(
    private val workManager: WorkManager,
) {

    /** Registra o trabalho periódico de 15 min (idempotente via KEEP). */
    fun ensurePeriodicSync() {
        val request = androidx.work.PeriodicWorkRequestBuilder<SyncWorker>(
            PERIOD_MINUTES, TimeUnit.MINUTES,
        )
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            SyncWorker.UNIQUE_PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /**
     * Drena imediatamente a fila (OneTimeWorkRequest com constraint
     * de rede). Com `ExistingWorkPolicy.KEEP`, chamadas repetidas
     * durante uma drenagem em curso são no-op.
     */
    fun requestImmediateSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .addTag(ONE_TIME_TAG)
            .build()
        workManager.enqueueUniqueWork(
            UNIQUE_ONE_TIME_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    private fun networkConstraints(): Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    companion object {
        /** Intervalo do trabalho periódico (minutos). */
        const val PERIOD_MINUTES: Long = 15

        /** Backoff inicial exponencial do WorkManager. */
        const val BACKOFF_SECONDS: Long = 30

        /** Tag comum aos OneTimeWorkRequests (usada em testes/QA). */
        const val ONE_TIME_TAG: String = "brainout-sync-onetime"

        /** Nome do trabalho único OneTime. */
        const val UNIQUE_ONE_TIME_NAME: String = "brainout-sync-now"
    }
}
