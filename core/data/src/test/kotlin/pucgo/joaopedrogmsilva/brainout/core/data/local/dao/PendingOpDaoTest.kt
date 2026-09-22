// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.PendingOpEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectSyncPayload
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.SyncEntityType
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.SyncOpType

/**
 * Testes do [PendingOpDao] rodando em Robolectric com banco em
 * memória (E3.3).
 *
 * Cobre os caminhos críticos da fila offline:
 * - Enfileiramento preserva a ordem de inserção (nextBatch).
 * - `enqueueInTx` grava a escrita e a op atomicamente — falha na
 *   escrita reverte a op.
 * - `markAttempt` incrementa attempts na mesma transação.
 * - `observeCount` emite a contagem reativa.
 * - `deleteForEntity` remove só as ops da entidade informada.
 * - Payloads round-trip (encode → decode) com os campos snake_case.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class PendingOpDaoTest {

    private lateinit var database: BrainOutDatabase
    private lateinit var dao: PendingOpDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, BrainOutDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.pendingOpDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `nextBatch retorna ops em ordem de enfileiramento`() = runTest {
        dao.insert(op(entityId = "p-1"))
        dao.insert(op(entityId = "p-2"))
        dao.insert(op(entityId = "p-3"))

        val batch = dao.nextBatch(10)

        assertThat(batch.map { it.entityId }).containsExactly("p-1", "p-2", "p-3").inOrder()
    }

    @Test
    fun `nextBatch respeita o limite informado`() = runTest {
        repeat(5) { index -> dao.insert(op(entityId = "p-$index")) }

        assertThat(dao.nextBatch(3)).hasSize(3)
        assertThat(dao.nextBatch(3).map { it.id }).isEqualTo(
            dao.nextBatch(10).take(3).map { it.id },
        )
    }

    @Test
    fun `enqueueInTx grava op e escrita na mesma transacao`() = runTest {
        val writes = mutableListOf<String>()
        dao.enqueueInTx(op(entityId = "p-1", payloadObj = sampleProjectPayload())) {
            writes.add("project-inserted")
        }

        assertThat(writes).containsExactly("project-inserted")
        assertThat(dao.count()).isEqualTo(1)
    }

    @Test
    fun `enqueueInTx reverte op quando a escrita falha`() = runTest {
        val resultado = runCatching {
            dao.enqueueInTx(op(entityId = "p-1")) {
                error("escrita falhou deliberadamente")
            }
        }

        assertThat(resultado.isFailure).isTrue()
        // A op NÃO deve ter sido gravada — rollback da transação.
        assertThat(dao.count()).isEqualTo(0)
    }

    @Test
    fun `markAttempt incrementa attempts sem recriar a op`() = runTest {
        val id = dao.insert(op(entityId = "p-1"))

        dao.markAttempt(id)
        dao.markAttempt(id)

        val op = dao.findById(id)!!
        assertThat(op.attempts).isEqualTo(2)
        assertThat(op.entityId).isEqualTo("p-1")
    }

    @Test
    fun `markAttempt em op inexistente é no-op`() = runTest {
        dao.markAttempt(999L)
        assertThat(dao.count()).isEqualTo(0)
    }

    @Test
    fun `observeCount emite contagem reativa`() = runTest {
        assertThat(dao.observeCount().first()).isEqualTo(0)

        dao.insert(op(entityId = "p-1"))
        dao.insert(op(entityId = "p-2"))

        assertThat(dao.observeCount().first()).isEqualTo(2)

        dao.deleteById(dao.nextBatch(1).first().id)
        assertThat(dao.observeCount().first()).isEqualTo(1)
    }

    @Test
    fun `deleteForEntity remove apenas as ops da entidade`() = runTest {
        dao.insert(op(entityId = "p-1", entityType = SyncEntityType.PROJECT))
        dao.insert(op(entityId = "p-1", entityType = SyncEntityType.TASK))
        dao.insert(op(entityId = "p-2", entityType = SyncEntityType.PROJECT))

        dao.deleteForEntity(SyncEntityType.PROJECT.name, "p-1")

        val restantes = dao.nextBatch(10)
        assertThat(restantes).hasSize(2)
        assertThat(restantes.map { it.entityType }).containsExactly("PROJECT", "TASK")
    }

    @Test
    fun `payload round trip mantem campos snake_case`() = runTest {
        val payload = sampleProjectPayload()
        val id = dao.insert(
            op(entityId = payload.id, payloadObj = payload),
        )

        val stored = dao.findById(id)!!
        val decoded = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
        }.decodeFromString<ProjectSyncPayload>(stored.payload)

        assertThat(decoded).isEqualTo(payload)
    }

    @Test
    fun `op DELETE tem payload vazio`() = runTest {
        val deleteOp = PendingOpEntity.enqueue(
            entityType = SyncEntityType.PROJECT,
            entityId = "p-1",
            opType = SyncOpType.DELETE,
        )
        val id = dao.insert(deleteOp)

        assertThat(dao.findById(id)!!.payload).isEqualTo("{}")
    }

    private fun op(
        entityId: String,
        entityType: SyncEntityType = SyncEntityType.PROJECT,
        opType: SyncOpType = SyncOpType.CREATE,
        payloadObj: Any? = null,
    ): PendingOpEntity = PendingOpEntity.enqueue(
        entityType = entityType,
        entityId = entityId,
        opType = opType,
        payloadObj = payloadObj,
    )

    private fun sampleProjectPayload() = ProjectSyncPayload(
        id = "11111111-1111-4111-8111-111111111111",
        name = "Projeto PUC",
        description = "Trabalho de ADS",
    )
}
