// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.settings.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pucgo.joaopedrogmsilva.brainout.core.domain.error.BackupException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.BackupFormatException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.BackupIntegrityException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.BackupRestoreException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.BackupTooLargeException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.UnsupportedBackupVersionException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.BackupSummary
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.ExportBackupUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.RestoreBackupUseCase
import javax.inject.Inject

private const val TAG = "BrainOut:BackupVM"

/**
 * ViewModel da seção de backup local (issue #214 — LOCAL-03).
 *
 * Ele recebe o [Uri] escolhido pelo usuário no Storage Access
 * Framework e é responsável por abrir o fluxo, executar o caso de uso
 * e classificar o resultado — a tela cuida apenas dos diálogos e dos
 * launchers.
 *
 * **Threads:** abrir e ler/gravar o arquivo é I/O bloqueante; por
 * isso as chamadas ao caso de uso rodam em [Dispatchers.IO]. A camada
 * de dados não troca o dispatcher — ela apenas executa o que é
 * pedido.
 */
@HiltViewModel
class BackupViewModel
    @Inject
    constructor(
        private val exportBackupUseCase: ExportBackupUseCase,
        private val restoreBackupUseCase: RestoreBackupUseCase,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _state: MutableStateFlow<BackupUiState> = MutableStateFlow(BackupUiState())
        val state: StateFlow<BackupUiState> = _state.asStateFlow()

        /**
         * Exporta o conteúdo local para o arquivo escolhido.
         *
         * @param uri destino do arquivo (SAF).
         * @param passphrase senha de cifragem do backup.
         */
        fun export(
            uri: Uri,
            passphrase: String,
        ) {
            runBackupOperation(
                successLog = "Backup exportado",
                failureLog = "Falha ao exportar o backup",
                successType = BackupSuccessType.Exported,
                exporting = true,
            ) {
                val output =
                    context.contentResolver.openOutputStream(uri)
                        ?: throw BackupRestoreException(EXPORT_UNAVAILABLE_MESSAGE)
                output.use { stream -> exportBackupUseCase(stream, passphrase) }
            }
        }

        /**
         * Restaura o backup do arquivo escolhido, substituindo o
         * conteúdo local.
         *
         * @param uri origem do arquivo (SAF).
         * @param passphrase senha de decifragem do backup.
         */
        fun restore(
            uri: Uri,
            passphrase: String,
        ) {
            runBackupOperation(
                successLog = "Backup restaurado",
                failureLog = "Falha ao restaurar o backup",
                successType = BackupSuccessType.Restored,
                exporting = false,
            ) {
                val input =
                    context.contentResolver.openInputStream(uri)
                        ?: throw BackupRestoreException(IMPORT_UNAVAILABLE_MESSAGE)
                input.use { stream -> restoreBackupUseCase(stream, passphrase) }.summary
            }
        }

        /**
         * Executa uma operação de backup.
         *
         * Exportar e restaurar têm a **mesma** forma: recusar se já há
         * uma operação em curso, marcar o formulário como ocupado,
         * rodar a operação no [Dispatchers.IO], publicar o resumo e
         * classificar a falha. A única diferença entre as duas é a
         * operação em si — então o que é comum mora aqui, escrito uma
         * vez.
         *
         * @param successLog texto do log de sucesso.
         * @param failureLog texto do log de falha.
         * @param successType o sucesso publicado no estado.
         * @param exporting se a operação é a de exportação.
         * @param operation a operação de backup (roda em [Dispatchers.IO]).
         */
        private fun runBackupOperation(
            successLog: String,
            failureLog: String,
            successType: BackupSuccessType,
            exporting: Boolean,
            operation: suspend () -> BackupSummary,
        ) {
            if (_state.value.isBusy) return
            _state.update { it.copy(isBusy = true, errorType = null, successType = null, summary = null) }
            viewModelScope.launch {
                try {
                    val summary = withContext(Dispatchers.IO) { operation() }
                    Log.d(TAG, "$successLog: ${summary.projects} projetos, ${summary.tasks} tarefas")
                    _state.update {
                        it.copy(isBusy = false, successType = successType, summary = summary)
                    }
                } catch (error: CancellationException) {
                    // Cancelamento da corrotina nao e falha de backup: propaga.
                    throw error
                } catch (error: Exception) {
                    Log.e(TAG, failureLog, error)
                    _state.update {
                        it.copy(isBusy = false, errorType = errorTypeFor(error, exporting = exporting))
                    }
                }
            }
        }

        /** Limpa a mensagem exibida (erro ou sucesso). */
        fun dismissMessage() {
            _state.update { it.copy(errorType = null, successType = null, summary = null) }
        }

        /**
         * Traduz a exceção de domínio em uma falha classificável pela
         * tela.
         */
        private fun errorTypeFor(
            error: Throwable,
            exporting: Boolean,
        ): BackupErrorType =
            when (error) {
                is BackupFormatException -> BackupErrorType.InvalidFile
                is UnsupportedBackupVersionException -> BackupErrorType.UnsupportedVersion
                is BackupIntegrityException -> BackupErrorType.Integrity
                is BackupTooLargeException -> BackupErrorType.TooLarge
                is BackupRestoreException -> BackupErrorType.RestoreFailed
                is BackupException -> BackupErrorType.Unexpected
                else -> if (exporting) BackupErrorType.ExportFailed else BackupErrorType.Unexpected
            }

        private companion object {
            const val EXPORT_UNAVAILABLE_MESSAGE: String =
                "Não foi possível abrir o arquivo escolhido para escrita."
            const val IMPORT_UNAVAILABLE_MESSAGE: String =
                "Não foi possível abrir o arquivo escolhido para leitura."
        }
    }
