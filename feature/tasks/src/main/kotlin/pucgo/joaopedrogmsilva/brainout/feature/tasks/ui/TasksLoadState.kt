// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach

/**
 * Resultado de uma leitura do Room nas telas de Tarefas e Painel.
 *
 * "Leu" e "falhou" são estados diferentes por construção: o estado
 * interno da leitura carrega só os dados, e a mensagem mora em
 * [TasksLoadState]. Antes o `catch` emitia um estado com
 * `errorMessage` preenchido e o passo seguinte usava esse campo como
 * flag de sucesso, enquanto o `combine` final sobrescrevia o mesmo
 * campo — o campo era escrito duas vezes e lido uma vez.
 */
internal sealed interface TasksLoadResult<out T> {
    /** Leitura bem-sucedida (a lista pode estar vazia). */
    data class Loaded<T>(
        val value: T,
    ) : TasksLoadResult<T>

    /**
     * A leitura falhou. A mensagem mora em [TasksLoadState] — este tipo
     * só distingue o desfecho, sem duplicar o texto.
     */
    data object Failed : TasksLoadResult<Nothing>
}

/**
 * Estado de leitura compartilhado pelas telas de Tarefas e Painel (E2.8).
 *
 * As duas telas tinham o mesmo par "token de retry + mensagem de erro" e
 * os mesmos métodos de re-tentativa, escritos duas vezes. Aqui a política
 * mora em um lugar só:
 *
 * - [retry] descarta a coleta atual e re-assina os repositórios;
 * - uma leitura bem-sucedida limpa o erro pendente;
 * - uma falha de carga registra [TasksErrorType.LoadFailed].
 */
internal class TasksLoadState {
    /**
     * Token de retry. Cada chamada a [retry] o incrementa; o pipeline de
     * UI depende dele como chave de `flatMapLatest`, então a mudança
     * descarta a coleta anterior (liberando o `WhileSubscribed`
     * interno) e inicia uma nova inscrição nos repositórios.
     */
    private val _retryToken: MutableStateFlow<Int> = MutableStateFlow(0)
    val retryToken: StateFlow<Int> = _retryToken.asStateFlow()

    private val _errorMessage: MutableStateFlow<TasksErrorType?> = MutableStateFlow(null)
    val errorMessage: StateFlow<TasksErrorType?> = _errorMessage.asStateFlow()

    /** Re-assina o pipeline após uma falha. */
    fun retry() {
        _errorMessage.value = null
        _retryToken.value = _retryToken.value + 1
    }

    /** Limpa a mensagem de erro atual (chamado pela UI ao dispensar o banner). */
    fun clearError() {
        _errorMessage.value = null
    }

    /** Registra uma falha de carga. */
    fun onLoadFailure() {
        _errorMessage.value = TasksErrorType.LoadFailed
    }

    /** Limpa o erro pendente — só em leitura bem-sucedida. */
    fun onLoadSuccess() {
        _errorMessage.value = null
    }
}

/**
 * A política de falha de uma leitura: a exceção vira
 * [TasksLoadResult.Failed], o estado registra a falha, e o erro pendente
 * é limpo **apenas** em leituras bem-sucedidas.
 *
 * Sem o filtro do `onEach`, o `catch` emitiria um estado vazio e o passo
 * seguinte resetaria o erro para `null` imediatamente, "engolindo" a
 * falha. (E2.8)
 *
 * A política morava escrita nos dois ViewModels — Tarefas e Painel — com
 * o mesmo encadeamento de operadores e as mesmas regras; o que diferia era
 * a mensagem do log, que continua na chamada.
 *
 * @param onFailure o que fazer com a exceção antes de registrar a falha
 *  — nos dois casos, o log.
 */
internal fun <T> Flow<TasksLoadResult<T>>.withLoadFailurePolicy(
    state: TasksLoadState,
    onFailure: (Throwable) -> Unit,
): Flow<TasksLoadResult<T>> =
    catch { throwable ->
        if (throwable is CancellationException) throw throwable
        onFailure(throwable)
        state.onLoadFailure()
        emit(TasksLoadResult.Failed)
    }.onEach { result ->
        if (result is TasksLoadResult.Loaded) {
            state.onLoadSuccess()
        }
    }
