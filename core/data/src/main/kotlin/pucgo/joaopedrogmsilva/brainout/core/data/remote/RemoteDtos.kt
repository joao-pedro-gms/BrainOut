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
    @SerialName("id")
    val id: String? = null,
    @SerialName("project_id")
    val projectId: String,
    @SerialName("title")
    val title: String,
    @SerialName("priority")
    val priority: Int = 0,
    @SerialName("done")
    val done: Boolean = false,
)

/*
 * Corpo de criação/upsert de projeto (`POST`/`PUT /v1/projects`). Não leva
 * `created_at`, gerado pelo servidor. `id` é opcional (política
 * cliente-supplied do stub): em PUT o dispatcher o informa no body
 * (obrigatório no upsert, divergência com o path é 400); em POST fica
 * nulo e o servidor gera um UUID.
 */
@Serializable
data class ProjectCreateDto(
    @SerialName("id")
    val id: String? = null,
    @SerialName("name")
    val name: String,
    @SerialName("description")
    val description: String? = null,
)

/** Resposta de listagem de tags (`items`). */
@Serializable
data class TagListDto(
    val items: List<TagDto> = emptyList(),
)

/**
 * Corpo de criação de tag (`POST /v1/tags`) — o servidor gera o `id`
 * (tags são vocabulário controlado; sem identidade local forte).
 */
@Serializable
data class TagCreateDto(
    @SerialName("name")
    val name: String,
    @SerialName("color")
    val color: String = "#888888",
)

/**
 * Tag no contrato remoto (espelho da entidade Room `TagEntity`).
 *
 * `owner_id` e `created_at` são opcionais no decode: o stub gera o id
 * no servidor e nem sempre devolve todos os campos em respostas de
 * associação; o dispatcher do sync só precisa do `id`/`name`.
 */
@Serializable
data class TagDto(
    @SerialName("id")
    val id: String,
    @SerialName("owner_id")
    val ownerId: String? = null,
    @SerialName("name")
    val name: String,
    @SerialName("color")
    val color: String = "#888888",
    @SerialName("created_at")
    val createdAt: String? = null,
)

/** Resposta do healthcheck `GET /v1/ping`. */
@Serializable
data class PingDto(
    @SerialName("pong")
    val pong: Boolean,
)
