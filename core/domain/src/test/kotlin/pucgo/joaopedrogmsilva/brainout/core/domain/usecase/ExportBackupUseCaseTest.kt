// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.FakeBackupRepository
import java.io.ByteArrayOutputStream

/**
 * Testes do [ExportBackupUseCase] — exportação do backup local
 * (issue #214 — LOCAL-03).
 */
class ExportBackupUseCaseTest {
    private val repository = FakeBackupRepository()
    private val useCase = ExportBackupUseCase(repository)

    @Test
    fun `exporta e devolve o resumo do conteudo gravado`() =
        runTest {
            val output = ByteArrayOutputStream()

            val summary = useCase(output, PASSPHRASE)

            assertThat(repository.exportCalled).isTrue()
            assertThat(summary.projects).isEqualTo(2)
            assertThat(summary.tasks).isEqualTo(3)
        }

    @Test
    fun `repassa a senha do backup para o repositorio`() =
        runTest {
            useCase(ByteArrayOutputStream(), PASSPHRASE)

            assertThat(repository.exportedPassphrase).isEqualTo(PASSPHRASE)
        }

    @Test
    fun `rejeita senha curta antes de tocar no repositorio`() =
        runTest {
            val error =
                runCatching {
                    useCase(ByteArrayOutputStream(), "curta")
                }.exceptionOrNull()

            assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(repository.exportCalled).isFalse()
        }

    @Test
    fun `propaga a falha do repositorio sem envolver a excecao`() =
        runTest {
            val failure = IllegalStateException("disco cheio")
            repository.exportError = failure

            val error =
                runCatching {
                    useCase(ByteArrayOutputStream(), PASSPHRASE)
                }.exceptionOrNull()

            assertThat(error).isSameInstanceAs(failure)
        }

    private companion object {
        const val PASSPHRASE: String = "senha-segura"
    }
}
