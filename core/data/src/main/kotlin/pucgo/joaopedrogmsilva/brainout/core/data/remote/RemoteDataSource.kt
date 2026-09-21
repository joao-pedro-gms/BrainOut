// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.remote

import android.util.Log
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Fonte de dados remota (E3.2): envolve [BrainOutApi] e centraliza a
 * construção do cliente Retrofit/OkHttp a partir da URL base injetada
 * via `BuildConfig.BASE_URL` (flavor `debug`/`release`).
 *
 * Erros de rede/HTTP são logados e re-sinalizados como exceção para o
 * chamador decidir (fila offline do E3.4). O logger é injetável para
 * que os testes unitários não dependam de `android.util.Log`.
 */
class RemoteDataSource(
    baseUrl: String,
    private val logError: (String) -> Unit = { message -> Log.e(TAG, message) },
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val api: BrainOutApi = retrofit.create(BrainOutApi::class.java)

    /** Healthcheck do serviço (GET /v1/ping). */
    suspend fun ping(): Boolean = try {
        api.ping().pong
    } catch (e: Exception) {
        logError("ping falhou: ${e.message}")
        false
    }

    /** Lista todos os projetos (GET /v1/projects). */
    suspend fun listProjects(): List<ProjectDto> = try {
        api.listProjects().items
    } catch (e: Exception) {
        logError("listProjects falhou: ${e.message}")
        throw e
    }

    /** Busca um projeto por id (GET /v1/projects/{id}). */
    suspend fun getProject(projectId: String): ProjectDto = try {
        api.getProject(projectId)
    } catch (e: Exception) {
        logError("getProject($projectId) falhou: ${e.message}")
        throw e
    }

    /** Cria um projeto (POST /v1/projects). */
    suspend fun createProject(name: String, description: String?): ProjectDto = try {
        api.createProject(ProjectCreateDto(name = name, description = description))
    } catch (e: Exception) {
        logError("createProject falhou: ${e.message}")
        throw e
    }

    /** Atualiza um projeto (PUT /v1/projects/{id}). */
    suspend fun updateProject(projectId: String, name: String, description: String?): ProjectDto = try {
        api.updateProject(projectId, ProjectCreateDto(name = name, description = description))
    } catch (e: Exception) {
        logError("updateProject($projectId) falhou: ${e.message}")
        throw e
    }

    /** Remove um projeto (DELETE /v1/projects/{id}). */
    suspend fun deleteProject(projectId: String) = try {
        api.deleteProject(projectId)
    } catch (e: Exception) {
        logError("deleteProject($projectId) falhou: ${e.message}")
        throw e
    }

    /** Lista tarefas, opcionalmente filtradas por projeto (GET /v1/tasks). */
    suspend fun listTasks(projectId: String? = null): List<TaskDto> = try {
        api.listTasks(projectId).items
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
    ): TaskDto = try {
        api.createTask(TaskCreateDto(projectId = projectId, title = title, priority = priority, done = done))
    } catch (e: Exception) {
        logError("createTask falhou: ${e.message}")
        throw e
    }

    companion object {
        private const val TAG = "BrainOutRemote"
    }
}
