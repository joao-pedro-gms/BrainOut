// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.notifications

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** Tempo máximo que o teste espera o efeito assíncrono do receiver. */
private const val AWAIT_TIMEOUT_MS: Long = 5_000

/** Intervalo entre as tentativas de leitura do efeito. */
private const val POLL_INTERVAL_MS: Long = 20

/** Janela de silêncio usada nos casos em que o efeito não deve acontecer. */
private const val SILENCE_MS: Long = 300

/** Atraso do lembrete pendente — longe o bastante para não disparar durante o teste. */
private const val REMINDER_DELAY_SECONDS: Long = 3_600

/**
 * Testes do [DeadlineReceiver] isolado (marco E3.6).
 *
 * O receiver é a ponte entre o botão da notificação de prazo e o
 * WorkManager. O que precisa estar provado:
 *
 * - a ação "Concluir" **enfileira** o [CompleteTaskWorker] com o id da
 *   tarefa — a alteração de status acontece via WorkManager, nunca no
 *   receiver (critério E3.6 do ROADMAP);
 * - o lembrete pendente da tarefa é cancelado;
 * - a notificação em exibição é dispensada;
 * - uma ação que não seja a de concluir cancela e dispensa, sem
 *   enfileirar worker;
 * - um intent sem o id da tarefa não faz nada.
 *
 * O receiver roda com o grafo real do Hilt (o `@AndroidEntryPoint`
 * injeta o agendador), então o cancelamento do lembrete é observado
 * pelo estado do trabalho no WorkManager de teste.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class DeadlineReceiverTest {
    private lateinit var context: Context
    private lateinit var workerFactory: CompletionWorkerFactory

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        workerFactory = CompletionWorkerFactory()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build(),
        )
    }

    private fun intent(
        action: String,
        taskId: String? = "t1",
    ): Intent =
        Intent(action).apply {
            taskId?.let { putExtra(DeadlineKeys.TASK_ID, it) }
        }

    private fun completionWorks(): List<WorkInfo> =
        WorkManager
            .getInstance(context)
            .getWorkInfosByTag(WorkManagerDeadlineScheduler.DEADLINE_TAG)
            .get()
            .filter { it.workerClassName == CompleteTaskWorker::class.java.name }

    private fun reminderState(taskId: String): WorkInfo.State? =
        WorkManager
            .getInstance(context)
            .getWorkInfosForUniqueWork(WorkManagerDeadlineScheduler.uniqueWorkName(taskId))
            .get()
            .firstOrNull()
            ?.state

    private fun publishReminderNotification(taskId: String) {
        NotificationManagerCompat
            .from(context)
            .notify(
                DeadlineNotificationId.tagOf(taskId),
                DeadlineNotificationId.of(taskId),
                NotificationCompat
                    .Builder(context, WorkManagerDeadlineScheduler.CHANNEL_ID)
                    .setContentTitle("Prazo")
                    .setContentText("Prazo")
                    .build(),
            )
    }

    private fun activeNotificationCount(): Int = NotificationManagerCompat.from(context).activeNotifications.size

    private fun awaitUntil(
        description: String,
        condition: () -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(POLL_INTERVAL_MS)
        }
        throw AssertionError("Timeout esperando: $description")
    }

    @Test
    fun `acao Concluir enfileira o worker da tarefa`() {
        DeadlineReceiver().onReceive(context, intent(DeadlineReceiver.ACTION_COMPLETE_TASK))

        awaitUntil("worker de conclusão enfileirado") { completionWorks().isNotEmpty() }
        awaitUntil("id da tarefa entregue ao worker") { workerFactory.taskIds.isNotEmpty() }

        val work = completionWorks().single()
        assertThat(work.tags).contains(WorkManagerDeadlineScheduler.DEADLINE_TAG)
        assertThat(workerFactory.taskIds.single()).isEqualTo("t1")
    }

    @Test
    fun `acao Concluir cancela o lembrete pendente da tarefa`() {
        WorkManagerDeadlineScheduler(context).schedule("t1", Instant.now().plusSeconds(REMINDER_DELAY_SECONDS))
        assertThat(reminderState("t1")).isEqualTo(WorkInfo.State.ENQUEUED)

        DeadlineReceiver().onReceive(context, intent(DeadlineReceiver.ACTION_COMPLETE_TASK))

        awaitUntil("lembrete cancelado") { reminderState("t1") == WorkInfo.State.CANCELLED }
    }

    @Test
    fun `acao dispensa a notificacao em exibicao`() {
        publishReminderNotification("t1")
        assertThat(activeNotificationCount()).isEqualTo(1)

        DeadlineReceiver().onReceive(context, intent(DeadlineReceiver.ACTION_COMPLETE_TASK))

        awaitUntil("notificação dispensada") { activeNotificationCount() == 0 }
    }

    @Test
    fun `outra acao cancela o lembrete e dispensa a notificacao sem enfileirar worker`() {
        WorkManagerDeadlineScheduler(context).schedule("t1", Instant.now().plusSeconds(REMINDER_DELAY_SECONDS))
        publishReminderNotification("t1")

        DeadlineReceiver().onReceive(context, intent(OUTRA_ACAO))

        awaitUntil("lembrete cancelado") { reminderState("t1") == WorkInfo.State.CANCELLED }
        awaitUntil("notificação dispensada") { activeNotificationCount() == 0 }
        assertThat(completionWorks()).isEmpty()
    }

    @Test
    fun `intent sem id de tarefa nao enfileira nem cancela nada`() {
        publishReminderNotification("t1")

        DeadlineReceiver().onReceive(context, intent(DeadlineReceiver.ACTION_COMPLETE_TASK, taskId = null))

        Thread.sleep(SILENCE_MS)
        assertThat(completionWorks()).isEmpty()
        assertThat(reminderState("t1")).isNull()
        assertThat(activeNotificationCount()).isEqualTo(1)
    }

    /**
     * Ação que não é a de concluir — o receiver trata qualquer outra
     * ação como dispensa do lembrete.
     */
    private companion object {
        const val OUTRA_ACAO: String = "pucgo.joaopedrogmsilva.brainout.action.OUTRA"
    }

    /**
     * Worker de mentira para o [CompleteTaskWorker]: o que está sob
     * teste é o *enfileiramento* com o id da tarefa, não o corpo do
     * worker (que tem teste próprio, `CompleteTaskWorkerTest`).
     */
    private class StubCompletionWorker(
        context: Context,
        params: WorkerParameters,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result = Result.success()
    }

    /**
     * Fábrica que resolve o worker de conclusão para a implementação de
     * mentira e registra o id da tarefa que chegou no worker.
     *
     * O `WorkInfo` não expõe os dados de entrada — a captura acontece
     * no único ponto onde eles existem, que é a fábrica.
     */
    private class CompletionWorkerFactory : WorkerFactory() {
        val taskIds: MutableList<String?> = mutableListOf()

        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters,
        ): ListenableWorker? {
            if (workerClassName != CompleteTaskWorker::class.java.name) return null
            taskIds += workerParameters.inputData.getString(DeadlineKeys.TASK_ID)
            return StubCompletionWorker(appContext, workerParameters)
        }
    }
}
