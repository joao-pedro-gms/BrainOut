// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.settings.viewmodel

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.domain.error.BackupFormatException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.BackupIntegrityException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.BackupRestoreException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.BackupTooLargeException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.UnsupportedBackupVersionException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.BackupSummary
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ActiveUserPort
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupAccountOption
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupRestoreResult
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.ExportBackupUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.InspectBackupUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.RestoreBackupUseCase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant

/**
 * Testes do [BackupViewModel] — exportação e restauração do backup
 * local pela tela de Configurações (issue #214 — LOCAL-03).
 *
 * O `ContentResolver` do Robolectric permite registrar os fluxos do
 * `Uri`: assim o teste exercita o caminho completo (abrir o arquivo,
 * escrever, reportar o estado) sem depender do Storage Access
 * Framework.
 *
 * A operação real acontece em `Dispatchers.IO` (por isso a
 * `[kotlinx.coroutines.flow.first]` esperando o estado final, em vez
 * de um simples `advanceUntilIdle`).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class BackupViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeBackupRepository()
    private val exportedContent = ByteArrayOutputStream()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `exportacao bem sucedida publica o resumo e limpa o estado de ocupado`() =
        runTest(dispatcher) {
            val viewModel = newViewModel()

            viewModel.export(EXPORT_URI, PASSPHRASE)
            val state = viewModel.state.first { it.successType != null }

            assertThat(state.isBusy).isFalse()
            assertThat(state.successType).isEqualTo(BackupSuccessType.Exported)
            assertThat(state.errorType).isNull()
            assertThat(state.summary?.projects).isEqualTo(1)
            assertThat(repository.exportedPassphrase).isEqualTo(PASSPHRASE)
        }

    @Test
    fun `arquivo invalido e reportado como erro de formato`() =
        runTest(dispatcher) {
            repository.restoreError = BackupFormatException("formato")
            val viewModel = newViewModel()

            viewModel.restore(EXPORT_URI, PASSPHRASE)
            val state = viewModel.state.first { it.errorType != null }

            assertThat(state.errorType).isEqualTo(BackupErrorType.InvalidFile)
            assertThat(state.successType).isNull()
            assertThat(state.isBusy).isFalse()
        }

    @Test
    fun `versao desconhecida e reportada separadamente`() =
        runTest(dispatcher) {
            repository.restoreError = UnsupportedBackupVersionException(99)
            val viewModel = newViewModel()

            viewModel.restore(EXPORT_URI, PASSPHRASE)
            val state = viewModel.state.first { it.errorType != null }

            assertThat(state.errorType).isEqualTo(BackupErrorType.UnsupportedVersion)
        }

    @Test
    fun `senha errada e reportada como falha de integridade`() =
        runTest(dispatcher) {
            repository.restoreError = BackupIntegrityException()
            val viewModel = newViewModel()

            viewModel.restore(EXPORT_URI, PASSPHRASE)
            val state = viewModel.state.first { it.errorType != null }

            assertThat(state.errorType).isEqualTo(BackupErrorType.Integrity)
        }

    @Test
    fun `arquivo grande demais e reportado como limite excedido`() =
        runTest(dispatcher) {
            repository.restoreError = BackupTooLargeException(1024)
            val viewModel = newViewModel()

            viewModel.restore(EXPORT_URI, PASSPHRASE)
            val state = viewModel.state.first { it.errorType != null }

            assertThat(state.errorType).isEqualTo(BackupErrorType.TooLarge)
        }

    @Test
    fun `falha ao gravar e reportada como falha de restauracao`() =
        runTest(dispatcher) {
            repository.restoreError = BackupRestoreException("grava")
            val viewModel = newViewModel()

            viewModel.restore(EXPORT_URI, PASSPHRASE)
            val state = viewModel.state.first { it.errorType != null }

            assertThat(state.errorType).isEqualTo(BackupErrorType.RestoreFailed)
        }

    @Test
    fun `falha inesperada na exportacao vira erro de exportacao`() =
        runTest(dispatcher) {
            repository.exportError = IllegalStateException("provedor fora do ar")
            val viewModel = newViewModel()

            viewModel.export(EXPORT_URI, PASSPHRASE)
            val state = viewModel.state.first { it.errorType != null }

            assertThat(state.errorType).isEqualTo(BackupErrorType.ExportFailed)
        }

    @Test
    fun `restauracao bem sucedida publica o estado de sucesso`() =
        runTest(dispatcher) {
            val viewModel = newViewModel()

            viewModel.restore(EXPORT_URI, PASSPHRASE)
            val state = viewModel.state.first { it.successType != null }

            assertThat(state.successType).isEqualTo(BackupSuccessType.Restored)
            assertThat(state.errorType).isNull()
            assertThat(state.isBusy).isFalse()
        }

    @Test
    fun `dismissMessage limpa a mensagem exibida`() =
        runTest(dispatcher) {
            val viewModel = newViewModel()
            viewModel.restore(EXPORT_URI, PASSPHRASE)
            viewModel.state.first { it.successType != null }

            viewModel.dismissMessage()
            val state = viewModel.state.first { it.successType == null }

            assertThat(state.errorType).isNull()
            assertThat(state.summary).isNull()
        }

    @Test
    fun `inspecao pede selecao e restaura somente a conta escolhida`() =
        runTest(dispatcher) {
            repository.accounts = listOf(BackupAccountOption("a", "Conta A"), BackupAccountOption("b", "Conta B"))
            val vm = newViewModel()
            vm.inspect(EXPORT_URI, PASSPHRASE)
            val inspected = vm.state.first { it.restoreAccounts.isNotEmpty() }
            assertThat(inspected.restoreAccounts).hasSize(2)
            assertThat(repository.restoredPassphrase).isNull()
            vm.restoreSelected("b")
            vm.state.first { it.successType == BackupSuccessType.Restored }
            assertThat(repository.selectedAccount).isEqualTo("b")
            assertThat(repository.expectedOwner).isEqualTo("u1")
            assertThat(vm.state.value.restoreAccounts).isEmpty()
        }

    @Test
    fun `cancelar selecao limpa a operacao e impede restauracao posterior`() =
        runTest(dispatcher) {
            repository.accounts = listOf(BackupAccountOption("a", "Conta A"))
            val vm = newViewModel()
            vm.inspect(EXPORT_URI, PASSPHRASE)
            vm.state.first { it.restoreAccounts.isNotEmpty() }
            vm.cancelRestore()
            vm.restoreSelected("a")
            assertThat(repository.restoredPassphrase).isNull()
            assertThat(vm.state.value.restoreAccounts).isEmpty()
        }

    private fun newViewModel(): BackupViewModel {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadow = Shadows.shadowOf(context.contentResolver)
        shadow.registerOutputStream(EXPORT_URI, exportedContent)
        shadow.registerInputStream(EXPORT_URI, ByteArrayInputStream(ByteArray(0)))
        return BackupViewModel(
            inspectBackupUseCase =
                InspectBackupUseCase(
                    repository,
                ),
            activeUser = testBackupSession(),
            exportBackupUseCase = ExportBackupUseCase(repository),
            restoreBackupUseCase = RestoreBackupUseCase(repository, NoOpScheduler, testBackupSession()),
            context = context,
        )
    }

    private companion object {
        const val PASSPHRASE: String = "senha-segura"
        val EXPORT_URI: Uri = Uri.parse("content://test/backup.bnbak")
    }
}

/**
 * [BackupRepository] falso usado pelo teste do ViewModel: registra as
 * senhas recebidas e devolve erros programmed.
 */
