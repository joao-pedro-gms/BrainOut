// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.backup

import org.json.JSONArray
import org.json.JSONObject
import pucgo.joaopedrogmsilva.brainout.core.domain.error.BackupFormatException

/**
 * Conteúdo lógico de um backup local (issue #214 — LOCAL-03).
 *
 * O formato em disco é JSON, mas a manipulação acontece sobre estas
 * estruturas — assim o codec fica testável sem tocar em disco e a
 * camada de dados só conhece o domínio de dados, não o JSON.
 *
 * **O que NÃO entra no backup:** hash de senha, pepper do
 * Android Keystore e a sessão ativa. A senha do backup é a credencial
 * de cifragem; as contas restauradas recebem a senha escolhida no
 * aparelho de destino (ver `docs/BACKUP.md`).
 */
internal data class BackupPayload(
    val users: List<BackupUser>,
    val projects: List<BackupProject>,
    val tasks: List<BackupTask>,
    val tags: List<BackupTag>,
    val projectTags: List<BackupProjectTag>,
    val preferences: BackupPreferences,
)

/** Conta local exportada, sem material de senha. */
internal data class BackupUser(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val createdAtEpochMillis: Long,
)

/** Projeto exportado. */
internal data class BackupProject(
    val id: String,
    val ownerId: String,
    val name: String,
    val description: String?,
    val createdAtEpochMillis: Long,
    val isCompleted: Boolean,
)

/** Tarefa exportada. */
internal data class BackupTask(
    val id: String,
    val projectId: String,
    val title: String,
    val priorityCode: Int,
    val status: String,
    val assigneeId: String?,
    val dueDateEpochMillis: Long?,
    val createdAtEpochMillis: Long,
    val completedAtEpochMillis: Long?,
)

/** Etiqueta exportada. */
internal data class BackupTag(
    val id: String,
    val ownerId: String,
    val name: String,
    val color: String,
    val createdAtEpochMillis: Long,
)

/** Associação N:N entre projeto e etiqueta. */
internal data class BackupProjectTag(
    val projectId: String,
    val tagId: String,
)

/**
 * Preferências que atravessam a instalação.
 *
 * @property themeMode modo de tema (chave de [pucgo.joaopedrogmsilva.brainout.core.domain.model.ThemeMode]).
 * @property sortOrderByUser ordenação da listagem, por id de conta.
 */
internal data class BackupPreferences(
    val themeMode: String,
    val sortOrderByUser: Map<String, String>,
)

/**
 * Codec do payload lógico do backup para JSON.
 *
 * Usa `org.json` (disponível na plataforma Android) em vez de
 * adicionar uma dependência de serialização ao projeto — o formato
 * é pequeno, auto-descritivo e versionado pelo envelope.
 *
 * As conversões de cada tipo ficam em funções de nível superior do
 * arquivo (e não dentro do objeto) para que o objeto de codec
 * permaneça pequeno e legível.
 */
internal object BackupPayloadCodec {
    private const val KEY_USERS: String = "users"
    private const val KEY_PROJECTS: String = "projects"
    private const val KEY_TASKS: String = "tasks"
    private const val KEY_TAGS: String = "tags"
    private const val KEY_PROJECT_TAGS: String = "projectTags"
    private const val KEY_PREFERENCES: String = "preferences"
    private const val KEY_THEME_MODE: String = "themeMode"
    private const val KEY_SORT_ORDER_BY_USER: String = "sortOrderByUser"

    /** Serializa o payload em JSON UTF-8. */
    fun encode(payload: BackupPayload): ByteArray {
        val root =
            JSONObject()
                .put(KEY_USERS, JSONArray().apply { payload.users.forEach { put(encodeUser(it)) } })
                .put(KEY_PROJECTS, JSONArray().apply { payload.projects.forEach { put(encodeProject(it)) } })
                .put(KEY_TASKS, JSONArray().apply { payload.tasks.forEach { put(encodeTask(it)) } })
                .put(KEY_TAGS, JSONArray().apply { payload.tags.forEach { put(encodeTag(it)) } })
                .put(KEY_PROJECT_TAGS, JSONArray().apply { payload.projectTags.forEach { put(encodeProjectTag(it)) } })
                .put(
                    KEY_PREFERENCES,
                    JSONObject()
                        .put(KEY_THEME_MODE, payload.preferences.themeMode)
                        .put(
                            KEY_SORT_ORDER_BY_USER,
                            JSONObject().apply {
                                payload.preferences.sortOrderByUser.forEach { (userId, order) -> put(userId, order) }
                            },
                        ),
                )
        return root.toString().toByteArray(Charsets.UTF_8)
    }

