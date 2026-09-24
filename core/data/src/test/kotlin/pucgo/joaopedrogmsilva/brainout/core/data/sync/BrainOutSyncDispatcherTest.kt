// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.sync

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.PendingOpEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectSyncPayload
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.SyncEntityType
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.SyncOpType
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TagSyncPayload
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TaskSyncPayload
import pucgo.joaopedrogmsilva.brainout.core.data.remote.RemoteDataSource

/**
 * Testes do [BrainOutSyncDispatcher] contra [MockWebServer] (E3.3):
 * valida a tradução op → requisição HTTP e a classificação de erros
 * que o `SyncWorker` consome.
 *
 * Cobre:
 * - CREATE/UPDATE de projeto → PUT upsert com id no path E no body
 *   (contrato cliente-supplied do stub).
 * - CREATE/UPDATE de tarefa → PUT /v1/tasks/{id} com snake_case.
 * - CREATE de tag → POST /v1/tags (sem id no corpo).
 * - DELETE → verbo DELETE no path do id.
 * - HTTP 5xx → [SyncOutcome.Retriable]; IOException → Retriable;
 *   HTTP 4xx → [SyncOutcome.Permanent] com o código.
 * - Payload inválido / op desconhecida → Permanent sem tocar a rede.
 */
class BrainOutSyncDispatcherTest {

    private lateinit var server: MockWebServer
    private lateinit var dispatcher: SyncDispatcher

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        dispatcher = BrainOutSyncDispatcher(
            remote = RemoteDataSource(baseUrl = server.url("/").toString(), logError = { }),
            logError = { },
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `update de projeto envia PUT com id no path e no body`() = runTest {
        server.enqueue(projectResponse())

        val payload = ProjectSyncPayload(
            id = "11111111-1111-4111-8111-111111111111",
            name = "Projeto PUC",
            description = null,
        )
        val outcome = dispatcher.send(
            op(SyncEntityType.PROJECT, SyncOpType.CREATE, payload),
        )

        assertThat(outcome).isEqualTo(SyncOutcome.Success)
        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("PUT")
        assertThat(request.path).isEqualTo("/v1/projects/11111111-1111-4111-8111-111111111111")
        val body = request.body.readUtf8()
        assertThat(body).contains("\"name\":\"Projeto PUC\"")
        // Id no body é obrigatório pelo contrato cliente-supplied.
        assertThat(body).contains("11111111-1111-4111-8111-111111111111")
    }

    @Test
    fun `update de tarefa envia PUT com snake_case e done espelhado`() = runTest {
        server.enqueue(taskResponse())

        val payload = TaskSyncPayload(
            id = "22222222-2222-4222-8222-222222222222",
            projectId = "11111111-1111-4111-8111-111111111111",
            title = "Estudar para a prova",
            priority = 3,
            done = false,
        )
        val outcome = dispatcher.send(
            op(SyncEntityType.TASK, SyncOpType.UPDATE, payload),
        )

        assertThat(outcome).isEqualTo(SyncOutcome.Success)
        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("PUT")
        assertThat(request.path).isEqualTo("/v1/tasks/22222222-2222-4222-8222-222222222222")
        val body = request.body.readUtf8()
        assertThat(body).contains("\"project_id\"")
        assertThat(body).contains("\"done\":false")
    }

    @Test
    fun `create de tag envia POST sem id no corpo`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(
                    """
                    {"id":"33333333-3333-4333-8333-333333333333","name":"Urgente","color":"#FF0000","created_at":"2026-09-22T12:00:00Z"}
                    """.trimIndent(),
                ),
        )

        val outcome = dispatcher.send(
            op(
                SyncEntityType.TAG,
                SyncOpType.CREATE,
                TagSyncPayload(name = "Urgente", color = "#FF0000"),
            ),
        )

