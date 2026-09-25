// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.error.DuplicateEmailException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.PasswordHasher
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.UserRepository

/**
 * Testes do caso de uso [CreateUserUseCase] com repositório e hasher
 * simulados via mockk.
 */
class CreateUserUseCaseTest {

    private val repository: UserRepository = mockk()
    private val hasher: PasswordHasher = mockk()
    private val useCase = CreateUserUseCase(repository, hasher)

    @Test
    fun `creates user when email is new and data is valid`() = runTest {
        val captured = slot<User>()
        coEvery { repository.findByEmail(any()) } returns null
        coEvery { hasher.hash("plain-text-fixture-input") } returns "hashed::plain-text-fixture-input"
        coEvery { repository.save(capture(captured)) } answers { captured.captured }

        val created = useCase(
            name = "  João Pedro  ",
            email = "  joao@example.com  ",
            rawPassword = "plain-text-fixture-input",
            role = UserRole.OWNER,
        )

        assertThat(created.id).isNotEmpty()
        assertThat(created.email).isEqualTo("joao@example.com")
        assertThat(created.passwordHash).isEqualTo("hashed::plain-text-fixture-input")
        assertThat(created.role).isEqualTo(UserRole.OWNER)
        coVerifyOrder {
            repository.findByEmail("joao@example.com")
            hasher.hash("plain-text-fixture-input")
            repository.save(created)
        }
    }

    @Test
    fun `throws duplicate when email already exists`() = runTest {
        val existing = User.create(
            name = "Outra Pessoa",
            email = "joao@example.com",
            passwordHash = "x",
            role = UserRole.MEMBER,
        )
        coEvery { repository.findByEmail("joao@example.com") } returns existing

        val ex = kotlin.runCatching {
            useCase(
                name = "João",
                email = "joao@example.com",
                rawPassword = "fixture-pwd-long-BBB",
                role = UserRole.MEMBER,
            )
        }.exceptionOrNull()

        assertThat(ex).isInstanceOf(DuplicateEmailException::class.java)
        assertThat((ex as DuplicateEmailException).email).isEqualTo("joao@example.com")
        coVerify(exactly = 0) { hasher.hash(any()) }
        coVerify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `rejects invalid name before touching repository`() = runTest {
        kotlin.runCatching {
            useCase(
                name = "   ",
                email = "joao@example.com",
                rawPassword = "fixture-pwd-long-BBB",
                role = UserRole.MEMBER,
            )
        }.also {
            assertThat(it.exceptionOrNull()).isInstanceOf(InvalidModelException::class.java)
        }
        coVerify(exactly = 0) { repository.findByEmail(any()) }
    }

    @Test
    fun `rejects invalid email before touching repository`() = runTest {
        kotlin.runCatching {
            useCase(
                name = "João",
                email = "not-an-email",
                rawPassword = "fixture-pwd-long-BBB",
                role = UserRole.MEMBER,
            )
        }.also {
            assertThat(it.exceptionOrNull()).isInstanceOf(InvalidModelException::class.java)
        }
        coVerify(exactly = 0) { repository.findByEmail(any()) }
    }

    @Test
    fun `rejects short raw password before touching repository`() = runTest {
        // da senha (MIN_PASSWORD_LENGTH = 8) é autoridade única no
        // domínio — antes era apenas client-side.
        kotlin.runCatching {
            useCase(
                name = "João",
                email = "joao@example.com",
                rawPassword = "shrt",
                role = UserRole.MEMBER,
            )
        }.also {
            assertThat(it.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        }
        coVerify(exactly = 0) { repository.findByEmail(any()) }
    }
}
