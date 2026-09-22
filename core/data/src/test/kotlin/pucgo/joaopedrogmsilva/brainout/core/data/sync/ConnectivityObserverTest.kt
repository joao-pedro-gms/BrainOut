// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities

/**
 * Testes do [AndroidConnectivityObserver] (E3.4) com o
 * `ConnectivityManager` simulado do Robolectric
 * (`ShadowConnectivityManager`):
 *
 * - Estado inicial reflete a rede ativa do ambiente simulado.
 * - Callbacks registrados recebem `onAvailable`/`onLost` quando o
 *   shadow adiciona/remove redes, e o estado chega no Flow — o mesmo
 *   caminho do callback real no dispositivo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ConnectivityObserverTest {

    private lateinit var context: Context
    private lateinit var observer: AndroidConnectivityObserver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        observer = AndroidConnectivityObserver(context)
    }

    @Test
    fun `emite estado inicial e reage a rede disponível e perdida`() = runTest {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val shadow = shadowOf(connectivityManager)

        // Ambiente simulado sem redes ativas → estado inicial offline.
        shadow.clearAllNetworks()

        observer.observe().test {
            // 1) Inicial: offline.
            assertThat(awaitItem().isOnline).isFalse()

            // 2) Rede com internet fica disponível → online.
            val network = ShadowNetwork.newInstance(42)
            // addTransportType/addCapability não estão no stub público
            // do SDK 35 (@SystemApi); o caminho do Robolectric é via
            // shadow da instância real.
            val capabilities: NetworkCapabilities = ShadowNetworkCapabilities.newInstance()
                .also { caps ->
                    val capsShadow = shadowOf(caps)
                    capsShadow.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    capsShadow.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }
            shadow.addNetwork(network, null)
            shadow.setNetworkCapabilities(network, capabilities)
            shadow.setActiveNetworkInfo(null) // não interfere: usamos activeNetwork + capabilities

            // O callback registra onAvailable ao addNetwork? O shadow
            // não dispara callbacks automaticamente — entregamos
            // manualmente ao callback registrado (mesmo contrato).
            val callback = shadow.networkCallbacks.single()
            callback.onAvailable(network)
            assertThat(awaitItem().isOnline).isTrue()

            // 3) Rede perdida (nenhuma restante) → offline novamente.
            shadow.removeNetwork(network)
            callback.onLost(network)
            assertThat(awaitItem().isOnline).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }
}
