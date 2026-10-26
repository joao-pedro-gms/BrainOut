// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Testes do caso de uso [DeleteTaskUseCase].
//
// A parte que importa é a **permissão**: a matriz `UserRole`/`Permission`
// dá a `Member` apenas `CREATE_TASK` e `EDIT_TASK` — excluir é do
// `Owner`. O caso de uso tem de recusar a remoção **antes** de tocar
// o repositório (DEF-36).

package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.error.UserNotFoundException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ActiveUserPort
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository

class DeleteTaskUseCaseTest {
    private val repository: TaskRepository = mockk()
    private val deadlineScheduler: DeadlineNotificationScheduler = mockk(relaxed = true)
    private val activeUser: ActiveUserPort = mockk()

    private fun useCase() = DeleteTaskUseCase(repository, deadlineScheduler, activeUser)

    private fun user(): User =
        User.create(
            username = "pessoa",
            passwordHash = "hash",
        )

    @Test
    fun `owner apaga a tarefa`() =
        runTest {
            coEvery { activeUser.currentUser() } returns user()
            coEvery { repository.delete("t1") } returns Unit

            useCase()("t1")

            coVerify { repository.delete("t1") }
            coVerify { deadlineScheduler.cancel("t1") }
        }

    @Test
    fun `sem sessao nao apaga a tarefa e o repositorio nao e tocado`() =
        runTest {
            coEvery { activeUser.currentUser() } returns null

            val failure =
                runCatching {
                    useCase()("t1")
                }.exceptionOrNull()

            assertThat(failure).isInstanceOf(UserNotFoundException::class.java)
            coVerify(exactly = 0) { repository.delete(any()) }
            coVerify(exactly = 0) { deadlineScheduler.cancel(any()) }
        }

    @Test
    fun `sem sessao a remocao e recusada`() =
        runTest {
            coEvery { activeUser.currentUser() } returns null

            val failure =
                runCatching {
                    useCase()("t1")
                }.exceptionOrNull()

            assertThat(failure).isInstanceOf(UserNotFoundException::class.java)
            coVerify(exactly = 0) { repository.delete(any()) }
        }
}
