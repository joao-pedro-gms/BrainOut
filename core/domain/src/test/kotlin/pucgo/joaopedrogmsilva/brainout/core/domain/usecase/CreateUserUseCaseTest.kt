// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.error.DuplicateUsernameException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.PasswordHasher
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.UserRepository

class CreateUserUseCaseTest {
    private val repository = mockk<UserRepository>()
    private val hasher = mockk<PasswordHasher>()
    private val useCase = CreateUserUseCase(repository, hasher)

    @Test
    fun `cadastro usa nome aparado e persiste somente o hash`() =
        runTest {
            coEvery { repository.findByUsername("Joao") } returns null
            coEvery { hasher.hash("fixture-input-long") } returns "hash"
            coEvery { repository.save(any()) } answers { firstArg() }
            val user = useCase(" Joao ", "fixture-input-long")
            assertThat(user.username).isEqualTo("Joao")
            assertThat(user.passwordHash).isEqualTo("hash")
            coVerify(exactly = 1) { repository.save(user) }
        }

    @Test
    fun `nome ocupado falha antes de derivar senha`() =
        runTest {
            coEvery { repository.findByUsername("Joao") } returns User.create("Joao", "hash")
            assertThat(
                runCatching {
                    useCase("Joao", "fixture-input-long")
                }.exceptionOrNull(),
            ).isInstanceOf(DuplicateUsernameException::class.java)
            coVerify(exactly = 0) { hasher.hash(any()) }
        }

    @Test
    fun `nome invalido ou senha curta falham antes de acessar dados`() =
        runTest {
            assertThat(runCatching { useCase("a-b", "fixture-input-long") }.isFailure).isTrue()
            assertThat(runCatching { useCase("Joao", "short") }.isFailure).isTrue()
            coVerify(exactly = 0) { repository.findByUsername(any()) }
        }
}
