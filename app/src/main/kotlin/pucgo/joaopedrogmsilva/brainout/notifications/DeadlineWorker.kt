// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import pucgo.joaopedrogmsilva.brainout.R
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository

private const val TAG = "BrainOut:DeadlineWorker"

/**
 * Quantas vezes o worker tenta publicar o lembrete antes de desistir.
 *
 * O WorkManager repete com backoff exponencial (o padrão é 30 s); três
 * tentativas cobrem uma falha de I/O transória sem deixar o trabalho
 * reprocessando para sempre (DEF-32).
 */
private const val MAX_ATTEMPTS = 3

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
 *
 * Falha **transitória** (I/O do banco) não é o mesmo que desfecho
 * normal: o worker pede `Result.retry()` para o WorkManager repetir
 * com backoff, em vez de marcar o trabalho como falho na hora e nunca
 * mais lembrar do prazo (DEF-32).
 */
@HiltWorker
class DeadlineWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted params: WorkerParameters,
        private val taskRepository: TaskRepository,
        private val projectRepository: ProjectRepository,
    ) : CoroutineWorker(appContext, params) {
        override suspend fun doWork(): Result {
            val taskId = inputData.getString(DeadlineKeys.TASK_ID)
            if (taskId == null) {
                Log.e(TAG, "doWork falhou: id da tarefa não informado")
                return Result.failure()
            }
            Log.d(TAG, "Executando DeadlineWorker para taskId=$taskId")
            return try {
                publishReminder(taskId)
                Result.success()
            } catch (e: CancellationException) {
                // Cancelamento não é falha: o WorkManager trata o
                // cancellation separadamente.
                throw e
            } catch (e: Exception) {
                // Falha transitória (I/O do banco, por exemplo): o
                // WorkManager repete com backoff. Sem este `try/catch`,
                // a exceção virava `Result.failure()` — terminal — e o
                // lembrete de prazo nunca mais aparecia para aquela
                // tarefa (DEF-32).
                if (runAttemptCount >= MAX_ATTEMPTS) {
                    Log.e(TAG, "Lembrete de $taskId falhou após $MAX_ATTEMPTS tentativas; desistindo", e)
                    Result.failure()
                } else {
                    Log.w(TAG, "Falha transitória ao publicar o lembrete de $taskId; o WorkManager repetirá", e)
                    Result.retry()
                }
            }
        }

        /**
         * Publica o lembrete da tarefa [taskId].
         *
         * Não há resultado a devolver: qualquer corte-circuito (tarefa
         * inexistente, já concluída, permissão negada) é um desfecho
         * normal — o worker termina com sucesso para não reprocessar a
         * fila.
         */
        private suspend fun publishReminder(taskId: String) {
            val task: Task? = taskRepository.findById(taskId)
            if (task == null) {
                Log.d(TAG, "Lembrete cancelado: tarefa $taskId não foi encontrada")
                return
            }
            if (task.status == TaskStatus.DONE) {
                Log.d(TAG, "Lembrete cancelado: tarefa $taskId já está concluída")
                return
            }

            val projectName =
                projectRepository.findById(task.projectId)?.name
                    ?: applicationContext.getString(R.string.deadline_unknown_project)

            if (!hasNotificationPermission(applicationContext)) {
                Log.w(TAG, "Lembrete não exibido: permissão POST_NOTIFICATIONS negada")
                return
            }

            val notificationId = DeadlineNotificationId.of(task.id)
            val notificationTag = DeadlineNotificationId.tagOf(task.id)
            val contentIntent =
                PendingIntent.getActivity(
                    applicationContext,
                    notificationId,
                    Intent(applicationContext, pucgo.joaopedrogmsilva.brainout.MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            val completeIntent =
                PendingIntent.getBroadcast(
                    applicationContext,
                    notificationId,
                    Intent(applicationContext, DeadlineReceiver::class.java)
                        .setAction(DeadlineReceiver.ACTION_COMPLETE_TASK)
                        // O `data` torna o `PendingIntent` único por
                        // tarefa. Sem ele, duas tarefas com o mesmo id
                        // numérico (colisão de `hashCode`) compartilhariam
                        // o mesmo `PendingIntent` — e o "Concluir" de uma
                        // acabaria concluindo a outra (DEF-54).
                        .setData(Uri.parse("brainout://deadline/${task.id}"))
                        .putExtra(DeadlineKeys.TASK_ID, task.id),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            val notification =
                NotificationCompat
                    .Builder(applicationContext, WorkManagerDeadlineScheduler.CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(applicationContext.getString(R.string.deadline_notification_title, task.title))
                    .setContentText(applicationContext.getString(R.string.deadline_notification_body, projectName))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setAutoCancel(true)
                    .setContentIntent(contentIntent)
                    .addAction(0, applicationContext.getString(R.string.deadline_action_complete), completeIntent)
                    .build()

            // `NotificationManagerCompat.notify` exige que a permissão
            // `POST_NOTIFICATIONS` tenha sido concedida em API 33+. O
            // guarda `hasNotificationPermission` acima cobre isso em runtime;
            // aqui usamos `@RequiresPermission` para silenciar o lint com
            // garantia documentada (a verificação é feita na linha 68).
            @SuppressLint("MissingPermission")
            NotificationManagerCompat
                .from(applicationContext)
                .notify(notificationTag, notificationId, notification)
            Log.d(TAG, "Notificação de lembrete de prazo enviada para tarefa ${task.id} (${task.title})")
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
    }
