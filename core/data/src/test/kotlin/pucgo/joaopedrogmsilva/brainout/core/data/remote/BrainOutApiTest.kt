// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.remote

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

/**
 * Testes unitários do cliente remoto (E3.2): [BrainOutApi] contra
 * [MockWebServer], com respostas JSON idênticas às emitidas pelo
 * backend FastAPI (`backend-stub/server.py`) — campos snake_case,
 * listagem empacotada em `{"items": [...]}`.
 */
class BrainOutApiTest {

    private lateinit var server: MockWebServer
    private lateinit var remote: RemoteDataSource

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        // Logger no-op: testes unitários não devem tocar `android.util.Log`.
        remote = RemoteDataSource(baseUrl = server.url("/").toString(), logError = { })
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `ping retorna pong true`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"pong": true}"""),
        )

        assertTrue(remote.ping())

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/v1/ping", request.path)
    }

    @Test
    fun `listProjects com 200 e lista vazia`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"items": []}"""),
        )

        val projects = remote.listProjects()

        assertTrue(projects.isEmpty())
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/v1/projects", request.path)
    }

    @Test
    fun `listProjects com 200 parseia um projeto do backend`() = runTest {
        // Exemplo fiel do response real do backend-stub (POST /v1/projects):
        // {"id": "<uuid-hex>", "name": ..., "description": ..., "created_at": "...Z"}
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {"items": [{
                      "id": "a1b2c3d4e5f67890a1b2c3d4e5f67890",
                      "name": "Projeto PUC",
                      "description": "Trabalho de ADS",
                      "created_at": "2026-09-21T11:00:00Z"
                    }]}
                    """.trimIndent(),
                ),
        )

        val projects = remote.listProjects()

        assertEquals(1, projects.size)
        with(projects.single()) {
            assertEquals("a1b2c3d4e5f67890a1b2c3d4e5f67890", id)
            assertEquals("Projeto PUC", name)
            assertEquals("Trabalho de ADS", description)
            assertEquals("2026-09-21T11:00:00Z", createdAt)
        }
    }

    @Test
    fun `listTasks parseia tarefa com snake_case e defaults`() = runTest {
        // Response real do backend-stub: priority=0 e done=false quando omitidos.
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {"items": [{
                      "id": "0f1e2d3c4b5a69788796a5b4c3d2e1f0",
                      "project_id": "a1b2c3d4e5f67890a1b2c3d4e5f67890",
                      "title": "Estudar para a prova",
                      "created_at": "2026-09-21T11:05:00Z"
                    }]}
                    """.trimIndent(),
                ),
        )

        val tasks = remote.listTasks()

        assertEquals(1, tasks.size)
        with(tasks.single()) {
            assertEquals("a1b2c3d4e5f67890a1b2c3d4e5f67890", projectId)
            assertEquals(0, priority)
            assertEquals(false, done)
        }
        val request = server.takeRequest()
        assertEquals("/v1/tasks", request.path)
    }

    @Test
    fun `createProject envia JSON snake_case e parseia resposta 201`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(
                    """
                    {
                      "id": "b2c3d4e5f67890a1b2c3d4e5f67890a1",
                      "name": "Novo projeto",
                      "description": null,
                      "created_at": "2026-09-21T11:10:00Z"
                    }
                    """.trimIndent(),
                ),
        )

        val created = remote.createProject(name = "Novo projeto", description = null)

        assertEquals("b2c3d4e5f67890a1b2c3d4e5f67890a1", created.id)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/projects", request.path)
        val sentBody = request.body.readUtf8()
        // O corpo enviado usa o contrato do backend (sem id/created_at).
        assertTrue(sentBody.contains("\"name\""))
        assertTrue(!sentBody.contains("created_at"))
    }

    @Test
    fun `createTask envia project_id e parseia resposta 201`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(
                    """
                    {
                      "id": "c3d4e5f67890a1b2c3d4e5f67890a1b2",
                      "project_id": "a1b2c3d4e5f67890a1b2c3d4e5f67890",
                      "title": "Tarefa nova",
                      "priority": 2,
                      "done": false,
                      "created_at": "2026-09-21T11:15:00Z"
                    }
                    """.trimIndent(),
                ),
        )

        val created = remote.createTask(
            projectId = "a1b2c3d4e5f67890a1b2c3d4e5f67890",
            title = "Tarefa nova",
            priority = 2,
        )

        assertEquals("c3d4e5f67890a1b2c3d4e5f67890a1b2", created.id)
        assertEquals(2, created.priority)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/tasks", request.path)
        assertTrue(request.body.readUtf8().contains("\"project_id\""))
    }

    @Test
    fun `erro 404 do backend lanca HttpException`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("""{"detail": "project not found"}"""),
        )

        val resultado = runCatching { remote.getProject("nao-existe") }
        val excecao = resultado.exceptionOrNull()

        assertTrue("esperava HttpException, veio: $excecao", excecao is HttpException)
    }
}
