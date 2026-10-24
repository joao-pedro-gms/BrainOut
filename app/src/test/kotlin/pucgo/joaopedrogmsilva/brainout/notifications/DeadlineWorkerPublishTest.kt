// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.notifications

import android.Manifest
import android.app.Application
import android.app.Notification
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.R
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository

/**
 * Testes do **caminho de publicação** do [DeadlineWorker] (marco E3.6).
 *
 * O [DeadlineWorkerTest] cobre o desfecho do worker (falha terminal vs.
 * repetição). Este arquivo cobre o que o worker faz de verdade: publicar
 * o lembrete de prazo.
 *
 * O que fica provado:
 *
 * - a notificação sai com o título da tarefa, o nome do projeto e a
 *   ação "Concluir";
 * - a identidade da notificação é o par (tag, id) da tarefa — a mesma
 *   que o "Dispensar" e a publicação usam (DEF-54);
 * - o gate de permissão `POST_NOTIFICATIONS` segura a publicação (o
 *   worker termina com sucesso, sem notificar);
 * - tarefa inexistente e tarefa já concluída não publicam nada — e
 *   também não viram falha, para não reprocessar a fila;
 * - projeto inexistente não derruba a publicação: o corpo da
 *   notificação usa o texto de projeto desconhecido.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class DeadlineWorkerPublishTest {
    private lateinit var context: Context
    private lateinit var taskRepository: TaskRepository
    private lateinit var projectRepository: ProjectRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        taskRepository = mockk(relaxed = true)
        projectRepository = mockk(relaxed = true)
        // O worker só publica com a permissão concedida em API 33+.
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun buildWorker(taskId: String = "t1"): DeadlineWorker =
        TestListenableWorkerBuilder<DeadlineWorker>(context)
            .setInputData(workDataOf(DeadlineKeys.TASK_ID to taskId))
            .setWorkerFactory(deadlineWorkerFactory(taskRepository, projectRepository))
            .build()

    private fun notificationOf(taskId: String): Notification? =
        shadowOf(context.getSystemService(android.app.NotificationManager::class.java))
            .getNotification(DeadlineNotificationId.tagOf(taskId), DeadlineNotificationId.of(taskId))

    private fun textOf(
        notification: Notification,
        key: String,
    ): String =
        notification.extras
            .getCharSequence(key)
            ?.toString()
            .orEmpty()

    @Test
    fun `publica o lembrete com titulo da tarefa, projeto e acao Concluir`() =
        runBlocking {
            val tarefa = Task.create(projectId = "p1", title = "Tarefa")
            val projeto = Project.create(name = "Meu Projeto", ownerId = "u1")
            coEvery { taskRepository.findById("t1") } returns tarefa
            coEvery { projectRepository.findById("p1") } returns projeto

            val result = buildWorker().doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.success())
            val notification = notificationOf(tarefa.id)
            assertThat(notification).isNotNull()
            assertThat(textOf(notification!!, Notification.EXTRA_TITLE))
                .isEqualTo(context.getString(R.string.deadline_notification_title, "Tarefa"))
            assertThat(textOf(notification, Notification.EXTRA_TEXT))
                .isEqualTo(context.getString(R.string.deadline_notification_body, "Meu Projeto"))
            assertThat(notification.actions.toList()).hasSize(1)
            assertThat(notification.actions[0].title)
                .isEqualTo(context.getString(R.string.deadline_action_complete))
            assertThat(notification.actions[0].actionIntent).isNotNull()
        }

    @Test
    fun `nao publica quando a permissao de notificacao esta negada`() =
        runBlocking {
            shadowOf(context as Application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
            val tarefa = Task.create(projectId = "p1", title = "Tarefa")
            coEvery { taskRepository.findById("t1") } returns tarefa

            val result = buildWorker().doWork()

            // Sem notificação, mas com sucesso: a falta de permissão é
            // um desfecho normal, não algo a reprocessar.
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
            assertThat(notificationOf(tarefa.id)).isNull()
        }

    @Test
    fun `nao publica quando a tarefa nao existe`() =
        runBlocking {
            coEvery { taskRepository.findById("t1") } returns null

            val result = buildWorker().doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.success())
            assertThat(notificationOf("t1")).isNull()
        }

    @Test
    fun `nao publica quando a tarefa ja esta concluida`() =
        runBlocking {
            val tarefa = Task.create(projectId = "p1", title = "Tarefa", status = TaskStatus.DONE)
            coEvery { taskRepository.findById("t1") } returns tarefa

            val result = buildWorker().doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.success())
            assertThat(notificationOf(tarefa.id)).isNull()
        }

    @Test
    fun `usa o texto de projeto desconhecido quando o projeto nao existe`() =
        runBlocking {
            val tarefa = Task.create(projectId = "p1", title = "Tarefa")
            coEvery { taskRepository.findById("t1") } returns tarefa
            coEvery { projectRepository.findById("p1") } returns null

            val result = buildWorker().doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.success())
            val notification = notificationOf(tarefa.id)
            assertThat(notification).isNotNull()
            assertThat(textOf(notification!!, Notification.EXTRA_TEXT))
                .isEqualTo(
                    context.getString(
                        R.string.deadline_notification_body,
                        context.getString(R.string.deadline_unknown_project),
                    ),
                )
        }
}
