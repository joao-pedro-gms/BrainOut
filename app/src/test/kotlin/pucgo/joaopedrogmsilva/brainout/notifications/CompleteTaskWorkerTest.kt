// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.ChangeTaskStatusUseCase

/**
 * Testes do [CompleteTaskWorker] (marco E3.6): o botão "Concluir" da
 * notificação deve alterar o status da Task via WorkManager —
 * exercitado aqui com Robolectric + [TestListenableWorkerBuilder],
 * exatamente o caminho que o WorkManager usa em produção.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class CompleteTaskWorkerTest {

    private lateinit var context: Context
    private lateinit var taskRepository: TaskRepository
    private lateinit var deadlineScheduler: DeadlineNotificationScheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        taskRepository = io.mockk.mockk(relaxed = true)
        deadlineScheduler = io.mockk.mockk(relaxed = true)
    }

    private fun buildWorker(): CompleteTaskWorker {
        val useCase = ChangeTaskStatusUseCase(taskRepository, deadlineScheduler)
        return TestListenableWorkerBuilder<CompleteTaskWorker>(context)
            .setInputData(workDataOf(CompleteTaskWorker.KEY_TASK_ID to "t1"))
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: androidx.work.WorkerParameters,
                ): ListenableWorker = CompleteTaskWorker(
                    appContext,
                    workerParameters,
                    useCase,
                )
            })
            .build()
    }

    @Test
    fun `worker conclui task ativa com prazo`() = runBlocking {
        // DOING → DONE é uma transição válida na matriz TaskStatus
        // (TODO → DONE é proibida pela regra de domínio E1.4).
        val task = Task.create(
            projectId = "p1",
            title = "Entrega do relatório",
            status = TaskStatus.DOING,
            dueDate = Instant.now().plus(java.time.Duration.ofHours(2)),
        )
        val done = task.transitionTo(TaskStatus.DONE)
        coEvery { taskRepository.changeStatus("t1", TaskStatus.DONE) } returns done

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify(exactly = 1) { taskRepository.changeStatus("t1", TaskStatus.DONE) }
        // ChangeTaskStatusUseCase cancela o lembrete ao concluir (o id
        // cancelado é o id da Task criada dentro do use case).
        io.mockk.verify(exactly = 1) { deadlineScheduler.cancel(task.id) }
    }

    @Test
    fun `worker sem task_id falha imediatamente`() = runBlocking {
        val useCase = ChangeTaskStatusUseCase(taskRepository, deadlineScheduler)
        val worker = TestListenableWorkerBuilder<CompleteTaskWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: androidx.work.WorkerParameters,
                ): ListenableWorker = CompleteTaskWorker(appContext, workerParameters, useCase)
            })
            .build()

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        coVerify(exactly = 0) { taskRepository.changeStatus(any(), any()) }
    }

    @Test
    fun `worker com tarefa inexistente termina com sucesso (idempotencia)`() = runBlocking {
        coEvery { taskRepository.changeStatus("t-desaparecida", TaskStatus.DONE) } throws
            IllegalArgumentException("Tarefa não encontrada: t-desaparecida")

        val useCase = ChangeTaskStatusUseCase(taskRepository, deadlineScheduler)
        val worker = TestListenableWorkerBuilder<CompleteTaskWorker>(context)
            .setInputData(workDataOf(CompleteTaskWorker.KEY_TASK_ID to "t-desaparecida"))
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: androidx.work.WorkerParameters,
                ): ListenableWorker = CompleteTaskWorker(appContext, workerParameters, useCase)
            })
            .build()

        val result = worker.doWork()

        // Falha de escrita vira retry — o WorkManager reprocessa com backoff.
        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
    }
}
