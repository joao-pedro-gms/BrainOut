// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.notifications

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import pucgo.joaopedrogmsilva.brainout.core.data.session.SessionStore
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.ChangeTaskStatusUseCase

// Fábricas de worker dos testes do módulo :app.
//
// O `WorkerFactory` anônimo que monta o worker estava copiado em seis
// lugares — três em `CompleteTaskWorkerTest`, dois em `DeadlineWorkerTest`
// e um em `DeadlineWorkerPublishTest`. Cada cópia fixava o mesmo
// construtor, e uma mudança na assinatura do worker exigia seis
// edições.

// A fábrica que monta o [DeadlineWorker] com os repositórios dados.
internal fun deadlineWorkerFactory(
    taskRepository: TaskRepository,
    projectRepository: ProjectRepository,
): WorkerFactory =
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
                io.mockk.mockk<SessionStore>(relaxed = true).also { session ->
                    io.mockk.coEvery { session.withUser<Unit>(any(), any()) } coAnswers
                        { secondArg<suspend () -> Unit>().invoke() }
                },
            )
    }

// A fábrica que monta o [CompleteTaskWorker] com o caso de uso dado.
internal fun completeTaskWorkerFactory(changeTaskStatus: ChangeTaskStatusUseCase): WorkerFactory =
    object : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters,
        ): ListenableWorker =
            CompleteTaskWorker(
                appContext,
                workerParameters,
                changeTaskStatus,
            )
    }
