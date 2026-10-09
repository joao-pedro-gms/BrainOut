// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
@file:Suppress("ConstructorParameterNaming")

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
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.SyncEntityType
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.SyncOpType
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TaskEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TaskSyncPayload
import pucgo.joaopedrogmsilva.brainout.core.data.repository.TaskRepositoryImpl
import java.time.Instant

/**
 * Testes de auditoria da gravação de tarefas (rodada de revisão).
 *
 * NÃO são um contrato desejado: cada teste documenta um comportamento
 * **observado** do código de produção e só fica verde se o bug
 * descrito estiver de fato presente. Serve de rede de segurança para
 * confirmar as conclusões da auditoria e como base de refactor
 * (quando corrigir, o teste deve ser invertido propositalmente).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class TaskSaveAuditTest {
    private lateinit var database: BrainOutDatabase
    private lateinit var projectDao: ProjectDao
    private lateinit var taskDao: TaskDao
    private lateinit var pendingOpDao: PendingOpDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, BrainOutDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        projectDao = database.projectDao()
        taskDao = database.taskDao()
        pendingOpDao = database.pendingOpDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun repository() = TaskRepositoryImpl(taskDao, projectDao, pendingOpDao)

    // ------------------------------------------------------------------
    // HIPÓTESE REFUTADA — `SUM(...)` devolve NULL quando a agregação
    // não percorre nenhuma linha, e a propriedade de destino em
    // [CompletionStatsRow] é `Int` não-nula. A auditoria suspeitou de
    // crash por leitura de NULL; o teste **verificou que não há crash**
    // e que o resultado é o correto (0). O `Cursor.getInt` do SQLite
    // coalesce NULL para 0, então a semântica está correta — só é
    // frágil por depender desse comportamento implícito em vez de um
    // `COALESCE` explícito na query.
    //
    // Mantido como teste de regressão para fechar a hipótese.
    // ------------------------------------------------------------------

    @Test
    fun `REFUTADO 1 observeCompletionStats com owner sem tarefas retorna zeros`() =
        runTest {
            val stats = taskDao.observeCompletionStats("u-sem-tarefas", weekStartMillis = 0L).first()

            assertThat(stats.totalCount).isEqualTo(0)
            assertThat(stats.doneCount).isEqualTo(0)
            assertThat(stats.doneThisWeekCount).isEqualTo(0)
        }

    @Test
    fun `REFUTADO 1b observeCompletionStats com tarefas mas nenhuma concluida`() =
        runTest {
            projectDao.insert(sampleProject())
            taskDao.insert(sampleTask(id = "t-1", status = "TODO"))
            taskDao.insert(sampleTask(id = "t-2", status = "DOING"))

            val stats = taskDao.observeCompletionStats("u-1", weekStartMillis = 0L).first()

            assertThat(stats.totalCount).isEqualTo(2)
            assertThat(stats.doneCount).isEqualTo(0)
            assertThat(stats.doneThisWeekCount).isEqualTo(0)
        }

    /**
     * Complemento do caso acima: quando **existe** ao menos uma tarefa
     * DONE mas nenhuma foi concluída dentro da janela semanal, o
     * `SUM` do segundo agregador não é NULL (a linha existe) e o
     * resultado também é 0. Os dois caminhos convergem para o mesmo
     * valor, o que confirma a hipótese refutada.
     */
    @Test
    fun `REFUTADO 1c doneThisWeekCount zero quando nada foi concluido na semana`() =
        runTest {
            projectDao.insert(sampleProject())
            taskDao.insert(
                sampleTask(
                    id = "t-antiga",
                    status = "DONE",
                    completedAt = Instant.parse("2020-01-01T00:00:00Z"),
                ),
            )

            val stats =
                taskDao
                    .observeCompletionStats(
                        ownerId = "u-1",
                        weekStartMillis = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli(),
                    ).first()

            assertThat(stats.doneCount).isEqualTo(1)
            assertThat(stats.doneThisWeekCount).isEqualTo(0)
        }

    // ------------------------------------------------------------------
    // ACHADO 2 (CORRIGIDO) — @Update do Room é no-op silencioso quando
    // o id não existe. Antes da correção o repositório devolvia a
    // entidade como persistida e ainda enfileirava a op de sync.
    // ------------------------------------------------------------------

    @Test
    fun `ACHADO 2 update de tarefa inexistente lanca e nao enfileira op`() =
        runTest {
            val fantasma =
                pucgo.joaopedrogmsilva.brainout.core.domain.model.Task.create(
                    projectId = "p-inexistente",
                    title = "Tarefa fantasma",
                )

            val thrown = runCatching { repository().update(fantasma) }.exceptionOrNull()

            assertThat(thrown).isInstanceOf(
                pucgo.joaopedrogmsilva.brainout.core.domain.error.TaskNotFoundException::class.java,
            )
            assertThat(taskDao.findById(fantasma.id)).isNull()
            // A fila fica limpa: o backend não cria uma tarefa órfã.
            assertThat(pendingOpDao.count()).isEqualTo(0)
        }

    // ------------------------------------------------------------------
    // ACHADO 3 — payload de sync não transporta prazo nem responsável.
    // ------------------------------------------------------------------

    @Test
    fun `ACHADO 3 payload de sync descarta dueDate e assigneeId`() =
        runTest {
            projectDao.insert(sampleProject())
            val comPrazo =
                pucgo.joaopedrogmsilva.brainout.core.domain.model.Task.create(
                    projectId = "p-1",
                    title = "Tarefa com prazo",
                    dueDate = Instant.parse("2026-12-31T00:00:00Z"),
                    assigneeId = "u-7",
                )
            taskDao.insert(TaskEntity.fromDomain(comPrazo))

            val op =
                PendingOpEntity.enqueue(
                    entityType = SyncEntityType.TASK,
                    entityId = comPrazo.id,
                    opType = SyncOpType.UPDATE,
                    payloadObj =
                        TaskSyncPayload(
                            id = comPrazo.id,
                            projectId = comPrazo.projectId,
                            title = comPrazo.title,
                            priority = comPrazo.priority.priorityCode,
                            done = false,
                        ),
                )

            val campos = op.payload
            assertThat(campos).doesNotContain("due_date")
            assertThat(campos).doesNotContain("assignee_id")
            assertThat(campos).doesNotContain("2026-12-31")
            assertThat(campos).doesNotContain("u-7")
        }

    // ------------------------------------------------------------------
    // ACHADO 4 (CORRIGIDO) — estado DONE legado (v2, completed_at
    // NULL) não era reparado por completeAndCascade, que retornava
    // no-op e deixava o projeto marcado como ativo para sempre.
    // ------------------------------------------------------------------

    @Test
    fun `ACHADO 4 completeAndCascade repara projeto de tarefa DONE legada`() =
        runTest {
            projectDao.insert(sampleProject(isCompleted = false))
            // Tarefa já DONE desde a v2: sem completed_at e o projeto
            // nunca foi marcado como concluído pela cascata (que só
            // passou a existir na v3).
            taskDao.insert(
                sampleTask(id = "t-legada", status = "DONE", completedAt = null),
            )

            repository().completeAndCascade("t-legada")

            // A reidratação marca o projeto: zero tarefas ativas.
            assertThat(projectDao.findById("p-1")?.isCompleted).isTrue()
            assertThat(taskDao.countActiveByProject("p-1")).isEqualTo(0)
            // Reparo puramente local — nada vai para a fila.
            assertThat(pendingOpDao.count()).isEqualTo(0)
        }

    /**
     * O reparo não pode marcar de mais: se ainda restam tarefas ativas
     * no projeto, a flag permanece como estava.
     */
    @Test
    fun `ACHADO 4b reparo nao marca projeto que ainda tem tarefas ativas`() =
        runTest {
            projectDao.insert(sampleProject(isCompleted = false))
            taskDao.insert(sampleTask(id = "t-legada", status = "DONE", completedAt = null))
            taskDao.insert(sampleTask(id = "t-ativa", status = "DOING"))

            repository().completeAndCascade("t-legada")

            assertThat(projectDao.findById("p-1")?.isCompleted).isFalse()
        }

    // ------------------------------------------------------------------
    // ACHADO 5 — fila de sync não coalesce: N edições da mesma tarefa
    // viram N ops completas na fila.
    // ------------------------------------------------------------------

    @Test
    fun `ACHADO 5 edicoes repetidas enfileiram uma op por edicao`() =
        runTest {
            projectDao.insert(sampleProject())
            val base =
                pucgo.joaopedrogmsilva.brainout.core.domain.model.Task.create(
                    projectId = "p-1",
                    title = "Tarefa",
                )
            taskDao.insert(TaskEntity.fromDomain(base))
            val repo = repository()

            repo.update(base.rename("Título 1"))
            repo.update(base.rename("Título 2"))
            repo.update(base.rename("Título 3"))

            val ops = pendingOpDao.nextBatch(limit = 50)
            assertThat(ops).hasSize(3)
            assertThat(ops.map { it.entityId }.distinct()).hasSize(1)
        }

    // ------------------------------------------------------------------
    // ACHADO 6 — updateStatus (usado fora das cascatas) grava status
    // sem tocar completed_at, quebrando o invariante de RN03.
    // ------------------------------------------------------------------

    @Test
    fun `ACHADO 6 updateStatus grava DONE sem completed_at`() =
        runTest {
            projectDao.insert(sampleProject())
            taskDao.insert(sampleTask(id = "t-1", status = "DOING"))

            taskDao.updateStatus("t-1", "DONE")

            val relida = taskDao.findById("t-1")
            assertThat(relida?.status).isEqualTo("DONE")
            assertThat(relida?.completedAt).isNull()
            // Ler de volta para o domínio não quebra, mas a tarefa
            // concluída não tem carimbo — a taxa semanal do dashboard
            // nunca a conta.
        }

    // ------------------------------------------------------------------
    // ACHADO 7 — reabertura ignora o limite RN01 de tarefas ativas.
    // ------------------------------------------------------------------

    @Test
    fun `ACHADO 7 reabertura ultrapassa o limite de tarefas ativas`() =
        runTest {
            projectDao.insert(sampleProject())
            // 50 tarefas ativas + 1 DONE = limite de RN01 já no teto.
            repeat(50) { i -> taskDao.insert(sampleTask(id = "a-$i", status = "DOING")) }
            taskDao.insert(sampleTask(id = "z-done", status = "DONE"))

            assertThat(taskDao.countActiveByProject("p-1")).isEqualTo(50)

            repository().reopenAndCascade("z-done", pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus.DOING)

            // Nenhum caminho revalidou RN01 na reabertura.
            assertThat(taskDao.countActiveByProject("p-1")).isEqualTo(51)
        }

    private fun sampleProject(
        id: String = "p-1",
        ownerId: String = "u-1",
        isCompleted: Boolean = false,
    ): ProjectEntity =
        ProjectEntity(
            id = id,
            ownerId = ownerId,
            name = "Projeto",
            description = null,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            isCompleted = isCompleted,
        )

    private fun sampleTask(
        id: String,
        projectId: String = "p-1",
        status: String = "TODO",
        completedAt: Instant? = null,
    ): TaskEntity =
        TaskEntity(
            id = id,
            projectId = projectId,
            title = "Tarefa $id",
            priorityCode = 1,
            status = status,
            assigneeId = null,
            dueDate = null,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            completedAt = completedAt,
        )
}
