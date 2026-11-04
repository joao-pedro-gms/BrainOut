// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.data.local.LocalTransaction
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.TagDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TagEntity
import pucgo.joaopedrogmsilva.brainout.core.domain.error.DuplicateTagNameException
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TagRepository

/**
 * Testes diretos do [TagRepositoryImpl] (gap Kover 60% do módulo).
 *
 * Cobre o mapeamento Entity ↔ Domain, a escrita transacional e o
 * `observeByProjectIds` introduzido no P2 de tags-por-projeto.
 */
class TagRepositoryImplTest {
    @Test
    fun `findById mapeia entidade para dominio`() =
        runTest {
            val tagDao = FakeTagDao()
            val tag = sampleTag()
            tagDao.tags[tag.id] = TagEntity.fromDomain(tag)
            val repository = newRepository(tagDao)

            assertThat(repository.findById(tag.id)).isEqualTo(tag)
        }

    @Test
    fun `findById retorna null quando ausente`() =
        runTest {
            assertThat(newRepository().findById("ausente")).isNull()
        }

    @Test
    fun `observeForOwner mapeia entidades para dominio`() =
        runTest {
            val tagDao = FakeTagDao()
            val tag = sampleTag()
            tagDao.tags[tag.id] = TagEntity.fromDomain(tag)
            val repository = newRepository(tagDao)

            assertThat(repository.observeForOwner("u1").first()).containsExactly(tag)
        }

    @Test
    fun `observeByProjectIds mapeia projeto para suas tags`() =
        runTest {
            val tagDao = FakeTagDao()
            val t1 = sampleTag(id = "t1", name = "Urgente")
            val t2 = sampleTag(id = "t2", name = "Backlog")
            tagDao.tags[t1.id] = TagEntity.fromDomain(t1)
            tagDao.tags[t2.id] = TagEntity.fromDomain(t2)
            tagDao.associations["p1"] = listOf(t1.id, t2.id)
            tagDao.associations["p2"] = listOf(t1.id)
            val repository = newRepository(tagDao)

            val map = repository.observeByProjectIds(setOf("p1", "p2")).first()

            assertThat(map.keys).containsExactly("p1", "p2")
            assertThat(map["p1"]).containsExactly(t1, t2)
            assertThat(map["p2"]).containsExactly(t1)
        }

    @Test
    fun `observeByProjectIds mantem projeto sem tags com lista vazia`() =
        runTest {
            val tagDao = FakeTagDao()
            tagDao.associations["p1"] = emptyList()
            val repository = newRepository(tagDao)

            val map = repository.observeByProjectIds(setOf("p1")).first()

            assertThat(map["p1"]).isEmpty()
        }

    @Test
    fun `observeByProjectIds com conjunto vazio emite mapa vazio`() =
        runTest {
            val repository = newRepository()

            assertThat(repository.observeByProjectIds(emptySet()).first()).isEmpty()
        }

    @Test
    fun `create persiste tag localmente com id cliente-supplied`() =
        runTest {
            val tagDao = FakeTagDao()
            val localTransaction = FakeLocalTransaction()
            val repository = newRepository(tagDao, localTransaction = localTransaction)
            val tag = sampleTag()

            repository.create(tag)

            assertThat(tagDao.tags[tag.id]?.toDomain()).isEqualTo(tag)
        }

    @Test
    fun `create traduz nome duplicado para DuplicateTagNameException`() =
        runTest {
            val tagDao = FakeTagDao()
            val repository = newRepository(tagDao)
            repository.create(sampleTag(name = "Urgente"))

            // DEF-61 — sem a tradução, a violação do índice UNIQUE
            // subia crua e a tela só conseguia mostrar "não foi
            // possível concluir a operação".
            val failure =
                runCatching {
                    repository.create(sampleTag(id = "t2", name = "Urgente"))
                }.exceptionOrNull()

            assertThat(failure).isInstanceOf(DuplicateTagNameException::class.java)
            assertThat((failure as DuplicateTagNameException).name).isEqualTo("Urgente")
        }

    @Test
    fun `delete remove tag localmente`() =
        runTest {
            val tagDao = FakeTagDao()
            val localTransaction = FakeLocalTransaction()
            val repository = newRepository(tagDao, localTransaction = localTransaction)
            val tag = sampleTag()
            tagDao.tags[tag.id] = TagEntity.fromDomain(tag)

            repository.delete(tag.id)

            assertThat(tagDao.tags).doesNotContainKey(tag.id)
        }

    private fun newRepository(
        tagDao: TagDao = FakeTagDao(),
        localTransaction: LocalTransaction = FakeLocalTransaction(),
    ): TagRepository =
        TagRepositoryImpl(
            tagDao,
            testAccess(localTransaction),
            FakeProjectDao().apply {
                projects["p1"] =
                    ProjectEntity
                        .fromDomain(sampleProject())
                ; projects["p2"] =
                    ProjectEntity
                        .fromDomain(sampleProject(id = "p2"))
            },
        )
}
