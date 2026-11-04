// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupAccountOption
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupRepository

class InspectBackupUseCaseTest {
    @Test
    fun `inspecao valida senha e devolve contas sem restaurar`() =
        runTest {
            val repo = mockk<BackupRepository>()
            val useCase = InspectBackupUseCase(repo)
            val input = byteArrayOf().inputStream()
            assertThat(runCatching { useCase(input, "short") }.isFailure).isTrue()
            coVerify(exactly = 0) { repo.inspect(any(), any()) }
            coEvery { repo.inspect(input, "fixture-backup-input") } returns listOf(BackupAccountOption("u1", "Joao"))
            assertThat(useCase(input, "fixture-backup-input").single().id).isEqualTo("u1")
            coVerify(exactly = 0) { repo.restoreFrom(any(), any(), any(), any()) }
        }
}