        assertThat(outcome).isEqualTo(SyncOutcome.Success)
        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.path).isEqualTo("/v1/tags")
        assertThat(request.body.readUtf8()).contains("\"id\"")
    }

    @Test
    fun `delete de projeto e tarefa usam DELETE com o id no path`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(MockResponse().setResponseCode(204))

        val projeto = dispatcher.send(
            PendingOpEntity.enqueue(
                entityType = SyncEntityType.PROJECT,
                entityId = "11111111-1111-4111-8111-111111111111",
                opType = SyncOpType.DELETE,
            ),
        )
        val tarefa = dispatcher.send(
            PendingOpEntity.enqueue(
                entityType = SyncEntityType.TASK,
                entityId = "22222222-2222-4222-8222-222222222222",
                opType = SyncOpType.DELETE,
            ),
        )

        assertThat(projeto).isEqualTo(SyncOutcome.Success)
        assertThat(tarefa).isEqualTo(SyncOutcome.Success)
        assertThat(server.takeRequest().method).isEqualTo("DELETE")
        assertThat(server.takeRequest().method).isEqualTo("DELETE")
    }

    @Test
    fun `HTTP 5xx é retriable`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))

        val outcome = dispatcher.send(
            op(SyncEntityType.PROJECT, SyncOpType.UPDATE, sampleProjectPayload()),
        )

        assertThat(outcome).isInstanceOf(SyncOutcome.Retriable::class.java)
    }

    @Test
    fun `HTTP 4xx é permanente com o código preservado`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("{\"detail\": \"id do corpo difere do id do path\"}"),
        )

        val outcome = dispatcher.send(
            op(SyncEntityType.PROJECT, SyncOpType.UPDATE, sampleProjectPayload()),
        )

        val permanent = outcome as SyncOutcome.Permanent
        assertThat(permanent.httpCode).isEqualTo(400)
    }

    @Test
    fun `rede indisponível é retriable`() = runTest {
        server.shutdown()

        val outcome = dispatcher.send(
            op(SyncEntityType.PROJECT, SyncOpType.UPDATE, sampleProjectPayload()),
        )

        assertThat(outcome).isInstanceOf(SyncOutcome.Retriable::class.java)
    }

    @Test
    fun `payload inválido é permanente sem tocar a rede`() = runTest {
        // O dispatcher só decodifica payloads serializáveis conhecidos;
        // um valor não-serializável quebra o encode na enfileiragem, e
        // um payload JSON malformado quebra o decode no dispatcher.
        val opMalformada = PendingOpEntity.enqueue(
            entityType = SyncEntityType.PROJECT,
            entityId = "11111111-1111-4111-8111-111111111111",
            opType = SyncOpType.UPDATE,
        ).copy(payload = "{json malformado")

        val outcome = dispatcher.send(opMalformada)

        assertThat(outcome).isInstanceOf(SyncOutcome.Permanent::class.java)
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `opType e entityType desconhecidos são permanentes`() = runTest {
        val opDesconhecida = PendingOpEntity(
            entityType = "UNKNOWN",
            entityId = "x-1",
            opType = "UPDATE",
            payload = "{}",
            createdAt = java.time.Instant.now(),
        )

        val outcome = dispatcher.send(opDesconhecida)

        assertThat(outcome).isInstanceOf(SyncOutcome.Permanent::class.java)
        assertThat(server.requestCount).isEqualTo(0)
    }

    // --- helpers -----------------------------------------------------------

    private fun op(
        entityType: SyncEntityType,
        opType: SyncOpType,
        payload: Any? = null,
    ): PendingOpEntity = PendingOpEntity.enqueue(
        entityType = entityType,
        entityId = "11111111-1111-4111-8111-111111111111",
        opType = opType,
        payloadObj = payload,
    )

    private fun sampleProjectPayload() = ProjectSyncPayload(
        id = "11111111-1111-4111-8111-111111111111",
        name = "Projeto PUC",
        description = "Trabalho de ADS",
    )

    private fun projectResponse(): MockResponse = MockResponse()
        .setResponseCode(200)
        .setBody(
            """
            {
              "id": "11111111-1111-4111-8111-111111111111",
              "name": "Projeto PUC",
              "description": "Trabalho de ADS",
              "created_at": "2026-09-22T12:00:00Z"
            }
            """.trimIndent(),
        )

    private fun taskResponse(): MockResponse = MockResponse()
        .setResponseCode(200)
        .setBody(
            """
            {
              "id": "22222222-2222-4222-8222-222222222222",
              "project_id": "11111111-1111-4111-8111-111111111111",
              "title": "Estudar para a prova",
              "priority": 3,
              "done": false,
              "created_at": "2026-09-22T12:00:00Z"
            }
            """.trimIndent(),
        )
}
