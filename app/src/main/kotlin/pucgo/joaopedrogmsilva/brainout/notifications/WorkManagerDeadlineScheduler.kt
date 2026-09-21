// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import pucgo.joaopedrogmsilva.brainout.R
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler

/**
 * Implementação concreta de [DeadlineNotificationScheduler] sobre
 * **WorkManager** (marco E3.6).
 *
 * Escolha documentada em `docs/ARQUITETURA.md` (Seção 10): WorkManager
 * com `OneTimeWorkRequest` + `setInitialDelay` cobre o requisito
 * "lembrar 1 hora antes do prazo" sem exigir a permissão
 * `SCHEDULE_EXACT_ALARM`, restrita no Android 13+. A janela de
 * tolerância do WorkManager (tipicamente poucos minutos) é aceitável
 * para lembretes de prazo acadêmico e ganha consistência com o E3.3
 * (sincronização também em WorkManager).
 *
 * Cada tarefa tem exatamente um trabalho pendente, identificado por
 * [uniqueWorkName] — reagendar substitui o trabalho anterior via
 * [ExistingWorkPolicy.REPLACE], cancelar remove-o via
 * [WorkManager.cancelUniqueWork].
 */
@Singleton
class WorkManagerDeadlineScheduler @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : DeadlineNotificationScheduler {

    override fun schedule(taskId: String, triggerAt: Instant) {
        val delayMillis = triggerAt.toEpochMilli() - System.currentTimeMillis()
        if (delayMillis <= 0) {
            // Prazo já vencido no momento do agendamento — sem lembrete
            // retroativo (regra do domínio E3.6).
            cancel(taskId)
            return
        }
        val request = OneTimeWorkRequestBuilder<DeadlineWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(DeadlineWorker.KEY_TASK_ID to taskId))
            .addTag(DEADLINE_TAG)
            .build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(uniqueWorkName(taskId), ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel(taskId: String) {
        WorkManager.getInstance(appContext).cancelUniqueWork(uniqueWorkName(taskId))
    }

    companion object {

        /** Prefixo do nome de trabalho único por tarefa. */
        const val UNIQUE_WORK_PREFIX: String = "deadline-"

        /** Tag comum a todos os trabalhos de lembrete (útil para testes/QA). */
        const val DEADLINE_TAG: String = "brainout_deadline"

        /** Id do canal de notificações de prazo (HIGH importance). */
        const val CHANNEL_ID: String = "brainout_deadlines"

        /** Nome único do trabalho WorkManager de uma tarefa. */
        fun uniqueWorkName(taskId: String): String = "$UNIQUE_WORK_PREFIX$taskId"

        /**
         * Cria o canal `brainout_deadlines` (importância HIGH). Idempotente:
         * chamar para canal já existente é no-op na plataforma. Deve ser
         * chamado em `BrainOutApplication.onCreate()`.
         *
         * Usa recursos string resolvidos diretamente por id — evitar
         * `getIdentifier` por nome, que é frágil em Robolectric.
         *
         * Canal de notificação é API 26+ (`Build.VERSION_CODES.O`). Em
         * dispositivos 24/25 a chamada é silenciosamente ignorada —
         * notificações funcionam sem canal (sem tom/opções de
         * importância customizadas).
         */
        fun ensureChannel(context: Context) {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.deadline_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.deadline_channel_description)
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}
