// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.backup

import org.json.JSONArray
import org.json.JSONException
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
    val username: String,
    val createdAtEpochMillis: Long,
    val name: String = "",
    val email: String = "",
    val role: String = "",
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
 * Chaves do JSON do payload.
 *
 * Cada chave é escrita **uma vez** e usada no encode e no decode. A
 * duplicação importa porque o formato é o que fica **no disco**: uma
 * chave grafada diferente no lado da escrita e no lado da leitura
 * perde o dado em silêncio — o backup sai "correto" e a restauração
 * devolve o campo vazio.
 */
private const val KEY_USERS: String = "users"
private const val KEY_PROJECTS: String = "projects"
private const val KEY_TASKS: String = "tasks"
private const val KEY_TAGS: String = "tags"
private const val KEY_PROJECT_TAGS: String = "projectTags"
private const val KEY_PREFERENCES: String = "preferences"
private const val KEY_THEME_MODE: String = "themeMode"
private const val KEY_SORT_ORDER_BY_USER: String = "sortOrderByUser"

private const val KEY_ID: String = "id"
private const val KEY_OWNER_ID: String = "ownerId"
private const val KEY_NAME: String = "name"
private const val KEY_USERNAME: String = "username"
private const val KEY_EMAIL: String = "email"
private const val KEY_ROLE: String = "role"
private const val KEY_CREATED_AT_EPOCH_MILLIS: String = "createdAtEpochMillis"
private const val KEY_DESCRIPTION: String = "description"
private const val KEY_IS_COMPLETED: String = "isCompleted"
private const val KEY_PROJECT_ID: String = "projectId"
private const val KEY_TITLE: String = "title"
private const val KEY_PRIORITY_CODE: String = "priorityCode"
private const val KEY_STATUS: String = "status"
private const val KEY_ASSIGNEE_ID: String = "assigneeId"
private const val KEY_DUE_DATE_EPOCH_MILLIS: String = "dueDateEpochMillis"
private const val KEY_COMPLETED_AT_EPOCH_MILLIS: String = "completedAtEpochMillis"
private const val KEY_COLOR: String = "color"
private const val KEY_TAG_ID: String = "tagId"

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
                    themeMode = preferences.optString(KEY_THEME_MODE, "system"),
                    sortOrderByUser =
                        sortOrders.keys().asSequence().associateWith { key ->
                            sortOrders.optString(key, "")
                        },
                ),
        )
    }
}

/** Lê o JSON raiz do payload. */
private fun parsePayloadRoot(bytes: ByteArray): JSONObject =
    try {
        JSONObject(String(bytes, Charsets.UTF_8))
    } catch (error: JSONException) {
        throw BackupFormatException("Conteúdo do backup não contém JSON válido.")
    }

private fun encodeUser(user: BackupUser): JSONObject =
    JSONObject()
        .put(KEY_ID, user.id)
        .put(KEY_USERNAME, user.username)
        .put(KEY_CREATED_AT_EPOCH_MILLIS, user.createdAtEpochMillis)

private fun encodeProject(project: BackupProject): JSONObject {
    val json =
        JSONObject()
            .put(KEY_ID, project.id)
            .put(KEY_OWNER_ID, project.ownerId)
            .put(KEY_NAME, project.name)
            .put(KEY_CREATED_AT_EPOCH_MILLIS, project.createdAtEpochMillis)
            .put(KEY_IS_COMPLETED, project.isCompleted)
    if (project.description != null) json.put(KEY_DESCRIPTION, project.description)
    return json
}

private fun encodeTask(task: BackupTask): JSONObject {
    val json =
        JSONObject()
            .put(KEY_ID, task.id)
            .put(KEY_PROJECT_ID, task.projectId)
            .put(KEY_TITLE, task.title)
            .put(KEY_PRIORITY_CODE, task.priorityCode)
            .put(KEY_STATUS, task.status)
            .put(KEY_CREATED_AT_EPOCH_MILLIS, task.createdAtEpochMillis)
    if (task.assigneeId != null) json.put(KEY_ASSIGNEE_ID, task.assigneeId)
    if (task.dueDateEpochMillis != null) json.put(KEY_DUE_DATE_EPOCH_MILLIS, task.dueDateEpochMillis)
    if (task.completedAtEpochMillis != null) json.put(KEY_COMPLETED_AT_EPOCH_MILLIS, task.completedAtEpochMillis)
    return json
}

private fun encodeTag(tag: BackupTag): JSONObject =
    JSONObject()
        .put(KEY_ID, tag.id)
        .put(KEY_OWNER_ID, tag.ownerId)
        .put(KEY_NAME, tag.name)
        .put(KEY_COLOR, tag.color)
        .put(KEY_CREATED_AT_EPOCH_MILLIS, tag.createdAtEpochMillis)

private fun encodeProjectTag(projectTag: BackupProjectTag): JSONObject =
    JSONObject()
        .put(KEY_PROJECT_ID, projectTag.projectId)
        .put(KEY_TAG_ID, projectTag.tagId)

private fun decodeUser(json: JSONObject): BackupUser =
    BackupUser(
        id = json.optString(KEY_ID),
        name = json.optString(KEY_NAME),
        username = json.optString(KEY_USERNAME),
        email = json.optString(KEY_EMAIL),
        role = json.optString(KEY_ROLE),
        createdAtEpochMillis = json.optLong(KEY_CREATED_AT_EPOCH_MILLIS),
    )

private fun decodeProject(json: JSONObject): BackupProject =
    BackupProject(
        id = json.optString(KEY_ID),
        ownerId = json.optString(KEY_OWNER_ID),
        name = json.optString(KEY_NAME),
        description = json.optNullableString(KEY_DESCRIPTION),
        createdAtEpochMillis = json.optLong(KEY_CREATED_AT_EPOCH_MILLIS),
        isCompleted = json.optBoolean(KEY_IS_COMPLETED),
    )

private fun decodeTask(json: JSONObject): BackupTask =
    BackupTask(
        id = json.optString(KEY_ID),
        projectId = json.optString(KEY_PROJECT_ID),
        title = json.optString(KEY_TITLE),
        priorityCode = json.optInt(KEY_PRIORITY_CODE),
        status = json.optString(KEY_STATUS),
        assigneeId = json.optNullableString(KEY_ASSIGNEE_ID),
        dueDateEpochMillis = json.optNullableLong(KEY_DUE_DATE_EPOCH_MILLIS),
        createdAtEpochMillis = json.optLong(KEY_CREATED_AT_EPOCH_MILLIS),
        completedAtEpochMillis = json.optNullableLong(KEY_COMPLETED_AT_EPOCH_MILLIS),
    )

private fun decodeTag(json: JSONObject): BackupTag =
    BackupTag(
        id = json.optString(KEY_ID),
        ownerId = json.optString(KEY_OWNER_ID),
        name = json.optString(KEY_NAME),
        color = json.optString(KEY_COLOR),
        createdAtEpochMillis = json.optLong(KEY_CREATED_AT_EPOCH_MILLIS),
    )

private fun decodeProjectTag(json: JSONObject): BackupProjectTag =
    BackupProjectTag(
        projectId = json.optString(KEY_PROJECT_ID),
        tagId = json.optString(KEY_TAG_ID),
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
