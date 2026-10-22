// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository
import java.io.IOException

/**
 * Testes do [DeadlineWorker] (marco E3.6).
 *
 * O ponto é o desfecho: uma falha **transitória** de I/O não pode
 * virar `Result.failure()` (terminal), senão o lembrete de prazo
 * daquela tarefa nunca mais aparece (DEF-32).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class DeadlineWorkerTest {
    private lateinit var context: Context
    private lateinit var taskRepository: TaskRepository
    private lateinit var projectRepository: ProjectRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        taskRepository = mockk(relaxed = true)
        projectRepository = mockk(relaxed = true)
    }

    private fun buildWorker(): DeadlineWorker =
        TestListenableWorkerBuilder<DeadlineWorker>(context)
            .setInputData(workDataOf(DeadlineKeys.TASK_ID to "t1"))
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker =
                        DeadlineWorker(
                            appContext,
                            workerParameters,
                            taskRepository,
                            projectRepository,
                        )
                },
            ).build()

    @Test
    fun `worker sem task_id falha imediatamente`() =
        runBlocking {
            val worker =
                TestListenableWorkerBuilder<DeadlineWorker>(context)
                    .setWorkerFactory(
                        object : WorkerFactory() {
                            override fun createWorker(
                                appContext: Context,
                                workerClassName: String,
                                workerParameters: WorkerParameters,
                            ): ListenableWorker =
                                DeadlineWorker(
                                    appContext,
                                    workerParameters,
                                    taskRepository,
                                    projectRepository,
                                )
                        },
                    ).build()

            val result = worker.doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        }

    @Test
    fun `falha transitoria pede repeticao em vez de falhar na hora`() =
        runBlocking {
            coEvery { taskRepository.findById("t1") } throws IOException("disco cheio")

            val result = buildWorker().doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        }
}
