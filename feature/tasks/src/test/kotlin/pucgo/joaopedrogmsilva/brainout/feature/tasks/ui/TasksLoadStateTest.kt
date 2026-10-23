// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * A política de falha compartilhada pelas telas de Tarefas e Painel.
 *
 * A regra que mais facilmente quebraria é a do `onEach`: sem ele, o `catch`
 * emitiria um estado vazio e o passo seguinte resetaria o erro para `null`
 * imediatamente — a falha seria "engolida" e o banner nunca apareceria.
 * O caso de falha cobre exatamente isso.
 */
class TasksLoadStateTest {
    @Test
    fun `uma leitura bem sucedida limpa o erro pendente`() =
        runTest {
            val state = TasksLoadState()
            state.onLoadFailure()

            flowOf(TasksLoadResult.Loaded("conteudo"))
                .withLoadFailurePolicy(state) {}
                .test {
                    assertThat(awaitItem()).isEqualTo(TasksLoadResult.Loaded("conteudo"))
                    cancelAndIgnoreRemainingEvents()
                }

            assertThat(state.errorMessage.value).isNull()
        }

    @Test
    fun `uma falha vira Failed e a mensagem continua valendo`() =
        runTest {
            val state = TasksLoadState()

            flow<Int> { throw IllegalStateException("boom") }
                .map { TasksLoadResult.Loaded(it) }
                .withLoadFailurePolicy(state) {}
                .test {
                    assertThat(awaitItem()).isEqualTo(TasksLoadResult.Failed)
                    cancelAndIgnoreRemainingEvents()
                }

            // O ponto do filtro: o erro continua valendo depois do `catch`.
            // Sem o `onEach`, o passo seguinte resetaria a mensagem para `null`
            // e o banner de erro nunca apareceria.
            assertThat(state.errorMessage.value).isEqualTo(TasksErrorType.LoadFailed)
        }

    @Test
    fun `a excecao chega ao tratamento da tela`() =
        runTest {
            val state = TasksLoadState()
            var received: Throwable? = null

            flow<Int> { throw IllegalStateException("boom") }
                .map { TasksLoadResult.Loaded(it) }
                .withLoadFailurePolicy(state) { throwable -> received = throwable }
                .test {
                    assertThat(awaitItem()).isEqualTo(TasksLoadResult.Failed)
                    cancelAndIgnoreRemainingEvents()
                }

            assertThat(received).isInstanceOf(IllegalStateException::class.java)
            assertThat(received).hasMessageThat().contains("boom")
        }

    @Test
    fun `o cancelamento e re-lancado em vez de virar falha`() {
        val state = TasksLoadState()

        val thrown =
            runCatching {
                runBlocking {
                    flow<Int> { throw CancellationException("cancelado") }
                        .map { TasksLoadResult.Loaded(it) }
                        .withLoadFailurePolicy(state) {}
                        .collect { }
                }
            }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(CancellationException::class.java)
        // Cancelamento não é falha de leitura: o banner não deve aparecer.
        assertThat(state.errorMessage.value).isNull()
    }
}