private class FakeBackupRepository : BackupRepository {
    override suspend fun inspect(
        input: InputStream,
        passphrase: String,
    ): List<BackupAccountOption> = accounts

    var accounts: List<BackupAccountOption> = emptyList()
    var selectedAccount: String? = null
    var expectedOwner: String? = null

    var exportError: Throwable? = null
    var restoreError: Throwable? = null
    var exportedPassphrase: String? = null
    var restoredPassphrase: String? = null

    override suspend fun exportTo(
        output: OutputStream,
        passphrase: String,
    ): BackupSummary {
        exportedPassphrase = passphrase
        exportError?.let { throw it }
        return SUMMARY
    }

    override suspend fun restoreFrom(
        input: InputStream,
        passphrase: String,
        sourceUserId: String?,
        expectedUserId: String?,
    ): BackupRestoreResult {
        restoredPassphrase = passphrase
        selectedAccount = sourceUserId
        expectedOwner = expectedUserId
        restoreError?.let { throw it }
        return BackupRestoreResult(summary = SUMMARY, taskIds = emptySet(), reminders = emptyList())
    }

    override suspend fun currentTaskIds(): Set<String> = emptySet()

    private companion object {
        val SUMMARY: BackupSummary =
            BackupSummary(
                createdAt = Instant.EPOCH,
                users = 1,
                projects = 1,
                tasks = 2,
                tags = 3,
            )
    }
}

/** Agendador de lembretes que não registra nada. */
private object NoOpScheduler :
    DeadlineNotificationScheduler {
    override fun schedule(
        taskId: String,
        triggerAt: Instant,
    ) = Unit

    override fun cancel(taskId: String) = Unit
}

private fun testBackupSession(): ActiveUserPort =
    object : ActiveUserPort {
        override suspend fun currentUser(): User = User.create("Teste", "hash").copy(id = "u1")

        override suspend fun runIfCurrent(
            userId: String,
            action: () -> Unit,
        ): Boolean {
            action()
            return true
        }
    }
