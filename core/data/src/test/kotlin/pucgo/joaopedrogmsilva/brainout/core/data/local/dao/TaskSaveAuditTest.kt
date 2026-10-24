// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
@file:Suppress("ConstructorParameterNaming")

package pucgo.joaopedrogmsilva.brainout.core.data.local.dao

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
import pucgo.joaopedrogmsilva.brainout.core.data.local.RoomLocalTransaction
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TaskEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.inMemoryDatabase
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

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        projectDao = database.projectDao()
        taskDao = database.taskDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun repository() = TaskRepositoryImpl(taskDao, projectDao, RoomLocalTransaction(database))

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

    @Test
    fun `ACHADO 2 update de tarefa inexistente lanca sem criar registro`() =
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
        }

    @Test
    fun `prazo e responsavel permanecem na persistencia local`() =
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

            repository().update(comPrazo)

            val saved = taskDao.findById(comPrazo.id)!!
            assertThat(saved.dueDate).isEqualTo(comPrazo.dueDate)
            assertThat(saved.assigneeId).isEqualTo("u-7")
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

    @Test
    fun `edicoes locais repetidas preservam o estado final`() =
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

            assertThat(taskDao.findById(base.id)?.title).isEqualTo("Título 3")
        }

    // ------------------------------------------------------------------
    // ACHADO 6 (CORRIGIDO — DEF-19) — existia um `TaskDao.updateStatus`
    // que gravava só a coluna `status`, deixando DONE sem carimbo
    // temporal. O método foi **removido**: sobrou só
    // `updateStatusAndCompletedAt`, o caminho que grava o par
    // `status` + `completed_at` numa única escrita. O teste que
    // provava o defeito foi invertido para travar o **invariante**
    // (não o método): qualquer conclusão de tarefa, venha de onde
    // vier, tem de deixar `status = DONE` E `completed_at` não-nulo.
    // ------------------------------------------------------------------

    @Test
    fun `ACHADO 6 concluindo tarefa grava status E completed_at na mesma escrita`() =
        runTest {
            projectDao.insert(sampleProject())
            taskDao.insert(sampleTask(id = "t-1", status = "DOING"))

            repository().completeAndCascade("t-1")

            val relida = taskDao.findById("t-1")
            assertThat(relida?.status).isEqualTo("DONE")
            // O carimbo temporal é o que DEF-19 apagava: sem ele a
            // tarefa nunca conta em `doneThisWeekCount` e a taxa de
            // conclusão semanal do Dashboard fica distorcida.
            assertThat(relida?.completedAt).isNotNull()

            // Efeito observável do defeito, agora fechado: a tarefa
            // concluída conta na janela semanal.
            val stats = taskDao.observeCompletionStats("u-1", weekStartMillis = 0L).first()
            assertThat(stats.doneCount).isEqualTo(1)
            assertThat(stats.doneThisWeekCount).isEqualTo(1)
        }

    /**
     * O mesmo invariante pela reabertura: sair de DONE limpa o
     * carimbo. Uma escrita de status que não limpe `completed_at`
     * deixaria a tarefa ativa com data de conclusão.
     */
    @Test
    fun `ACHADO 6b reabrindo tarefa limpa o completed_at junto com o status`() =
        runTest {
            projectDao.insert(sampleProject(isCompleted = true))
            taskDao.insert(
                sampleTask(
                    id = "t-1",
                    status = "DONE",
                    completedAt = Instant.parse("2026-03-02T10:15:00Z"),
                ),
            )

            repository().reopenAndCascade(
                "t-1",
                pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus.DOING,
            )

            val relida = taskDao.findById("t-1")
            assertThat(relida?.status).isEqualTo("DOING")
            assertThat(relida?.completedAt).isNull()
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
}
