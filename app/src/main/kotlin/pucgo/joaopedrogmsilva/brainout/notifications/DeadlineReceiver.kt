// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler

/**
 * Receiver da notificação de lembrete de prazo (marco E3.6).
 *
 * Ações suportadas:
 * - [ACTION_COMPLETE_TASK] ("Concluir"): enfileira o
 *   [CompleteTaskWorker] no WorkManager — a mudança de status da
 *   Task acontece **via WorkManager**, conforme o critério do
 *   ROADMAP — e cancela a notificação em exibição.
 * - Qualquer outra ação (incluindo "Dispensar"): apenas limpa a
 *   notificação da barrinha (o próprio `setAutoCancel(true)` já
 *   cobre o tap no corpo).
 *
 * O receiver é registrado no `AndroidManifest.xml` com
 * `android:exported="false"` — só o app pode disparar os intents de
 * ação.
 */
@AndroidEntryPoint
class DeadlineReceiver : BroadcastReceiver() {

    @Inject
    lateinit var deadlineScheduler: DeadlineNotificationScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val result = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                if (intent.action == ACTION_COMPLETE_TASK) {
                    // A alteração de status ocorre via WorkManager (requisito
                    // do critério E3.6): enfileiramos o worker dedicado e
                    // deixamos o WorkManager decidir quando executá-lo.
                    val request = OneTimeWorkRequestBuilder<CompleteTaskWorker>()
                        .setInputData(workDataOf(CompleteTaskWorker.KEY_TASK_ID to taskId))
                        .addTag(WorkManagerDeadlineScheduler.DEADLINE_TAG)
                        .build()
                    WorkManager.getInstance(context)
                        .enqueue(request)
                }
                deadlineScheduler.cancel(taskId)
            } finally {
                NotificationDismissal.dismiss(context, taskId)
                scope.cancel()
                result.finish()
            }
        }
    }

    companion object {

        /** Ação do botão "Concluir" da notificação. */
        const val ACTION_COMPLETE_TASK: String = "pucgo.joaopedrogmsilva.brainout.action.COMPLETE_TASK"

        /** Chave do extra com o id da tarefa. */
        const val EXTRA_TASK_ID: String = "task_id"
    }
}
