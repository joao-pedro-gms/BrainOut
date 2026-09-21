// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs remotos (E3.2): espelham o contrato do serviço de retaguarda
 * (backend FastAPI, evolução do `backend-stub/`). Os nomes de campo
 * seguem o `snake_case` usado pelo serviço e são declarados via
 * [SerialName] — nenhum host ou contrato fica hard-coded fora daqui.
 *
 * Os DTOs são o contrato de transporte; a conversão para os modelos de
 * domínio fica a cargo do mapeamento em [RemoteDataSource] / repositórios.
 */

/** Resposta de listagem paginada por `items` usada pelos endpoints de listagem `/v1/...`. */
@Serializable
data class ProjectListDto(
    val items: List<ProjectDto> = emptyList(),
)

/**
 * Projeto no contrato remoto.
 *
 * @property id UUID do projeto.
 * @property name Nome (1..120 caracteres).
 * @property description Descrição opcional (até 500 caracteres).
 * @property createdAt Instante de criação, ISO-8601 com sufixo `Z`.
 */
@Serializable
data class ProjectDto(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("description")
    val description: String? = null,
    @SerialName("created_at")
    val createdAt: String,
)

/** Resposta de listagem de tarefas (`items`). */
@Serializable
data class TaskListDto(
    val items: List<TaskDto> = emptyList(),
)

/**
 * Tarefa no contrato remoto.
 *
 * @property id UUID da tarefa.
 * @property projectId Id do projeto dono.
 * @property title Título (1..200 caracteres).
 * @property priority Código de prioridade 0..4 (alinhado a TaskPriority
 *   do domínio).
 * @property done Flag de conclusão do stub (mapeia para status DONE).
 * @property createdAt Instante de criação, ISO-8601 com sufixo `Z`.
 */
@Serializable
data class TaskDto(
    @SerialName("id")
    val id: String,
    @SerialName("project_id")
    val projectId: String,
    @SerialName("title")
    val title: String,
    @SerialName("priority")
    val priority: Int = 0,
    @SerialName("done")
    val done: Boolean = false,
    @SerialName("created_at")
    val createdAt: String,
)

/**
 * Corpo de criação de tarefa (`POST /v1/tasks`) — não leva `id` nem
 * `created_at`, gerados pelo servidor.
 */
@Serializable
data class TaskCreateDto(
    @SerialName("project_id")
    val projectId: String,
    @SerialName("title")
    val title: String,
    @SerialName("priority")
    val priority: Int = 0,
    @SerialName("done")
    val done: Boolean = false,
)

/**
 * Corpo de criação de projeto (`POST /v1/projects`) — não leva `id` nem
 * `created_at`, gerados pelo servidor.
 */
@Serializable
data class ProjectCreateDto(
    @SerialName("name")
    val name: String,
    @SerialName("description")
    val description: String? = null,
)

/**
 * Tag no contrato remoto (espelho da entidade Room `TagEntity`).
 * O stub FastAPI ainda não expõe `/v1/tags`; o DTO fica pronto para o
 * endpoint definitivo mantendo o mesmo padrão snake_case.
 */
@Serializable
data class TagDto(
    @SerialName("id")
    val id: String,
    @SerialName("owner_id")
    val ownerId: String,
    @SerialName("name")
    val name: String,
    @SerialName("color")
    val color: String,
    @SerialName("created_at")
    val createdAt: String,
)

/** Resposta do healthcheck `GET /v1/ping`. */
@Serializable
data class PingDto(
    @SerialName("pong")
    val pong: Boolean,
)
