// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
@file:Suppress("ConstructorParameterNaming")

package pucgo.joaopedrogmsilva.brainout.core.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectTagCrossRef
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TagEntity
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder
import java.time.Instant

/**
 * Regressão da ordenação configurável da Home (E2.6).
 *
 * Este teste existe porque o bug anterior escapou do
 * `HomeViewModelTest`: lá o repositório é mockado, então a chave de
 * ordenação nunca chegava ao SQLite. Aqui rodamos a query REAL de
 * [ProjectDao.searchProjects] contra um banco Room em memória,
 * alimentando-a com [SortOrder.toStorageKey] — exatamente o que
 * `ProjectRepositoryImpl.observeSearch` faz — e conferimos a ordem
 * devolvida.
 *
 * O bug: `toStorageKey()` emitia `name.lowercase()` (`nameasc`,
 * `namedesc`, `createddesc`, `createdasc`) enquanto o `ORDER BY`
 * compara `name_asc`, `name_desc`, `created_desc` e `created_asc`.
 * Nenhum dos quatro casava, os quatro `CASE WHEN` devolviam `NULL`
 * e o SQLite devolvia as linhas na ordem natural do scan.
 *
 * Os 3 projetos do owner `u-1` têm nomes e `created_at`
 * propositais para que a ordem de inserção (a ordem de scan) seja
 * diferente das quatro ordenações esperadas — assim todo teste
 * falha se a ordenação voltar a não ter efeito.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ProjectDaoSearchSortOrderTest {
    private lateinit var database: BrainOutDatabase
    private lateinit var projectDao: ProjectDao

    private val ownerId = "u-1"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, BrainOutDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        projectDao = database.projectDao()
        runBlocking { seedProjects() }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `name asc retorna os projetos em ordem alfabetica`() =
        runTest {
            assertOrder(SortOrder.NameAsc, listOf("p-1", "p-2", "p-3"))
        }

    @Test
    fun `name desc retorna os projetos em ordem alfabetica inversa`() =
        runTest {
            assertOrder(SortOrder.NameDesc, listOf("p-3", "p-2", "p-1"))
        }

    @Test
    fun `created asc retorna os mais antigos primeiro`() =
        runTest {
            assertOrder(SortOrder.CreatedAsc, listOf("p-2", "p-3", "p-1"))
        }

    @Test
    fun `created desc retorna os mais recentes primeiro`() =
        runTest {
            assertOrder(SortOrder.CreatedDesc, listOf("p-1", "p-3", "p-2"))
        }

    /**
     * Garante que o default de quem nunca tocou no filtro (string
     * vazia) continua significando "mais recentes primeiro", igual
     * ao comportamento histórico de
     * [ProjectDao.observeAllForOwner].
     */
    @Test
    fun `string vazia preserva o comportamento de mais recentes primeiro`() =
        runTest {
            assertOrder(rawSort = "", expectedIds = listOf("p-1", "p-3", "p-2"))
        }

    /**
     * Fecha o contrato de ponta a ponta: cada valor do enum tem uma
     * chave que o `ORDER BY` entende, então trocar a ordenação na
     * UI produz uma lista diferente da anterior. Sem este teste o
     * bug passa despercebido mesmo com testes por ordenação
     * isolados, porque o que quebra é justamente a concordância
     * entre a chave gravada e a string comparada no SQL.
     */
    @Test
    fun `as quatro chaves de SortOrder produzem quatro ordens distintas`() =
        runTest {
            val ordenacoes = SortOrder.entries.associateWith { order -> searchIds(order) }

            assertThat(ordenacoes.values.toSet()).hasSize(SortOrder.entries.size)
            assertThat(ordenacoes.getValue(SortOrder.NameAsc)).isEqualTo(listOf("p-1", "p-2", "p-3"))
            assertThat(ordenacoes.getValue(SortOrder.NameDesc)).isEqualTo(listOf("p-3", "p-2", "p-1"))
            assertThat(ordenacoes.getValue(SortOrder.CreatedAsc)).isEqualTo(listOf("p-2", "p-3", "p-1"))
            assertThat(ordenacoes.getValue(SortOrder.CreatedDesc)).isEqualTo(listOf("p-1", "p-3", "p-2"))
        }

    /**
     * Trava a concordância entre a chave emitida e a comparada no
     * SQL. Uma chave que não casa com nenhum `CASE WHEN` não é erro
     * de SQL: o SQLite ignora a ordenação e devolve a ordem de scan.
     * Essa distinção é exatamente o bug original, então o teste
     * afirma os dois lados do contrato.
     */
    @Test
    fun `toStorageKey emite exatamente as chaves que o ORDER BY compara`() =
        runTest {
            assertThat(SortOrder.entries.map { it.toStorageKey() })
                .containsExactly("name_asc", "name_desc", "created_desc", "created_asc")
                .inOrder()

            assertThat(searchIds(SortOrder.CreatedAsc))
                .isNotEqualTo(searchIds(rawSort = "chave_sem_correspondencia"))
        }

    /**
     * A ordenação compõe com o filtro textual e o filtro de tag: a
     * lista muda de tamanho e a ordem continua válida, provando que
     * o `CASE WHEN` não atropela as demais cláusulas do `WHERE`.
     */
    @Test
    fun `a ordenacao compoe com busca textual e filtro de tag`() =
        runTest {
            projectDao.insertProjectTags(
                listOf(
                    ProjectTagCrossRef(project_id = "p-1", tag_id = "tag-a"),
                    ProjectTagCrossRef(project_id = "p-2", tag_id = "tag-a"),
                ),
            )

            val filtrados =
                projectDao
                    .searchProjects(
                        ownerId = ownerId,
                        query = "",
                        tagId = "tag-a",
                        sort = SortOrder.CreatedAsc.toStorageKey(),
                    ).first()

            assertThat(filtrados.map { it.id }).containsExactly("p-2", "p-1").inOrder()

            val porNome =
                projectDao
                    .searchProjects(
                        ownerId = ownerId,
                        query = "aca",
                        tagId = null,
                        sort = SortOrder.CreatedAsc.toStorageKey(),
                    ).first()

            assertThat(porNome.map { it.id }).containsExactly("p-1")
        }

    /** O filtro por owner continua isolando contas no mesmo banco. */
    @Test
    fun `a busca nao vaza projetos de outro proprietario`() =
        runTest {
            assertOrder(SortOrder.NameAsc, listOf("p-1", "p-2", "p-3"))
        }

    private suspend fun assertOrder(
        sortOrder: SortOrder,
        expectedIds: List<String>,
    ) {
        assertOrder(rawSort = sortOrder.toStorageKey(), expectedIds = expectedIds)
    }

    private suspend fun assertOrder(
        rawSort: String,
        expectedIds: List<String>,
    ) {
        val ids = searchIds(rawSort)
        assertThat(ids).containsExactlyElementsIn(expectedIds).inOrder()
    }

    private suspend fun searchIds(sortOrder: SortOrder): List<String> = searchIds(sortOrder.toStorageKey())

    private suspend fun searchIds(rawSort: String): List<String> =
        projectDao
            .searchProjects(
                ownerId = ownerId,
                query = "",
                tagId = null,
                sort = rawSort,
            ).first()
            .map { it.id }

    /**
     * Fixture: a ordem de inserção é `p-3, p-1, p-2` (Caju,
     * Abacaxi, Banana) — deliberadamente diferente das quatro
     * ordenações esperadas. `p-9` pertence a outro owner e nunca
     * pode aparecer.
     */
    private suspend fun seedProjects() {
        projectDao.insert(
            ProjectEntity(
                id = "p-3",
                ownerId = ownerId,
                name = "Caju",
                description = null,
                createdAt = Instant.parse("2026-02-01T00:00:00Z"),
                isCompleted = false,
            ),
        )
        projectDao.insert(
            ProjectEntity(
                id = "p-1",
                ownerId = ownerId,
                name = "Abacaxi",
                description = null,
                createdAt = Instant.parse("2026-03-01T00:00:00Z"),
                isCompleted = false,
            ),
        )
        projectDao.insert(
            ProjectEntity(
                id = "p-2",
                ownerId = ownerId,
                name = "Banana",
                description = null,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                isCompleted = false,
            ),
        )
        database.tagDao().insert(
            TagEntity(
                id = "tag-a",
                ownerId = ownerId,
                name = "Estudo",
                color = "#FF0000",
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )
        projectDao.insert(
            ProjectEntity(
                id = "p-9",
                ownerId = "u-2",
                name = "Projeto de outro dono",
                description = null,
                createdAt = Instant.parse("2026-04-01T00:00:00Z"),
                isCompleted = false,
            ),
        )
    }
}
