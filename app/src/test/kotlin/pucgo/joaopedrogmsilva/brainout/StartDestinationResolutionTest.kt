// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.navigation.BrainOutRoutes

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class StartDestinationResolutionTest {
    @Test
    fun `sessao ativa abre o grafo principal`() =
        runBlocking {
            assertThat(resolveStartDestination { true }).isEqualTo(BrainOutRoutes.MainGraph)
        }

    @Test
    fun `sessao ausente ou com erro abre o splash`() =
        runBlocking {
            assertThat(resolveStartDestination { false }).isEqualTo(BrainOutRoutes.Splash)
            assertThat(resolveStartDestination { error("falha de leitura") }).isEqualTo(BrainOutRoutes.Splash)
        }

    @Test
    fun `cancelamento da leitura da sessao e propagado`() =
        runBlocking {
            val cancellation = CancellationException("activity encerrada")
            var observed: CancellationException? = null

            try {
                resolveStartDestination { throw cancellation }
            } catch (error: CancellationException) {
                observed = error
            }

            assertThat(observed).isNotNull()
            assertThat(observed!!.message).isEqualTo("activity encerrada")
        }
}
