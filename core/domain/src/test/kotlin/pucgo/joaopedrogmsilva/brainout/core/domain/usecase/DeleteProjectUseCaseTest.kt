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
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ActiveUserPort
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository

/**
 * Testes do caso de uso [DeleteProjectUseCase]: a remoção é uma
 * delegação pura — tarefas e `project_tags` caem por `ON DELETE
 * CASCADE` do schema Room, então o caso de uso não lê nem reescreve
 * nada além do próprio `delete`.
 *
 * A exclusão exige `DELETE_PROJECT`: só o dono apaga o projeto, porque
 * o efeito é em cascata sobre todas as tarefas (DEF-36).
 */
class DeleteProjectUseCaseTest {
    private val repository: ProjectRepository = mockk(relaxed = true)
    private val activeUser: ActiveUserPort = mockk()
    private val useCase = DeleteProjectUseCase(repository, activeUser)

    private fun usuario(): User =
        User.create(
            username = "pessoa",
            passwordHash = "hash",
        )

    @Test
    fun `delete delega a remocao do id informado e nao consulta o projeto`() =
        runTest {
            coEvery { activeUser.currentUser() } returns usuario()

            useCase("p1")

            coVerify(exactly = 1) { repository.delete("p1") }
            coVerify(exactly = 0) { repository.findById(any()) }
        }

    @Test
    fun `propaga ProjectNotFoundException do repositorio ao chamador`() =
        runTest {
            coEvery { activeUser.currentUser() } returns usuario()
            coEvery { repository.delete("p9") } throws ProjectNotFoundException("p9")

            val erro =
                runCatching {
                    useCase("p9")
                }.exceptionOrNull()

            assertThat(erro).isInstanceOf(ProjectNotFoundException::class.java)
            assertThat(erro?.message).contains("p9")
        }

    @Test
    fun `sem sessao nao apaga o projeto e o repositorio nao e tocado`() =
        runTest {
            coEvery { activeUser.currentUser() } returns null

            val erro =
                runCatching {
                    useCase("p1")
                }.exceptionOrNull()

            assertThat(erro).isInstanceOf(UserNotFoundException::class.java)
            coVerify(exactly = 0) { repository.delete(any()) }
        }
}
