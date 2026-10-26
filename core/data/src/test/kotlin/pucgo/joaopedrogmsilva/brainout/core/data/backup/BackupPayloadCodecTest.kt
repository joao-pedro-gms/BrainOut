// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.backup

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes do **formato** do payload de backup (issue #214 — LOCAL-03).
 *
 * O round-trip que o [BackupRepositoryImplTest] faz prova que o codec
 * é coerente consigo mesmo — mas não que o **formato** continua o
 * mesmo. Um campo renomeado passa no round-trip e quebra **todo
 * backup já gravado** no aparelho de quem exportou, sem erro: o
 * arquivo é lido, o campo não é reconhecido e o dado se perde.
 *
 * O `formatVersion` do envelope existe para uma mudança desse tipo
 * poder ser **declarada** — mas só ajuda se a mudança for visível. É o
 * que estes testes fazem: fixam o conjunto de chaves do payload, que
 * é o contrato descrito em `docs/BACKUP.md` §2.2.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class BackupPayloadCodecTest {
    @Test
    fun `o payload serializado tem exatamente as chaves do formato`() {
        val json = JSONObject(String(BackupPayloadCodec.encode(payloadCompleto()), Charsets.UTF_8))
        val preferences = json.getJSONObject("preferences")

        assertThat(json.keys().asSequence().toSet())
            .isEqualTo(setOf("users", "projects", "tasks", "tags", "projectTags", "preferences"))
        assertThat(json.keysOfFirst("users"))
            .isEqualTo(setOf("id", "username", "createdAtEpochMillis"))
        assertThat(json.keysOfFirst("projects"))
            .isEqualTo(setOf("id", "ownerId", "name", "description", "createdAtEpochMillis", "isCompleted"))
        assertThat(json.keysOfFirst("tasks"))
            .isEqualTo(
                setOf(
                    "id",
                    "projectId",
                    "title",
                    "priorityCode",
                    "status",
                    "assigneeId",
                    "dueDateEpochMillis",
                    "createdAtEpochMillis",
                    "completedAtEpochMillis",
                ),
            )
        assertThat(json.keysOfFirst("tags"))
            .isEqualTo(setOf("id", "ownerId", "name", "color", "createdAtEpochMillis"))
        assertThat(json.keysOfFirst("projectTags"))
            .isEqualTo(setOf("projectId", "tagId"))
        assertThat(preferences.keys().asSequence().toSet())
            .isEqualTo(setOf("sortOrderByUser"))
    }

    @Test
    fun `campos opcionais sao omitidos quando nulos`() {
        val json = JSONObject(String(BackupPayloadCodec.encode(payloadMinimo()), Charsets.UTF_8))
        val projeto = json.firstOf("projects")
        val tarefa = json.firstOf("tasks")

        assertThat(projeto.has("description")).isFalse()
        assertThat(tarefa.has("assigneeId")).isFalse()
        assertThat(tarefa.has("dueDateEpochMillis")).isFalse()
        assertThat(tarefa.has("completedAtEpochMillis")).isFalse()
    }

    @Test
    fun `a decodificacao devolve os mesmos campos`() {
        val original = payloadCompleto()

        val lido = BackupPayloadCodec.decode(BackupPayloadCodec.encode(original))

        assertThat(lido).isEqualTo(original)
    }

    /** O primeiro elemento de um array do payload. */
    private fun JSONObject.firstOf(arrayKey: String): JSONObject = getJSONArray(arrayKey).getJSONObject(0)

    /** As chaves do primeiro elemento de um array do payload. */
    private fun JSONObject.keysOfFirst(arrayKey: String): Set<String> {
        val element = firstOf(arrayKey)
        return element.keys().asSequence().toSet()
    }

    private fun payloadCompleto(): BackupPayload =
        BackupPayload(
            users = listOf(BackupUser("u1", "Ana", 1L)),
            projects = listOf(BackupProject("p1", "u1", "Projeto", "descrição", 2L, false)),
            tasks = listOf(BackupTask("t1", "p1", "Tarefa", 2, "TODO", "u1", 3L, 4L, 5L)),
            tags = listOf(BackupTag("tag1", "u1", "Urgente", "#FF0000", 6L)),
            projectTags = listOf(BackupProjectTag("p1", "tag1")),
            preferences = BackupPreferences("system", mapOf("u1" to "created_desc")),
        )

    private fun payloadMinimo(): BackupPayload =
        BackupPayload(
            users = listOf(BackupUser("u1", "Ana", 1L)),
            projects = listOf(BackupProject("p1", "u1", "Projeto", null, 2L, false)),
            tasks = listOf(BackupTask("t1", "p1", "Tarefa", 2, "TODO", null, null, 4L, null)),
            tags = listOf(BackupTag("tag1", "u1", "Urgente", "#FF0000", 6L)),
            projectTags = listOf(BackupProjectTag("p1", "tag1")),
            preferences = BackupPreferences("system", emptyMap()),
        )
}
