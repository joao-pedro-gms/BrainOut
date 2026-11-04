// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.backup

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase
import pucgo.joaopedrogmsilva.brainout.core.data.local.RoomLocalTransaction
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TaskEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.UserEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.inMemoryDatabase
import pucgo.joaopedrogmsilva.brainout.core.data.repository.testSession
import pucgo.joaopedrogmsilva.brainout.core.data.session.AccountAccess
import pucgo.joaopedrogmsilva.brainout.core.data.session.SessionStore
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import java.io.ByteArrayOutputStream
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class BackupRepositoryImplTest {
    @Test
    fun `exportacao exclui dados e credenciais de outras contas`() =
        runTest {
            inMemoryDatabase().use { db ->
                seed(db, "u1", "Joao")
                seed(db, "u2", "joao")
                val repo = repository(db, testSession("u1"))
                val raw = export(repo)
                val envelope = BackupFormat.decodeEnvelope(raw)
                val bytes =
                    BackupCrypto.decrypt(
                        EncryptedPayload(envelope.salt, envelope.iv, envelope.ciphertext),
                        PASSPHRASE,
                    )
                val payload = BackupPayloadCodec.decode(bytes)
                assertThat(envelope.formatVersion).isEqualTo(2)
                assertThat(payload.users.map { it.id }).containsExactly("u1")
                assertThat(payload.projects.map { it.ownerId }).containsExactly("u1")
                assertThat(String(bytes)).doesNotContain("hash-local")
                assertThat(String(bytes)).doesNotContain("joao")
                assertThat(repo.currentTaskIds()).containsExactly("task-u1")
            }
        }

    @Test
    fun `restauracao preserva credenciais sessao e outras contas`() =
        runTest {
            inMemoryDatabase().use { source ->
                inMemoryDatabase().use { target ->
                    seed(source, "origem", "Origem")
                    seed(target, "destino", "Destino")
                    seed(target, "outra", "Outra")
                    val session = testSession("destino")
                    val raw = export(repository(source, testSession("origem")))
                    val result = repository(target, session).restoreFrom(raw.inputStream(), PASSPHRASE)
                    assertThat(target.userDao().findById("destino")?.passwordHash).isEqualTo("hash-local-destino")
                    assertThat(target.userDao().findById("destino")?.username).isEqualTo("Destino")
                    assertThat(session.currentUserId()).isEqualTo("destino")
                    assertThat(target.taskDao().findById("task-outra")).isNotNull()
                    assertThat(target.taskDao().findById("task-destino")).isNull()
                    val task = target.taskDao().findById(result.taskIds.single())!!
                    assertThat(task.id).isNotEqualTo("task-origem")
                    assertThat(target.projectDao().findById(task.projectId)?.ownerId).isEqualTo("destino")
                    assertThat(task.assigneeId).isEqualTo("destino")
                    assertThat(result.reminders).hasSize(1)
                }
            }
        }

    @Test
    fun `v1 permite escolher uma conta sem importar as outras`() =
        runTest {
            inMemoryDatabase().use { db ->
                seed(db, "destino", "Destino")
                val json =
                    JSONObject().put(
                        "users",
                        org.json
                            .JSONArray()
                            .put(JSONObject().put("id", "a").put("name", "Mesmo Nome").put("email", "a@exemplo.com"))
                            .put(JSONObject().put("id", "b").put("name", "Mesmo Nome").put("email", "b@exemplo.com")),
                    )
                for (key in listOf("projects", "tasks", "tags", "projectTags")) json.put(key, org.json.JSONArray())
                json.put("preferences", JSONObject())
                val encrypted = BackupCrypto.encrypt(json.toString().toByteArray(), PASSPHRASE)
                val raw =
                    JSONObject(
                        String(BackupFormat.encodeEnvelope(0L, encrypted)),
                    ).put("formatVersion", 1).toString().toByteArray()
                val repo = repository(db, testSession("destino"))
                assertThat(repo.inspect(raw.inputStream(), PASSPHRASE).map { it.label })
                    .containsExactly("Mesmo Nome (a@exemplo.com)", "Mesmo Nome (b@exemplo.com)")
                assertThat(repo.restoreFrom(raw.inputStream(), PASSPHRASE, "b", "destino").summary.users).isEqualTo(1)
                assertThat(db.backupDao().getAllUsers()).hasSize(1)
            }
        }

    @Test
    fun `arquivo senha selecao e sessao invalidos nao alteram dados`() =
        runTest {
            inMemoryDatabase().use { db ->
                seed(db, "u1", "Joao")
                val session = testSession("u1")
                val repo = repository(db, session)
                val raw = export(repo)
                val missingAccountId =
                    java.util.UUID
                        .randomUUID()
                        .toString()
                assertThat(runCatching { repo.restoreFrom("invalid".byteInputStream(), PASSPHRASE) }.isFailure).isTrue()
                assertThat(
                    runCatching { repo.restoreFrom(raw.inputStream(), "wrong-long-passphrase") }.isFailure,
                ).isTrue()
                assertThat(
                    runCatching {
                        repo.restoreFrom(
                            raw.inputStream(),
                            PASSPHRASE,
                            sourceUserId = missingAccountId,
                        )
                    }.isFailure,
                ).isTrue()
                session.saveUserId("u2")
                assertThat(
                    runCatching { repo.restoreFrom(raw.inputStream(), PASSPHRASE, "u1", "u1") }.isFailure,
                ).isTrue()
                assertThat(db.taskDao().findById("task-u1")).isNotNull()
            }
        }

    private suspend fun export(repo: BackupRepositoryImpl): ByteArray {
        val output = ByteArrayOutputStream()
        repo.exportTo(output, PASSPHRASE)
        return output.toByteArray()
    }

    private fun repository(
        db: BrainOutDatabase,
        session: SessionStore,
    ): BackupRepositoryImpl {
        val preferences = BackupListingPreferences()
        return BackupRepositoryImpl(db, AccountAccess(session, RoomLocalTransaction(db)), preferences)
    }

    private suspend fun seed(
        db: BrainOutDatabase,
        owner: String,
        username: String,
    ) {
        db.userDao().insert(UserEntity.fromDomain(User(owner, username, "hash-local-$owner", Instant.EPOCH)))
        db.projectDao().insert(
            ProjectEntity.fromDomain(Project.create("Projeto $owner", owner).copy(id = "project-$owner")),
        )
        db.taskDao().insert(
            TaskEntity.fromDomain(
                Task
                    .create(
                        "project-$owner",
                        "Tarefa $owner",
                        dueDate = Instant.now().plusSeconds(7200),
                        assigneeId = owner,
                    ).copy(id = "task-$owner"),
            ),
        )
    }

    private companion object {
        const val PASSPHRASE = "fixture-backup-long"
    }
}
