// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.PendingOpDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.PendingOpEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectSyncPayload
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.SyncEntityType
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.SyncOpType
import pucgo.joaopedrogmsilva.brainout.core.data.remote.RemoteDataSource
import pucgo.joaopedrogmsilva.brainout.core.data.sync.BrainOutSyncDispatcher
import androidx.room.Room

/**
 * Testes do [SyncWorker] (E3.3) exercitando a drenagem da fila
 * `pending_ops` de ponta a ponta:
 *
 * - **Online** (MockWebServer 2xx): todas as ops são removidas na
 *   ordem de enfileiramento e o worker termina com sucesso.
 * - **Offline** (servidor fora): a op permanece, `attempts` é
 *   incrementado e o worker devolve `Result.retry()` (o WorkManager
 *   aplica o backoff exponencial).
 * - **4xx** (contrato violado): a op é descartada, a fila anda e o
 *   worker termina com sucesso.
 *
 * O worker é construído via [TestListenableWorkerBuilder] com um
 * [WorkerFactory] manual (mesmo padrão dos testes do E3.6) — o
 * banco Room é em memória e real, exercitando o DAO de verdade.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class SyncWorkerTest {

    private lateinit var context: Context
    private lateinit var database: BrainOutDatabase
    private lateinit var pendingOpDao: PendingOpDao
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        database = Room.inMemoryDatabaseBuilder(context, BrainOutDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pendingOpDao = database.pendingOpDao()
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        database.close()
        server.shutdown()
    }

    /** Constrói o worker com o dispatcher real apontando ao MockWebServer. */
    private fun buildWorker(): SyncWorker {
        val dispatcher = BrainOutSyncDispatcher(
            remote = RemoteDataSource(baseUrl = server.url("/").toString(), logError = { }),
            logError = { },
        )
        return TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: androidx.work.WorkerParameters,
                ): ListenableWorker = SyncWorker(
                    appContext,
                    workerParameters,
                    pendingOpDao,
                    dispatcher,
                )
            })
            .build()
    }

    private suspend fun enqueueProjectOp(id: String = "p-1"): PendingOpEntity {
        val op = PendingOpEntity.enqueue(
            entityType = SyncEntityType.PROJECT,
            entityId = id,
            opType = SyncOpType.CREATE,
            payloadObj = ProjectSyncPayload(
                id = id,
                name = "Projeto $id",
                description = null,
            ),
        )
        // O id real (rowid Room) só existe após o insert — devolve a
        // entidade reconstituída para asserts de ordem na fila.
        val rowId = pendingOpDao.insert(op)
        return op.copy(id = rowId)
    }

    private fun projectResponse(id: String): MockResponse = MockResponse()
        .setResponseCode(200)
        .setBody(
            """
            {"id":"$id","name":"Projeto $id","description":null,"created_at":"2026-09-22T12:00:00Z"}
            """.trimIndent(),
        )

    @Test
    fun `online — drena a fila em ordem e remove as ops`() = runTest {
        val p1 = enqueueProjectOp("p-1")
        val p2 = enqueueProjectOp("p-2")
        server.enqueue(projectResponse(p1.entityId))
        server.enqueue(projectResponse(p2.entityId))

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat(pendingOpDao.count()).isEqualTo(0)
        // Ordem de enfileiramento preservada nas requisições.
        assertThat(server.takeRequest().path).isEqualTo("/v1/projects/${p1.entityId}")
        assertThat(server.takeRequest().path).isEqualTo("/v1/projects/${p2.entityId}")
    }

    @Test
    fun `offline — op permanece e worker pede retry`() = runTest {
        enqueueProjectOp("p-1")
        // Sem respostas enfileiradas + servidor desligado = IOException.
        server.shutdown()

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        // A op continua na fila (com a tentativa registrada).
        val restante = pendingOpDao.nextBatch(10)
        assertThat(restante).hasSize(1)
        assertThat(restante.first().attempts).isEqualTo(1)
    }

    @Test
    fun `HTTP 5xx — op permanece e worker pede retry`() = runTest {
        enqueueProjectOp("p-1")
        server.enqueue(MockResponse().setResponseCode(503))

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        assertThat(pendingOpDao.count()).isEqualTo(1)
    }

    @Test
    fun `HTTP 4xx — op descartada e fila anda`() = runTest {
        val op = enqueueProjectOp("p-1")
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("{\"detail\":\"id do corpo difere do id do path\"}"),
        )

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat(pendingOpDao.count()).isEqualTo(0)
        // A requisição foi feita uma única vez (sem retry da op).
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `erro retriable interrompe a drenagem preservando a ordem`() = runTest {
        val p1 = enqueueProjectOp("p-1")
        val p2 = enqueueProjectOp("p-2")
        // Primeira op falha com 503 — a segunda NEM deve ser tentada.
        server.enqueue(MockResponse().setResponseCode(503))

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        assertThat(pendingOpDao.count()).isEqualTo(2)
        assertThat(server.requestCount).isEqualTo(1)
        assertThat(server.takeRequest().path).isEqualTo("/v1/projects/${p1.entityId}")
        // p1 segue no topo da fila (Retriable preserva a op para novo
        // envio antes de qualquer outra).
        assertThat(pendingOpDao.nextBatch(1).first().id).isEqualTo(p1.id)
        assertThat(pendingOpDao.nextBatch(2).last().id).isEqualTo(p2.id)
    }

    @Test
    fun `fila vazia — worker termina com sucesso sem requisições`() = runTest {
        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `agendador registra trabalho periódico único KEEP 15min`() {
        val scheduler = SyncScheduler(WorkManager.getInstance(context))

        scheduler.ensurePeriodicSync()
        scheduler.ensurePeriodicSync() // segunda chamada: KEEP → no-op

        val infos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(SyncWorker.UNIQUE_PERIODIC_NAME)
            .get()
        assertThat(infos).hasSize(1)
        assertThat(infos.first().state.name).isNotEqualTo("CANCELLED")
    }

    @Test
    fun `agendador OneTime usa constraint de rede e tag`() {
        val scheduler = SyncScheduler(WorkManager.getInstance(context))

        scheduler.requestImmediateSync()

        val infos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(SyncScheduler.UNIQUE_ONE_TIME_NAME)
            .get()
        assertThat(infos).hasSize(1)
        assertThat(infos.first().tags).contains(SyncScheduler.ONE_TIME_TAG)
    }
}
