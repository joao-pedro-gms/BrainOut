// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Testes Compose da [BackupSection] (issue #214 — LOCAL-03). Robolectric JVM.
//
// Cobre: as duas linhas de backup, o diálogo de senha na exportação,
// a validação da senha, o diálogo de confirmação de substituição na
// restauração e a exibição da mensagem de erro.

package pucgo.joaopedrogmsilva.brainout.feature.settings.ui

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
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
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.feature.settings.viewmodel.BackupSuccessType
import pucgo.joaopedrogmsilva.brainout.feature.settings.viewmodel.BackupUiState
import pucgo.joaopedrogmsilva.brainout.feature.settings.viewmodel.BackupViewModel
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate

/**
 * Testes da seção de backup local da tela de Configurações
 * (issue #214 — LOCAL-03).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "pt-rBR")
class BackupSectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renderiza as duas linhas de backup`() {
        setContent()

        composeRule.onNodeWithTag(BackupTestTags.EXPORT_ROW).assertIsDisplayed()
        composeRule.onNodeWithTag(BackupTestTags.RESTORE_ROW).assertIsDisplayed()
    }

    @Test
    fun `clicar em exportar abre o dialogo de senha`() {
        setContent()

        composeRule.onNodeWithTag(BackupTestTags.EXPORT_ROW).performClick()

        composeRule.onNodeWithTag(BackupTestTags.PASSPHRASE_FIELD).assertIsDisplayed()
        composeRule.onNodeWithTag(BackupTestTags.PASSPHRASE_CONFIRM_FIELD).assertIsDisplayed()
    }

    @Test
    fun `senha curta e recusada com mensagem de validacao`() {
        setContent()

        composeRule.onNodeWithTag(BackupTestTags.EXPORT_ROW).performClick()
        composeRule.onNodeWithTag(BackupTestTags.PASSPHRASE_FIELD).performTextInput("123")
        composeRule.onNodeWithTag(BackupTestTags.PASSPHRASE_CONFIRM_FIELD).performTextInput("123")
        composeRule.onNodeWithTag(BackupTestTags.CONFIRM).performClick()

        composeRule.onNodeWithTag(BackupTestTags.PASSPHRASE_ERROR).assertIsDisplayed()
    }

    @Test
    fun `senhas diferentes sao recusadas com mensagem de validacao`() {
        setContent()

        composeRule.onNodeWithTag(BackupTestTags.EXPORT_ROW).performClick()
        composeRule.onNodeWithTag(BackupTestTags.PASSPHRASE_FIELD).performTextInput("senha-longa")
        composeRule.onNodeWithTag(BackupTestTags.PASSPHRASE_CONFIRM_FIELD).performTextInput("outra-senha")
        composeRule.onNodeWithTag(BackupTestTags.CONFIRM).performClick()

        composeRule.onNodeWithTag(BackupTestTags.PASSPHRASE_ERROR).assertIsDisplayed()
    }

    @Test
    fun `clicar em restaurar abre a confirmacao de substituicao`() {
        setContent()

        composeRule.onNodeWithTag(BackupTestTags.RESTORE_ROW).performClick()

        composeRule.onNodeWithText("Substituir os dados locais?").assertIsDisplayed()
        composeRule.onNodeWithTag(BackupTestTags.REPLACE_CONFIRM).assertIsDisplayed()
    }

    @Test
    fun `cancelar a confirmacao de substituicao fecha o dialogo`() {
        setContent()

        composeRule.onNodeWithTag(BackupTestTags.RESTORE_ROW).performClick()
        composeRule.onNodeWithTag(BackupTestTags.CANCEL).performClick()

        composeRule.onNodeWithText("Substituir os dados locais?").assertDoesNotExist()
    }

    @Test
    fun `erro do viewmodel e exibido na tela`() {
        val viewModel = newViewModel()
        setContent(viewModel)

        // Um Uri sem provedor faz o ContentResolver falhar; o ViewModel
        // traduz a falha em um erro classificável e a tela o exibe. A
        // operação roda em Dispatchers.IO, então a asserção espera o
        // nó aparecer em vez de exigir um estado síncrono.
        viewModel.restore(Uri.parse("content://inexistente/backup.bnbak"), "senha-segura")
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithTag(BackupTestTags.MESSAGE).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag(BackupTestTags.MESSAGE).assertIsDisplayed()
    }

    @Test
    fun `linhas de backup ficam inativas enquanto a operacao corre`() {
        // Sem esta trava, um segundo toque durante a exportação abriria
        // outro seletor de arquivos por cima do primeiro.
        setBackupContent(BackupUiState(isBusy = true))

        composeRule.onNodeWithTag(BackupTestTags.EXPORT_ROW).assertIsNotEnabled()
        composeRule.onNodeWithTag(BackupTestTags.RESTORE_ROW).assertIsNotEnabled()
    }

    @Test
    fun `linhas de backup ficam ativas quando nao ha operacao em andamento`() {
        setBackupContent(BackupUiState())

        composeRule.onNodeWithTag(BackupTestTags.EXPORT_ROW).assertIsEnabled()
        composeRule.onNodeWithTag(BackupTestTags.RESTORE_ROW).assertIsEnabled()
    }

    @Test
    fun `resumo do conteudo exportado e exibido`() {
        setBackupContent(
            BackupUiState(
                successType = BackupSuccessType.Exported,
                summary = BackupSummary(Instant.EPOCH, 1, 2, 3, 4),
            ),
        )

        composeRule.onNodeWithTag(BackupTestTags.SUMMARY).assertIsDisplayed()
        composeRule.onNodeWithText("2 projetos e 3 tarefas.").assertIsDisplayed()
    }

    @Test
    fun `o nome sugerido do arquivo usa a data em ISO`() {
        // O formato do nome é contrato: `docs/qa/LOCAL-03.md` registra
        // `brainout-backup-2026-10-20.bnbak`, e o roteiro de QA depende
        // dele para reencontrar o arquivo exportado.
        assertThat(defaultBackupFileName(LocalDate.of(2026, 10, 20)))
            .isEqualTo("brainout-backup-2026-10-20.bnbak")
    }

    private fun setContent(viewModel: BackupViewModel = newViewModel()) {
        composeRule.setContent {
            BrainOutTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BackupSection(onRestoreCompleted = {}, viewModel = viewModel)
                }
            }
        }
    }

    /** Renderiza só o cartão de backup, com um estado forjado. */
    private fun setBackupContent(state: BackupUiState) {
        composeRule.setContent {
            BrainOutTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BackupContent(
                        state = state,
                        onExport = {},
                        onRestore = {},
                        onDismissMessage = {},
                    )
                }
            }
        }
    }

    private fun newViewModel(): BackupViewModel {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return BackupViewModel(
            inspectBackupUseCase =
                InspectBackupUseCase(
                    FakeBackupRepository(),
                ),
            activeUser = io.mockk.mockk(relaxed = true),
            exportBackupUseCase = ExportBackupUseCase(FakeBackupRepository()),
            restoreBackupUseCase = RestoreBackupUseCase(FakeBackupRepository(), NoOpScheduler, testBackupSession()),
            context = context,
        )
    }
}

/** [BackupRepository] sem efeito — a UI não escreve em disco no teste. */
private class FakeBackupRepository : BackupRepository {
    override suspend fun inspect(
        input: InputStream,
        passphrase: String,
    ): List<BackupAccountOption> = emptyList()

    override suspend fun exportTo(
        output: OutputStream,
        passphrase: String,
    ): BackupSummary = SUMMARY

    override suspend fun restoreFrom(
        input: InputStream,
        passphrase: String,
        sourceUserId: String?,
        expectedUserId: String?,
    ): BackupRestoreResult = BackupRestoreResult(SUMMARY, emptySet(), emptyList())

    override suspend fun currentTaskIds(): Set<String> = emptySet()

    private companion object {
        val SUMMARY: BackupSummary = BackupSummary(Instant.EPOCH, 1, 1, 1, 1)
    }
}

/** Agendador de lembretes sem efeito. */
private object NoOpScheduler : DeadlineNotificationScheduler {
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
