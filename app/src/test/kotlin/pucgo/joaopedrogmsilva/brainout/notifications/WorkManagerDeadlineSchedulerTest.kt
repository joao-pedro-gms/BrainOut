// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes do [WorkManagerDeadlineScheduler] (marco E3.6) contra um
 * WorkManager de teste (Robolectric):
 *
 * - `schedule` registra um trabalho único com o delay esperado e a
 *   tag de lembrete.
 * - `schedule` repetido para o mesmo taskId substitui o trabalho
 *   (política REPLACE — sempre 1 trabalho pendente por tarefa).
 * - `cancel` remove o trabalho pendente; chamar sem agendamento é no-op.
 * - Trigger no passado não registra trabalho (sem lembrete retroativo).
 * - `ensureChannel` cria o canal `brainout_deadlines` com
 *   importância HIGH.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class WorkManagerDeadlineSchedulerTest {

    private lateinit var context: Context
    private lateinit var scheduler: WorkManagerDeadlineScheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        scheduler = WorkManagerDeadlineScheduler(context)
    }

    private fun pendingInfos(taskId: String): List<WorkInfo> =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(WorkManagerDeadlineScheduler.uniqueWorkName(taskId))
            .get()

    @Test
    fun `schedule registra trabalho único com tag de lembrete`() {
        val triggerAt = Instant.now().plus(java.time.Duration.ofHours(2))
        scheduler.schedule("t1", triggerAt)

        val infos = pendingInfos("t1")
        assertThat(infos).hasSize(1)
        assertThat(infos.first().tags).contains(WorkManagerDeadlineScheduler.DEADLINE_TAG)
        assertThat(infos.first().state).isNotEqualTo(WorkInfo.State.CANCELLED)
    }

    @Test
    fun `schedule repetido substitui trabalho anterior`() {
        val triggerAt = Instant.now().plus(java.time.Duration.ofHours(2))
        scheduler.schedule("t1", triggerAt)
        scheduler.schedule("t1", triggerAt.plusSeconds(60))

        val infos = pendingInfos("t1")
        // Política REPLACE: a última escrita vence e fica um único trabalho.
        assertThat(infos.filter { it.state == WorkInfo.State.ENQUEUED }).hasSize(1)
    }

    @Test
    fun `cancel remove trabalho pendente`() {
        val triggerAt = Instant.now().plus(java.time.Duration.ofHours(2))
        scheduler.schedule("t1", triggerAt)
        scheduler.cancel("t1")

        val infos = pendingInfos("t1")
        assertThat(infos.first().state).isEqualTo(WorkInfo.State.CANCELLED)
    }

    @Test
    fun `cancel sem agendamento é no-op`() {
        scheduler.cancel("t-nunca-agendada")
        assertThat(pendingInfos("t-nunca-agendada")).isEmpty()
    }

    @Test
    fun `trigger no passado não registra trabalho`() {
        val past = Instant.now().minusSeconds(60)
        scheduler.schedule("t2", past)

        assertThat(pendingInfos("t2")).isEmpty()
    }

    @Test
    fun `ensureChannel cria canal com importância HIGH`() {
        WorkManagerDeadlineScheduler.ensureChannel(context)

        val manager = context.getSystemService(NotificationManager::class.java)
        val channel: NotificationChannel? =
            manager.getNotificationChannel(WorkManagerDeadlineScheduler.CHANNEL_ID)
        assertThat(channel).isNotNull()
        assertThat(channel!!.importance).isEqualTo(NotificationManager.IMPORTANCE_HIGH)
    }
}