    /** Lê o payload de JSON UTF-8. */
    fun decode(bytes: ByteArray): BackupPayload {
        val root = parsePayloadRoot(bytes)
        val preferences = root.optJSONObject(KEY_PREFERENCES) ?: JSONObject()
        val sortOrders = preferences.optJSONObject(KEY_SORT_ORDER_BY_USER) ?: JSONObject()
        return BackupPayload(
            users = root.optJSONArray(KEY_USERS).mapObjects(::decodeUser),
            projects = root.optJSONArray(KEY_PROJECTS).mapObjects(::decodeProject),
            tasks = root.optJSONArray(KEY_TASKS).mapObjects(::decodeTask),
            tags = root.optJSONArray(KEY_TAGS).mapObjects(::decodeTag),
            projectTags = root.optJSONArray(KEY_PROJECT_TAGS).mapObjects(::decodeProjectTag),
            preferences =
                BackupPreferences(
                    themeMode = preferences.optString(KEY_THEME_MODE, ""),
                    sortOrderByUser =
                        sortOrders.keys().asSequence().associateWith { key ->
                            sortOrders.optString(key, "")
                        },
                ),
        )
    }
}

/** Lê o JSON raiz do payload. */
@Suppress("TooGenericExceptionCaught")
private fun parsePayloadRoot(bytes: ByteArray): JSONObject =
    try {
        JSONObject(String(bytes, Charsets.UTF_8))
    } catch (error: Exception) {
        throw BackupFormatException("Conteúdo do backup não contém JSON válido.")
    }

private fun encodeUser(user: BackupUser): JSONObject =
    JSONObject()
        .put("id", user.id)
        .put("name", user.name)
        .put("email", user.email)
        .put("role", user.role)
        .put("createdAtEpochMillis", user.createdAtEpochMillis)

private fun encodeProject(project: BackupProject): JSONObject {
    val json =
        JSONObject()
            .put("id", project.id)
            .put("ownerId", project.ownerId)
            .put("name", project.name)
            .put("createdAtEpochMillis", project.createdAtEpochMillis)
            .put("isCompleted", project.isCompleted)
    if (project.description != null) json.put("description", project.description)
    return json
}

private fun encodeTask(task: BackupTask): JSONObject {
    val json =
        JSONObject()
            .put("id", task.id)
            .put("projectId", task.projectId)
            .put("title", task.title)
            .put("priorityCode", task.priorityCode)
            .put("status", task.status)
            .put("createdAtEpochMillis", task.createdAtEpochMillis)
    if (task.assigneeId != null) json.put("assigneeId", task.assigneeId)
    if (task.dueDateEpochMillis != null) json.put("dueDateEpochMillis", task.dueDateEpochMillis)
    if (task.completedAtEpochMillis != null) json.put("completedAtEpochMillis", task.completedAtEpochMillis)
    return json
}

private fun encodeTag(tag: BackupTag): JSONObject =
    JSONObject()
        .put("id", tag.id)
        .put("ownerId", tag.ownerId)
        .put("name", tag.name)
        .put("color", tag.color)
        .put("createdAtEpochMillis", tag.createdAtEpochMillis)

private fun encodeProjectTag(projectTag: BackupProjectTag): JSONObject =
    JSONObject()
        .put("projectId", projectTag.projectId)
        .put("tagId", projectTag.tagId)

private fun decodeUser(json: JSONObject): BackupUser =
    BackupUser(
        id = json.optString("id"),
        name = json.optString("name"),
        email = json.optString("email"),
        role = json.optString("role"),
        createdAtEpochMillis = json.optLong("createdAtEpochMillis"),
    )

private fun decodeProject(json: JSONObject): BackupProject =
    BackupProject(
        id = json.optString("id"),
        ownerId = json.optString("ownerId"),
        name = json.optString("name"),
        description = json.optNullableString("description"),
        createdAtEpochMillis = json.optLong("createdAtEpochMillis"),
        isCompleted = json.optBoolean("isCompleted"),
    )

private fun decodeTask(json: JSONObject): BackupTask =
    BackupTask(
        id = json.optString("id"),
        projectId = json.optString("projectId"),
        title = json.optString("title"),
        priorityCode = json.optInt("priorityCode"),
        status = json.optString("status"),
        assigneeId = json.optNullableString("assigneeId"),
        dueDateEpochMillis = json.optNullableLong("dueDateEpochMillis"),
        createdAtEpochMillis = json.optLong("createdAtEpochMillis"),
        completedAtEpochMillis = json.optNullableLong("completedAtEpochMillis"),
    )

private fun decodeTag(json: JSONObject): BackupTag =
    BackupTag(
        id = json.optString("id"),
        ownerId = json.optString("ownerId"),
        name = json.optString("name"),
        color = json.optString("color"),
        createdAtEpochMillis = json.optLong("createdAtEpochMillis"),
    )

private fun decodeProjectTag(json: JSONObject): BackupProjectTag =
    BackupProjectTag(
        projectId = json.optString("projectId"),
        tagId = json.optString("tagId"),
    )

/** Lê um array JSON opcional, mapeando cada elemento. */
private fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    val result = ArrayList<T>(length())
    for (index in 0 until length()) {
        val element = optJSONObject(index) ?: continue
        result.add(transform(element))
    }
    return result
}

/** Lê uma string opcional (ausente ou nula vira `null`). */
private fun JSONObject.optNullableString(key: String): String? = if (!has(key) || isNull(key)) null else optString(key)

/** Lê um instante opcional (ausente ou nulo vira `null`). */
private fun JSONObject.optNullableLong(key: String): Long? = if (!has(key) || isNull(key)) null else optLong(key)
