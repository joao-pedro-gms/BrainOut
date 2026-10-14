// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TagOwnershipException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository

/**
 * Testes do caso de uso [CreateProjectUseCase].
 *
 * O caso de uso monta a entidade com [Project.create] — daqui vêm as
 * invariantes de `name` e `description` (R3) — e delega a escrita ao
 * [ProjectRepository], que recebe o projeto e a lista de tags a associar
 * (a propriedade das tags é validada dentro do repositório).
 */
class CreateProjectUseCaseTest {
    private val repository: ProjectRepository = mockk()
    private val useCase = CreateProjectUseCase(repository)

    @Test
    fun `cria projeto nao concluido e delega as tags ao repositorio`() =
        runTest {
            val criado = Project.create(name = "Estagio", ownerId = "u1", description = "Descricao")
            coEvery { repository.create(any(), any()) } returns criado

            val result =
                useCase(
                    name = "Estagio",
                    ownerId = "u1",
                    description = "Descricao",
                    tagIds = listOf("t1", "t2"),
                )

            assertThat(result).isEqualTo(criado)
            assertThat(result.isCompleted).isFalse()
            coVerify(exactly = 1) { repository.create(any(), listOf("t1", "t2")) }
        }

    @Test
    fun `sem descricao e sem tags delega projeto cru e lista vazia`() =
        runTest {
            val enviado = slot<Project>()
            coEvery { repository.create(capture(enviado), any()) } answers { enviado.captured }

            val result = useCase(name = "Estagio", ownerId = "u1")

            assertThat(enviado.captured.id).isNotEmpty()
            assertThat(enviado.captured.ownerId).isEqualTo("u1")
            assertThat(enviado.captured.description).isNull()
            assertThat(result).isEqualTo(enviado.captured)
            coVerify { repository.create(any(), emptyList()) }
        }

    @Test
    fun `nome em branco lanca InvalidModelException sem tocar o repositorio`() =
        runTest {
            val erro =
                runCatching {
                    useCase(name = "   ", ownerId = "u1")
                }.exceptionOrNull()

            assertThat(erro).isInstanceOf(InvalidModelException::class.java)
            coVerify(exactly = 0) { repository.create(any(), any()) }
        }

    @Test
    fun `descricao acima do limite lanca InvalidModelException sem tocar o repositorio`() =
        runTest {
            val erro =
                runCatching {
                    useCase(
                        name = "Estagio",
                        ownerId = "u1",
                        description = "x".repeat(Project.MAX_DESCRIPTION_LENGTH + 1),
                    )
                }.exceptionOrNull()

            assertThat(erro).isInstanceOf(InvalidModelException::class.java)
            coVerify(exactly = 0) { repository.create(any(), any()) }
        }

    @Test
    fun `propaga erro do repositorio ao chamador`() =
        runTest {
            coEvery { repository.create(any(), any()) } throws TagOwnershipException("t9")

            val erro =
                runCatching {
                    useCase(name = "Estagio", ownerId = "u1", tagIds = listOf("t9"))
                }.exceptionOrNull()

            assertThat(erro).isInstanceOf(TagOwnershipException::class.java)
            assertThat(erro?.message).contains("t9")
        }
}
