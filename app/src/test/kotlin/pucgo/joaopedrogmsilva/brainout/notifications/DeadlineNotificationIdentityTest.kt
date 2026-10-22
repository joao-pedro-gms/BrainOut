// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Prova, na bandeja, do DEF-54: duas tarefas cujo `hashCode()` colide
 * publicam **duas** notificações — e dispensar uma não derruba a
 * outra.
 *
 * Antes da correção, o id numérico sozinho era a identidade: a
 * segunda notificação substituía a primeira, e o "Dispensar" de uma
 * cancelava a outra.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class DeadlineNotificationIdentityTest {
    /** Par real de UUIDs com o mesmo `hashCode()`. */
    private val first = "c3f9f00e-1c18-433f-8115-b38ff7434044"

    /** Ver [first] — mesmo `hashCode()`, tarefa diferente. */
    private val second = "1ed5817b-4697-49c7-b1d8-8a62c94fce50"

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun notification(title: String): NotificationCompat.Builder =
        NotificationCompat
            .Builder(context, WorkManagerDeadlineScheduler.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(title)

    @Test
    fun `duas tarefas com hashCode colidente publicam duas notificacoes`() {
        val manager = NotificationManagerCompat.from(context)

        manager.notify(
            DeadlineNotificationId.tagOf(first),
            DeadlineNotificationId.of(first),
            notification("Tarefa A").build(),
        )
        manager.notify(
            DeadlineNotificationId.tagOf(second),
            DeadlineNotificationId.of(second),
            notification("Tarefa B").build(),
        )

        assertThat(manager.activeNotifications).hasSize(2)
    }

    @Test
    fun `dispensar uma notificacao nao derruba a da tarefa que colide`() {
        val manager = NotificationManagerCompat.from(context)

        manager.notify(
            DeadlineNotificationId.tagOf(first),
            DeadlineNotificationId.of(first),
            notification("Tarefa A").build(),
        )
        manager.notify(
            DeadlineNotificationId.tagOf(second),
            DeadlineNotificationId.of(second),
            notification("Tarefa B").build(),
        )

        NotificationDismissal.dismiss(context, first)

        val restantes = manager.activeNotifications
        assertThat(restantes).hasSize(1)
        assertThat(restantes.single().tag).isEqualTo(DeadlineNotificationId.tagOf(second))
    }
}
