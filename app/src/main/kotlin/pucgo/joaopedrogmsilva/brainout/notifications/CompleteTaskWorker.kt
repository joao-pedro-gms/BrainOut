// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.notifications

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import pucgo.joaopedrogmsilva.brainout.core.domain.error.DomainException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.ChangeTaskStatusUseCase
import java.io.IOException

private const val TAG = "BrainOut:CompleteTaskWorker"

/**
 * Worker que conclui a tarefa quando o usuário toca em **"Concluir"**
 * na notificação de prazo (marco E3.6).
 *
 * O [DeadlineReceiver] enfileira este worker em vez de executar a
 * escrita diretamente no receiver — o critério do ROADMAP exige que
 * "a ação altera o status da Task via WorkManager". Usar o
 * WorkManager aqui garante ainda que a escrita sobreviva ao ciclo de
 * vida efêmero do receiver (processo pode morrer antes do commit).
 *
 * A alteração passa pelo [ChangeTaskStatusUseCase] (que também
 * cancela o lembrete pendente via [WorkManagerDeadlineScheduler]).
 * Tarefa inexistente termina com sucesso — idempotência contra
 * corridas com a exclusão em outro ponto da UI.
 */
@HiltWorker
class CompleteTaskWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted params: WorkerParameters,
        private val changeTaskStatus: ChangeTaskStatusUseCase,
    ) : CoroutineWorker(appContext, params) {
        override suspend fun doWork(): Result {
            val taskId = inputData.getString(KEY_TASK_ID)
            if (taskId == null) {
                Log.e(TAG, "doWork falhou: KEY_TASK_ID ausente nos dados de entrada")
                return Result.failure()
            }
            Log.d(TAG, "Concluindo tarefa $taskId via notificação")
            return try {
                changeTaskStatus(taskId, TaskStatus.DONE)
                Log.d(TAG, "Tarefa $taskId marcada como DONE com sucesso")
                Result.success()
            } catch (e: DomainException) {
                // Tarefa inexistente (excluída em outro ponto) ou outra
                // violação de regra permanente — idempotente: encerra
                // com sucesso para que o WorkManager não reprocesse
                Log.w(
                    TAG,
                    "Domínio rejeitou alteração de status da tarefa $taskId: ${e.message}. Finalizando idempotente.",
                )
                Result.success()
            } catch (e: IOException) {
                // Falha transitória (Room indisponível, etc.) — backoff
                // exponencial do WorkManager.
                Log.e(TAG, "Falha de I/O ao concluir tarefa $taskId: ${e.message}. Solicitando retry.")
                Result.retry()
            }
        }

        companion object {
            /** Chave do `Data` de entrada com o id da tarefa. */
            const val KEY_TASK_ID: String = "task_id"
        }
    }
