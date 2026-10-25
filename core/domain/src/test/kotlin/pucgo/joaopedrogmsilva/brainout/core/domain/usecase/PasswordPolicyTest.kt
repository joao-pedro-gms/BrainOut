// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.FakeBackupRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.PasswordHasher
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.UserRepository
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Testes da política de senha do domínio.
 *
 * O **número** ([MIN_PASSWORD_LENGTH]) já tinha uma fonte só, mas a
 * guarda era reescrita em cada caso de uso — cadastro, login,
 * exportação e restauração. Este teste existe para que a regra não
 * volte a divergir: os quatro precisam recusar a mesma senha curta,
 * aceitar a mesma senha no limite e dizer a **mesma** coisa quando
 * recusam.
 *
 * As comparações são **entre os casos de uso**, não contra a própria
 * constante: comparar a constante com ela mesma não prova nada. E o
 * texto da mensagem é fixado por literal, para que a regra também
 * tenha um contrato escrito.
 */
class PasswordPolicyTest {
    private val createRepository: UserRepository = mockk()
    private val authRepository: UserRepository = mockk()
    private val hasher: PasswordHasher = mockk()
    private val backupRepository = FakeBackupRepository()
    private val scheduler: DeadlineNotificationScheduler = mockk(relaxed = true)

    private val createUser = CreateUserUseCase(createRepository, hasher)
    private val authenticate = AuthenticateUserUseCase(authRepository, hasher)
    private val exportBackup = ExportBackupUseCase(backupRepository)
    private val restoreBackup = RestoreBackupUseCase(backupRepository, scheduler)

    @Test
    fun `a senha abaixo do limite e recusada pelos quatro casos de uso`() =
        runTest {
            val curta = "a".repeat(MIN_PASSWORD_LENGTH - 1)

            val erros =
                listOf(
                    erroDe { createUser(NAME, EMAIL, curta, UserRole.OWNER) },
                    erroDe { authenticate(EMAIL, curta) },
                    erroDe { exportBackup(ByteArrayOutputStream(), curta) },
                    erroDe { restoreBackup(ByteArrayInputStream(ByteArray(0)), curta) },
                )

            assertThat(erros.map { it?.javaClass }).containsExactly(
                IllegalArgumentException::class.java,
                IllegalArgumentException::class.java,
                IllegalArgumentException::class.java,
                IllegalArgumentException::class.java,
            )
        }

    @Test
    fun `a senha no limite e aceita pelos quatro casos de uso`() =
        runTest {
            val noLimite = "a".repeat(MIN_PASSWORD_LENGTH)
            val stored =
                User.create(
                    name = NAME,
                    email = EMAIL,
                    passwordHash = "hashed::fixture",
                    role = UserRole.OWNER,
                )
            coEvery { createRepository.findByEmail(EMAIL) } returns null
            coEvery { createRepository.save(any()) } answers { firstArg() }
            coEvery { hasher.hash(any()) } returns "hashed::fixture"
            coEvery { authRepository.findByEmail(EMAIL) } returns stored
            coEvery { hasher.verify(any(), any()) } returns true

            val erros =
                listOf(
                    erroDe { createUser(NAME, EMAIL, noLimite, UserRole.OWNER) },
                    erroDe { authenticate(EMAIL, noLimite) },
                    erroDe { exportBackup(ByteArrayOutputStream(), noLimite) },
                    erroDe { restoreBackup(ByteArrayInputStream(ByteArray(0)), noLimite) },
                )

            assertThat(erros.filterNotNull()).isEmpty()
        }

    @Test
    fun `a mensagem de senha curta e a mesma em cada par de casos de uso`() =
        runTest {
            val curta = "a".repeat(MIN_PASSWORD_LENGTH - 1)

            val contaCadastro = erroDe { createUser(NAME, EMAIL, curta, UserRole.OWNER) }?.message
            val contaLogin = erroDe { authenticate(EMAIL, curta) }?.message
            val backupExport = erroDe { exportBackup(ByteArrayOutputStream(), curta) }?.message
            val backupRestaura = erroDe { restoreBackup(ByteArrayInputStream(ByteArray(0)), curta) }?.message

            assertThat(contaCadastro).isEqualTo("Senha deve ter pelo menos 8 caracteres")
            assertThat(contaLogin).isEqualTo(contaCadastro)
            assertThat(backupExport).isEqualTo("A senha do backup deve ter pelo menos 8 caracteres")
            assertThat(backupRestaura).isEqualTo(backupExport)
        }

    /** Executa a operação e devolve a exceção que ela lançou, se houver. */
    private suspend fun erroDe(operacao: suspend () -> Unit): Throwable? = runCatching { operacao() }.exceptionOrNull()

    private companion object {
        const val NAME: String = "João Pedro"
        const val EMAIL: String = "joao@example.com"
    }
}
