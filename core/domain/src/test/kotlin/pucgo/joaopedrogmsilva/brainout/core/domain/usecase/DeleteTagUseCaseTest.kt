// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TagNotFoundException
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TagRepository

/**
 * Testes do caso de uso [DeleteTagUseCase]: delegação pura de remoção —
 * as associações em `project_tags` caem por `ON DELETE CASCADE` do
 * schema Room, então o caso de uso não lê nem reescreve nada além do
 * próprio `delete`.
 */
class DeleteTagUseCaseTest {
    private val repository: TagRepository = mockk(relaxed = true)
    private val useCase = DeleteTagUseCase(repository)

    @Test
    fun `delete delega a remocao do id informado e nao consulta a tag`() =
        runTest {
            useCase("t1")

            coVerify(exactly = 1) { repository.delete("t1") }
            coVerify(exactly = 0) { repository.findById(any()) }
        }

    @Test
    fun `propaga TagNotFoundException do repositorio ao chamador`() =
        runTest {
            coEvery { repository.delete("t9") } throws TagNotFoundException("t9")

            val erro =
                runCatching {
                    useCase("t9")
                }.exceptionOrNull()

            assertThat(erro).isInstanceOf(TagNotFoundException::class.java)
            assertThat(erro?.message).contains("t9")
        }
}
