// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.error.ProjectNotFoundException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.UserNotFoundException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ActiveUserPort
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository

/**
 * Testes do caso de uso [UpdateProjectUseCase]: caso de uso fino, que
 * recebe o projeto já modificado pela camada de apresentação (via
 * [Project.copy]) e repassa projeto + lista final de tags para
 * [ProjectRepository.update], que valida a propriedade das tags.
 *
 * Garante que nada se perca no caminho: o estado de conclusão e a lista
 * de tags chegam intactos ao repositório, e a exceção de domínio
 * lançada por ele sobe sem tradução.
 */
class UpdateProjectUseCaseTest {
    private val repository: ProjectRepository = mockk()
    private val activeUser: ActiveUserPort = mockk()
    private val useCase = UpdateProjectUseCase(repository, activeUser)

    private fun projeto() = Project.create(name = "Estagio", ownerId = "u1")

    private fun usuario(): User =
        User.create(
            username = "pessoa",
            passwordHash = "hash",
        )

    @Test
    fun `update delega projeto e lista de tags e devolve o retorno do repositorio`() =
        runTest {
            val alvo = projeto()
            val salvo = alvo.copy(description = "Nova descricao")
            coEvery { activeUser.currentUser() } returns usuario()
            coEvery { repository.update(any(), any()) } returns salvo

            val result = useCase(alvo, listOf("t1"))

            assertThat(result).isEqualTo(salvo)
            coVerify(exactly = 1) { repository.update(alvo, listOf("t1")) }
        }

    @Test
    fun `lista de tags vazia substitui as associacoes anteriores`() =
        runTest {
            val alvo = projeto()
            coEvery { activeUser.currentUser() } returns usuario()
            coEvery { repository.update(any(), any()) } returns alvo

            useCase(alvo, emptyList())

            coVerify(exactly = 1) { repository.update(alvo, emptyList()) }
        }

    @Test
    fun `projeto concluido chega ao repositorio marcado como concluido`() =
        runTest {
            val concluido = projeto().copy(isCompleted = true)
            coEvery { activeUser.currentUser() } returns usuario()
            coEvery { repository.update(any(), any()) } returns concluido

            val result = useCase(concluido, emptyList())

            assertThat(result.isCompleted).isTrue()
            coVerify(exactly = 1) { repository.update(concluido, emptyList()) }
        }

    @Test
    fun `propaga ProjectNotFoundException do repositorio ao chamador`() =
        runTest {
            val alvo = projeto()
            coEvery { activeUser.currentUser() } returns usuario()
            coEvery { repository.update(any(), any()) } throws ProjectNotFoundException(alvo.id)

            val erro =
                runCatching {
                    useCase(alvo, emptyList())
                }.exceptionOrNull()

            assertThat(erro).isInstanceOf(ProjectNotFoundException::class.java)
            assertThat(erro?.message).contains(alvo.id)
        }

    @Test
    fun `sem sessao nao edita o projeto e o repositorio nao e tocado`() =
        runTest {
            val alvo = projeto()
            coEvery { activeUser.currentUser() } returns null

            val erro =
                runCatching {
                    useCase(alvo, emptyList())
                }.exceptionOrNull()

            assertThat(erro).isInstanceOf(UserNotFoundException::class.java)
            coVerify(exactly = 0) { repository.update(any(), any()) }
        }
}
