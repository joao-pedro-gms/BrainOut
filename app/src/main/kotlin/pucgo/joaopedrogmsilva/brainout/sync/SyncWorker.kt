// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.PendingOpDao
import pucgo.joaopedrogmsilva.brainout.core.data.sync.SyncDispatcher
import pucgo.joaopedrogmsilva.brainout.core.data.sync.SyncOutcome

private const val TAG = "BrainOut:SyncWorker"

/**
 * Worker que drena a fila `pending_ops` contra a retaguarda (E3.3).
 *
 * Estratégia de drenagem:
 * - Lê as ops em ordem de enfileiramento ([PendingOpDao.nextBatch]);
 *   para no **primeiro** erro retriable para preservar a ordem global
 *   (op UPDATE chegando antes do CREATE correspondente violaria a FK
 *   `project_id` do stub).
 * - Sucesso → op removida da fila.
 * - Erro retriable (IOException, HTTP 5xx) → op permanece (com
 *   `attempts` incrementado) e o worker devolve [Result.retry] — o
 *   WorkManager aplica backoff exponencial (30 s inicial).
 * - Erro permanente (HTTP 4xx / payload inválido) → op **descartada**
 *   com log estruturado no [SyncDispatcher]: reenviar não mudaria o
 *   resultado e a op travaria toda a fila para sempre. A alteração
 *   local permanece (a fila é sobre reconciliar com o servidor).
 *
 * Agendamento: ver [SyncScheduler] — periódico de 15 min (KEEP) +
 * OneTimeWorkRequest com constraint `NetworkType.CONNECTED`.
 */
@HiltWorker
class SyncWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted params: WorkerParameters,
        private val pendingOpDao: PendingOpDao,
        private val dispatcher: SyncDispatcher,
    ) : CoroutineWorker(appContext, params) {
        override suspend fun doWork(): Result {
            Log.d(TAG, "Iniciando doWork para drenar fila de pendências")
            var processedTotal = 0
            while (true) {
                val batch = pendingOpDao.nextBatch(BATCH_SIZE)
                if (batch.isEmpty()) {
                    Log.d(TAG, "Nenhuma op pendente na fila. Drenagem concluída (total processado: $processedTotal)")
                    break
                }
                Log.d(TAG, "Lote recuperado: ${batch.size} ops pendentes")
                for (op in batch) {
                    Log.d(TAG, "Enviando op ${op.id} (${op.entityType}/${op.entityId} opType=${op.opType})")
                    when (val outcome = dispatcher.send(op)) {
                        is SyncOutcome.Success -> {
                            Log.d(TAG, "Op ${op.id} enviada com sucesso. Removendo da fila local.")
                            pendingOpDao.deleteById(op.id)
                            processedTotal++
                        }
                        is SyncOutcome.Permanent -> {
                            // Contrato violado: descarta a op — a alteração
                            // local permanece, mas a fila anda. O motivo
                            // (HTTP 4xx ou payload inválido) já foi logado
                            // estruturado pelo dispatcher.
                            val reason = "HTTP ${outcome.httpCode}: ${outcome.reason}"
                            Log.w(TAG, "Op ${op.id} falhou permanentemente ($reason). Descartando op.")
                            pendingOpDao.deleteById(op.id)
                            processedTotal++
                        }
                        is SyncOutcome.Retriable -> {
                            // Interrompe a drenagem preservando a ordem;
                            // registra a tentativa e pede retry com backoff.
                            Log.w(TAG, "Op ${op.id} falhou temporariamente (${outcome.reason}). Interrompendo lote.")
                            pendingOpDao.markAttempt(op.id)
                            return Result.retry()
                        }
                    }
                }
            }
            return Result.success()
        }

        companion object {
            /** Nome do trabalho periódico único (ver [SyncScheduler]). */
            const val UNIQUE_PERIODIC_NAME: String = "brainout-sync"

            /** Tamanho do lote de drenagem por consulta. */
            const val BATCH_SIZE: Int = 50
        }
    }
