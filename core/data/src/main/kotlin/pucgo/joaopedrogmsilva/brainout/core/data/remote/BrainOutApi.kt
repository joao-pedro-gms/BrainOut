// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Interface Retrofit do serviço de retaguarda (E3.2).
 *
 * Contrato alinhado ao backend FastAPI (evolução do `backend-stub/`):
 * CRUD de `/v1/projects` e `/v1/tasks` mais healthcheck `/v1/ping`.
 * A URL base é resolvida pelo OkHttp client construído em
 * [RemoteDataSource] a partir de `BuildConfig.BASE_URL` — nenhum host
 * aparece hard-coded aqui.
 */
interface BrainOutApi {

    /** Healthcheck simples do serviço. */
    @GET("v1/ping")
    suspend fun ping(): PingDto

    @GET("v1/projects")
    suspend fun listProjects(): ProjectListDto

    @GET("v1/projects/{project_id}")
    suspend fun getProject(@Path("project_id") projectId: String): ProjectDto

    @POST("v1/projects")
    suspend fun createProject(@Body body: ProjectCreateDto): ProjectDto

    @PUT("v1/projects/{project_id}")
    suspend fun updateProject(
        @Path("project_id") projectId: String,
        @Body body: ProjectCreateDto,
    ): ProjectDto

    @DELETE("v1/projects/{project_id}")
    suspend fun deleteProject(@Path("project_id") projectId: String)

    @GET("v1/tasks")
    suspend fun listTasks(@Query("project_id") projectId: String? = null): TaskListDto

    @GET("v1/tasks/{task_id}")
    suspend fun getTask(@Path("task_id") taskId: String): TaskDto

    @POST("v1/tasks")
    suspend fun createTask(@Body body: TaskCreateDto): TaskDto

    @PUT("v1/tasks/{task_id}")
    suspend fun updateTask(
        @Path("task_id") taskId: String,
        @Body body: TaskCreateDto,
    ): TaskDto

    @DELETE("v1/tasks/{task_id}")
    suspend fun deleteTask(@Path("task_id") taskId: String)

    @GET("v1/tags")
    suspend fun listTags(): TagListDto

    @POST("v1/tags")
    suspend fun createTag(@Body body: TagCreateDto): TagDto

    @DELETE("v1/tags/{tag_id}")
    suspend fun deleteTag(@Path("tag_id") tagId: String)
}
