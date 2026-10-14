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
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidStateTransitionException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TagRepository

/**
 * Testes do caso de uso [CreateTagUseCase].
 *
 * A entidade é montada por [Tag.create] — que normaliza o nome e valida
 * nome (1..30 caracteres, sem espaços nas pontas) e cor `#RRGGBB` (R5)
 * — e a persistência é delegada ao [TagRepository]. Como o índice único
 * `(owner_id, name)` é do Room, a colisão chega como erro de banco; aqui
 * fixamos apenas o contrato do domínio: validação antes de escrever e
 * propagação da exceção do repositório sem tradução.
 */
class CreateTagUseCaseTest {
    private val repository: TagRepository = mockk()
    private val useCase = CreateTagUseCase(repository)

    @Test
    fun `cria tag com nome normalizado e delega ao repositorio`() =
        runTest {
            val enviada = slot<Tag>()
            coEvery { repository.create(capture(enviada)) } answers { enviada.captured }

            val result = useCase(ownerId = "u1", name = "  Trabalho  ", color = "#FF8800")

            assertThat(enviada.captured.name).isEqualTo("Trabalho")
            assertThat(enviada.captured.ownerId).isEqualTo("u1")
            assertThat(enviada.captured.color).isEqualTo("#FF8800")
            assertThat(enviada.captured.id).isNotEmpty()
            assertThat(result).isEqualTo(enviada.captured)
        }

    @Test
    fun `nome em branco lanca InvalidModelException sem tocar o repositorio`() =
        runTest {
            val erro =
                runCatching {
                    useCase(ownerId = "u1", name = "   ", color = "#FF8800")
                }.exceptionOrNull()

            assertThat(erro).isInstanceOf(InvalidModelException::class.java)
            coVerify(exactly = 0) { repository.create(any()) }
        }

    @Test
    fun `nome acima do limite de 30 caracteres lanca InvalidModelException`() =
        runTest {
            val erro =
                runCatching {
                    useCase(
                        ownerId = "u1",
                        name = "x".repeat(Tag.MAX_NAME_LENGTH + 1),
                        color = "#FF8800",
                    )
                }.exceptionOrNull()

            assertThat(erro).isInstanceOf(InvalidModelException::class.java)
            coVerify(exactly = 0) { repository.create(any()) }
        }

    @Test
    fun `cor fora do formato hexadecimal lanca InvalidModelException`() =
        runTest {
            val erro =
                runCatching {
                    useCase(ownerId = "u1", name = "Trabalho", color = "laranja")
                }.exceptionOrNull()

            assertThat(erro).isInstanceOf(InvalidModelException::class.java)
            coVerify(exactly = 0) { repository.create(any()) }
        }

    @Test
    fun `propaga erro do repositorio ao chamador`() =
        runTest {
            // Índice único `(owner_id, name)` violado: a exceção de
            // domínio sobe crua, sem conversão no caso de uso.
            coEvery { repository.create(any()) } throws
                InvalidStateTransitionException("Violacao de unicidade (owner_id, name)")

            val erro =
                runCatching {
                    useCase(ownerId = "u1", name = "Trabalho", color = "#FF8800")
                }.exceptionOrNull()

            assertThat(erro).isInstanceOf(InvalidStateTransitionException::class.java)
            assertThat(erro?.message).contains("owner_id, name")
        }
}
