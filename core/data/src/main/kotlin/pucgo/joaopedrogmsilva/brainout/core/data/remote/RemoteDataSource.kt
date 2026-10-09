// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.remote

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pucgo.joaopedrogmsilva.brainout.core.data.util.logDebug
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Fonte de dados remota (E3.2): envolve [BrainOutApi] e centraliza a
 * construção do cliente Retrofit/OkHttp a partir da URL base injetada
 * via `BuildConfig.BASE_URL` (flavor `debug`/`release`).
 *
 * Erros de rede/HTTP são logados e re-sinalizados como exceção para o
 * chamador decidir (fila offline do E3.4). O logger é injetável para
 * que os testes unitários não dependerem de `android.util.Log`.
 */
class RemoteDataSource(
    baseUrl: String,
    private val logError: (String) -> Unit = { message -> runCatching { Log.e(TAG, message) } },
    private val loggingEnabled: Boolean = true,
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private val okHttp: OkHttpClient =
        OkHttpClient
            .Builder()
            .apply {
                if (loggingEnabled) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        },
                    )
                }
            }.build()

    private val retrofit: Retrofit =
        Retrofit
            .Builder()
            .baseUrl(baseUrl)
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    val api: BrainOutApi = retrofit.create(BrainOutApi::class.java)

    /** Healthcheck do serviço (GET /v1/ping). */
    suspend fun ping(): Boolean =
        try {
            logDebug(TAG, "Chamando ping (GET /v1/ping)")
            val res = api.ping().pong
            logDebug(TAG, "Ping executado com sucesso: pong=$res")
            res
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logError("ping falhou: ${e.message}")
            false
        }

    /** Lista todos os projetos (GET /v1/projects). */
    suspend fun listProjects(): List<ProjectDto> =
        try {
            logDebug(TAG, "Chamando listProjects (GET /v1/projects)")
            val result = api.listProjects().items
            logDebug(TAG, "listProjects retornou ${result.size} projetos")
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logError("listProjects falhou: ${e.message}")
            throw e
        }

    /** Busca um projeto por id (GET /v1/projects/{id}). */
    suspend fun getProject(projectId: String): ProjectDto =
        try {
            logDebug(TAG, "Chamando getProject (GET /v1/projects/$projectId)")
            val result = api.getProject(projectId)
            logDebug(TAG, "getProject($projectId) retornou sucesso")
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logError("getProject($projectId) falhou: ${e.message}")
            throw e
        }

    /** Cria um projeto (POST /v1/projects). */
    suspend fun createProject(
        name: String,
        description: String?,
    ): ProjectDto =
        try {
            logDebug(TAG, "Chamando createProject (POST /v1/projects) name=$name")
            val result = api.createProject(ProjectCreateDto(name = name, description = description))
            logDebug(TAG, "createProject retornou id=${result.id}")
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logError("createProject falhou: ${e.message}")
            throw e
        }

    /** Atualiza um projeto (PUT /v1/projects/{id}). */
    suspend fun updateProject(
        projectId: String,
        name: String,
        description: String?,
    ): ProjectDto =
        try {
            logDebug(TAG, "Chamando updateProject (PUT /v1/projects/$projectId)")
            val result =
                api.updateProject(
                    projectId,
                    ProjectCreateDto(id = projectId, name = name, description = description),
                )
            logDebug(TAG, "updateProject($projectId) executado com sucesso")
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logError("updateProject($projectId) falhou: ${e.message}")
            throw e
        }

    /** Remove um projeto (DELETE /v1/projects/{id}). */
    suspend fun deleteProject(projectId: String) =
        try {
            logDebug(TAG, "Chamando deleteProject (DELETE /v1/projects/$projectId)")
            api.deleteProject(projectId)
            logDebug(TAG, "deleteProject($projectId) executado com sucesso")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logError("deleteProject($projectId) falhou: ${e.message}")
            throw e
        }

    /** Lista tarefas, opcionalmente filtradas por projeto (GET /v1/tasks). */
    suspend fun listTasks(projectId: String? = null): List<TaskDto> =
        try {
            logDebug(TAG, "Chamando listTasks (GET /v1/tasks?projectId=$projectId)")
            val result = api.listTasks(projectId).items
            logDebug(TAG, "listTasks retornou ${result.size} tarefas")
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logError("listTasks falhou: ${e.message}")
            throw e
        }

    /** Cria uma tarefa (POST /v1/tasks). */
    suspend fun createTask(
        projectId: String,
        title: String,
        priority: Int = 0,
        done: Boolean = false,
    ): TaskDto =
        try {
            logDebug(TAG, "Chamando createTask (POST /v1/tasks) projectId=$projectId, title=$title")
            val payload =
                TaskCreateDto(
                    projectId = projectId,
                    title = title,
                    priority = priority,
                    done = done,
                )
            val result = api.createTask(payload)
            logDebug(TAG, "createTask retornou id=${result.id}")
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logError("createTask falhou: ${e.message}")
            throw e
        }

    /**
     * Atualiza (upsert) uma tarefa (PUT /v1/tasks/{id}) — o `id` vai
     * no path e no body; divergência é 400 no contrato do stub.
     */
    suspend fun updateTask(
        taskId: String,
        projectId: String,
        title: String,
        priority: Int,
        done: Boolean,
    ): TaskDto =
        try {
            logDebug(TAG, "Chamando updateTask (PUT /v1/tasks/$taskId) done=$done")
            val result =
                api.updateTask(
                    taskId,
                    TaskCreateDto(
                        id = taskId,
                        projectId = projectId,
                        title = title,
                        priority = priority,
                        done = done,
                    ),
                )
            logDebug(TAG, "updateTask($taskId) executado com sucesso")
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logError("updateTask($taskId) falhou: ${e.message}")
            throw e
        }

    /** Remove uma tarefa (DELETE /v1/tasks/{id}) — 204 mesmo ausente. */
    suspend fun deleteTask(taskId: String) =
        try {
            logDebug(TAG, "Chamando deleteTask (DELETE /v1/tasks/$taskId)")
            api.deleteTask(taskId)
            logDebug(TAG, "deleteTask($taskId) executado com sucesso")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logError("deleteTask($taskId) falhou: ${e.message}")
            throw e
        }

    /** Lista tags (GET /v1/tags). */
    suspend fun listTags(): List<TagDto> =
        try {
            logDebug(TAG, "Chamando listTags (GET /v1/tags)")
            val result = api.listTags().items
            logDebug(TAG, "listTags retornou ${result.size} tags")
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logError("listTags falhou: ${e.message}")
            throw e
        }

    /** Cria tag (POST /v1/tags) — o servidor gera o id. */
    suspend fun createTag(
        name: String,
        color: String,
        id: String? = null,
    ): TagDto =
        try {
            logDebug(TAG, "Chamando createTag (POST /v1/tags) name=$name, color=$color")
            val result = api.createTag(TagCreateDto(id = id, name = name, color = color))
            logDebug(TAG, "createTag retornou id=${result.id}")
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logError("createTag falhou: ${e.message}")
            throw e
        }

    /** Remove tag (DELETE /v1/tags/{id}) — 204 mesmo ausente. */
    suspend fun deleteTag(tagId: String) =
        try {
            logDebug(TAG, "Chamando deleteTag (DELETE /v1/tags/$tagId)")
            api.deleteTag(tagId)
            logDebug(TAG, "deleteTag($tagId) executado com sucesso")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logError("deleteTag($tagId) falhou: ${e.message}")
            throw e
        }

    companion object {
        private const val TAG = "BrainOut:RemoteDataSource"
    }
}
