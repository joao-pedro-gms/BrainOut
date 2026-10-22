// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat

/**
 * Helper de limpeza de notificações em exibição (marco E3.6).
 *
 * Isolado em arquivo próprio para que o [DeadlineReceiver] e os
 * testes Robolectric possam verificar o cancelamento sem depender de
 * Android framework mocks além do `NotificationManagerCompat`.
 */
object NotificationDismissal {
    /**
     * Remove a notificação de lembrete associada a [taskId]. A
     * identidade é a mesma usada na publicação — tag e id numérico
     * ([DeadlineNotificationId]).
     */
    fun dismiss(
        context: Context,
        taskId: String,
    ) {
        NotificationManagerCompat
            .from(context)
            .cancel(DeadlineNotificationId.tagOf(taskId), DeadlineNotificationId.of(taskId))
    }
}
