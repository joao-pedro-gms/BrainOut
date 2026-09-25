// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidCredentialsException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.PasswordHasher
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.UserRepository

/**
 * Testes do caso de uso [AuthenticateUserUseCase].
 *
 * Garante o comportamento de "falha uniforme" para evitar enumeração
 * de contas: tanto e-mail inexistente quanto senha incorreta lançam a
 * mesma exceção.
 */
class AuthenticateUserUseCaseTest {

    private val repository: UserRepository = mockk()
    private val hasher: PasswordHasher = mockk()
    private val useCase = AuthenticateUserUseCase(repository, hasher)

    @Test
    fun `returns user when email and password match`() = runTest {
        val stored = User.create(
            name = "João",
            email = "joao@example.com",
            passwordHash = "hashed::plain-text-fixture-input",
            role = UserRole.MEMBER,
        )
        coEvery { repository.findByEmail("joao@example.com") } returns stored
        coEvery { hasher.verify("plain-text-fixture-input", "hashed::plain-text-fixture-input") } returns true

        val result = useCase("  joao@example.com  ", "plain-text-fixture-input")

        assertThat(result).isEqualTo(stored)
    }

    @Test
    fun `throws InvalidCredentials when user not found`() = runTest {
        coEvery { repository.findByEmail("ghost@example.com") } returns null

        val ex = kotlin.runCatching {
            useCase("ghost@example.com", "anypassword")
        }.exceptionOrNull()

        assertThat(ex).isInstanceOf(InvalidCredentialsException::class.java)
        coVerify(exactly = 0) { hasher.verify(any(), any()) }
    }

    @Test
    fun `throws InvalidCredentials when password does not match`() = runTest {
        val stored = User.create(
            name = "João",
            email = "joao@example.com",
            passwordHash = "hashed::plain-text-fixture-input",
            role = UserRole.MEMBER,
        )
        coEvery { repository.findByEmail("joao@example.com") } returns stored
        coEvery { hasher.verify("fixture-pwd-wrong-EEE", "hashed::plain-text-fixture-input") } returns false

        val ex = kotlin.runCatching {
            useCase("joao@example.com", "fixture-pwd-wrong-EEE")
        }.exceptionOrNull()

        assertThat(ex).isInstanceOf(InvalidCredentialsException::class.java)
    }

    @Test
    fun `rejects invalid email format before any IO`() = runTest {
        kotlin.runCatching { useCase("not-an-email", "anypassword") }.also {
            assertThat(it.exceptionOrNull()).isInstanceOf(InvalidModelException::class.java)
        }
        coVerify(exactly = 0) { repository.findByEmail(any()) }
    }

    @Test
    fun `rejects short password before any IO`() = runTest {
        kotlin.runCatching { useCase("joao@example.com", "short") }.also {
            assertThat(it.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        }
        coVerify(exactly = 0) { repository.findByEmail(any()) }
    }
}
