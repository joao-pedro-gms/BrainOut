// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import pucgo.joaopedrogmsilva.brainout.R
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository

/**
 * Worker que publica a notificação de lembrete de prazo (marco E3.6).
 *
 * O [WorkManagerDeadlineScheduler] enfileira este worker para
 * `dueDate - 1h`; ao acordar, o worker:
 * 1. Recarrega a [Task] do Room — se a tarefa foi concluída ou
 *    deletada desde o agendamento, não notifica (leitura única de
 *    verdade; evita lembrete fantasma).
 * 2. Resolve o nome do projeto para o texto "Projeto \<nome\>".
 * 3. Publica a notificação no canal `brainout_deadlines` com as
 *    ações **Concluir** ([DeadlineReceiver.ACTION_COMPLETE_TASK]) e
 *    **Dispensar**.
 *
 * O POST_NOTIFICATIONS é verificado no momento da publicação: se o
 * usuário negou a permissão (Android 13+), `NotificationManagerCompat`
 * descarta silenciosamente — o worker termina com sucesso para não
 * reprocessar a fila.
 */
@HiltWorker
class DeadlineWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(KEY_TASK_ID)
            ?: return Result.failure()
        val outcome = publishReminder(taskId)
        return outcome ?: Result.success()
    }

    /**
     * Publica o lembrete da tarefa [taskId]; retorna `null` quando o
     * fluxo termina com sucesso (inclusive nos cortes-circuitos) ou
     * [Result.failure] quando o `Data` de entrada é inválido.
     */
    private suspend fun publishReminder(taskId: String): Result? {
        val task: Task = taskRepository.findById(taskId) ?: return null
        if (task.status == TaskStatus.DONE) return null

        val projectName = projectRepository.findById(task.projectId)?.name
            ?: applicationContext.getString(R.string.deadline_unknown_project)

        if (!hasNotificationPermission(applicationContext)) return null

        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            task.id.hashCode(),
            Intent(applicationContext, pucgo.joaopedrogmsilva.brainout.MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val completeIntent = PendingIntent.getBroadcast(
            applicationContext,
            task.id.hashCode(),
            Intent(applicationContext, DeadlineReceiver::class.java)
                .setAction(DeadlineReceiver.ACTION_COMPLETE_TASK)
                .putExtra(DeadlineReceiver.EXTRA_TASK_ID, task.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, WorkManagerDeadlineScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(applicationContext.getString(R.string.deadline_notification_title, task.title))
            .setContentText(applicationContext.getString(R.string.deadline_notification_body, projectName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(0, applicationContext.getString(R.string.deadline_action_complete), completeIntent)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(task.id.hashCode(), notification)
        return null
    }

    private fun hasNotificationPermission(context: Context): Boolean =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    companion object {

        /** Chave do `Data` de entrada com o id da tarefa. */
        const val KEY_TASK_ID: String = "task_id"
    }
}
