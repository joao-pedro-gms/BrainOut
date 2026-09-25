// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.DomainException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.ChangeTaskStatusUseCase

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
class CompleteTaskWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val changeTaskStatus: ChangeTaskStatusUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(KEY_TASK_ID)
            ?: return Result.failure()
        return try {
            changeTaskStatus(taskId, TaskStatus.DONE)
            Result.success()
        } catch (e: DomainException) {
            // Tarefa inexistente (excluída em outro ponto) ou outra
            // violação de regra permanente — idempotente: encerra
            // com sucesso para que o WorkManager não reprocesse
            Result.success()
        } catch (e: IOException) {
            // Falha transitória (Room indisponível, etc.) — backoff
            // exponencial do WorkManager.
            Result.retry()
        }
    }

    companion object {

        /** Chave do `Data` de entrada com o id da tarefa. */
        const val KEY_TASK_ID: String = "task_id"
    }
}
