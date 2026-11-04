// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.data.local.LocalTransaction
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.ProjectDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.TagDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TagEntity
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TagNotFoundException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TagOwnershipException
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder

/**
 * Testes diretos do [ProjectRepositoryImpl].
 *
 * Cobre a gravação transacional (a entidade e as associações de tags
 * entram juntas ou não entram), a validação de propriedade das tags
 * via `validateTagOwnership` (RN05) e os fluxos de observação reativa.
 *
 * A escrita dupla (Room + `pending_ops`) deixou de existir com a
 * remoção da camada de rede (#212) — o que these testes verificam hoje é
 * a **atomicidade** da gravação local.
 */
class ProjectRepositoryImplTest {
    private val ownerId: String = "u1"

    @Test
    fun `findById mapeia entidade para dominio`() =
        runTest {
            val projectDao = FakeProjectDao()
            val project = sampleProject()
            projectDao.projects[project.id] = ProjectEntity.fromDomain(project)
            val repository = newRepository(projectDao)

            assertThat(repository.findById(project.id)).isEqualTo(project)
        }

    @Test
    fun `findById retorna null quando ausente`() =
        runTest {
            assertThat(newRepository().findById("ausente")).isNull()
        }

    @Test
    fun `observeAllForOwner mapeia entidades para dominio`() =
        runTest {
            val projectDao = FakeProjectDao()
            val p1 = sampleProject(id = "p1", name = "P1")
            val p2 = sampleProject(id = "p2", name = "P2")
            projectDao.projects[p1.id] = ProjectEntity.fromDomain(p1)
            projectDao.projects[p2.id] = ProjectEntity.fromDomain(p2)
            val repository = newRepository(projectDao)

            val result = repository.observeAllForOwner(ownerId).first()

            assertThat(result).containsExactly(p1, p2)
        }

    @Test
    fun `create persiste projeto localmente`() =
        runTest {
            val projectDao = FakeProjectDao()
            val localTransaction = FakeLocalTransaction()
            val repository = newRepository(projectDao, localTransaction)
            val project = sampleProject()

            repository.create(project, emptyList())

            assertThat(projectDao.projects[project.id]?.toDomain()).isEqualTo(project)
        }

    @Test
    fun `create rejeita tag inexistente antes de persistir projeto`() =
        runTest {
            val projectDao = FakeProjectDao()
            val tagDao = FakeTagDao()
            val localTransaction = FakeLocalTransaction()
            val repository = newRepository(projectDao, localTransaction, tagDao)
            val project = sampleProject()

            val thrown =
                runCatching { repository.create(project, listOf("tag-ausente")) }
                    .exceptionOrNull()

            assertThat(thrown).isInstanceOf(TagNotFoundException::class.java)
            // A validação de tag lança **dentro** da transação, então a
            // inserção do projeto é desfeita: nada fica gravado.
        }

    @Test
    fun `create rejeita tag de outro owner antes de persistir`() =
        runTest {
            val projectDao = FakeProjectDao()
            val tagDao = FakeTagDao()
            val localTransaction = FakeLocalTransaction()
            val foreignTag = sampleTag(id = "t1", ownerId = "outro-owner")
            tagDao.tags[foreignTag.id] = TagEntity.fromDomain(foreignTag)
            val repository = newRepository(projectDao, localTransaction, tagDao)
            val project = sampleProject()

            val thrown =
                runCatching { repository.create(project, listOf(foreignTag.id)) }
                    .exceptionOrNull()

            assertThat(thrown).isInstanceOf(TagOwnershipException::class.java)
        }

    @Test
    fun `create substitui associacoes de tags atomicamente`() =
        runTest {
            val projectDao = FakeProjectDao()
            val tagDao = FakeTagDao()
            val localTransaction = FakeLocalTransaction()
            val repository = newRepository(projectDao, localTransaction, tagDao)
            val project = sampleProject()
            val tagA = sampleTag(id = "ta", ownerId = ownerId)
            val tagB = sampleTag(id = "tb", ownerId = ownerId)
            tagDao.tags[tagA.id] = TagEntity.fromDomain(tagA)
            tagDao.tags[tagB.id] = TagEntity.fromDomain(tagB)

            repository.create(project, listOf(tagA.id, tagB.id))

            assertThat(projectDao.associations[project.id])
                .containsExactly(tagA.id, tagB.id)
                .inOrder()
        }

    @Test
    fun `update persiste projeto localmente`() =
        runTest {
            val projectDao = FakeProjectDao()
            val localTransaction = FakeLocalTransaction()
            val repository = newRepository(projectDao, localTransaction)
            val project = sampleProject()
            projectDao.projects[project.id] = ProjectEntity.fromDomain(project)

            repository.update(project, emptyList())
        }

    @Test
    fun `update rejeita tag de outro owner antes de persistir`() =
        runTest {
            val projectDao = FakeProjectDao()
            val tagDao = FakeTagDao()
            val foreignTag = sampleTag(id = "t1", ownerId = "outro-owner")
            tagDao.tags[foreignTag.id] = TagEntity.fromDomain(foreignTag)
            val repository = newRepository(projectDao, tagDao = tagDao)
            val project = sampleProject()
            projectDao.projects[project.id] = ProjectEntity.fromDomain(project)

            val thrown =
                runCatching { repository.update(project, listOf(foreignTag.id)) }
                    .exceptionOrNull()

            assertThat(thrown).isInstanceOf(TagOwnershipException::class.java)
        }

    @Test
    fun `delete remove projeto localmente`() =
        runTest {
            val projectDao = FakeProjectDao()
            val localTransaction = FakeLocalTransaction()
            val repository = newRepository(projectDao, localTransaction)
            val project = sampleProject()
            projectDao.projects[project.id] = ProjectEntity.fromDomain(project)

            repository.delete(project.id)

            assertThat(projectDao.projects).doesNotContainKey(project.id)
        }

    @Test
    fun `observeSearch delega query ao DAO com SortOrder toStorageKey`() =
        runTest {
            val projectDao = FakeProjectDao()
            val repository = newRepository(projectDao)

            repository.observeSearch(ownerId, "query", "tag-1", SortOrder.NameAsc).first()

            assertThat(projectDao.lastSearch).isNotNull()
            val search = projectDao.lastSearch!!
            assertThat(search[0]).isEqualTo(ownerId)
            assertThat(search[1]).isEqualTo("query")
            assertThat(search[2]).isEqualTo("tag-1")
            // Grafia canônica `snake_case`: é exatamente a string que o
            // `CASE WHEN` de `ProjectDao.searchProjects` compara. A grafia
            // concatenada antiga ("nameasc") nunca casava com a SQL e fazia a
            // ordenação configurada ser um no-op — a asserção existe para
            // impedir a regressão. O comportamento do SQLite com a string
            // real está em `ProjectDaoSearchSortOrderTest`.
            assertThat(search[3]).isEqualTo("name_asc")
        }

    private fun newRepository(
        projectDao: ProjectDao = FakeProjectDao(),
        localTransaction: LocalTransaction = FakeLocalTransaction(),
        tagDao: TagDao = FakeTagDao(),
    ): ProjectRepository = ProjectRepositoryImpl(projectDao, tagDao, testAccess(localTransaction))

    /** Fake mínimo de [TagDao]. */
}
