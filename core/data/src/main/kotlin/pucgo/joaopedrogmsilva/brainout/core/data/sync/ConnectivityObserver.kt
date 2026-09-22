// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Estado de conectividade exposto à UI (E3.4).
 *
 * @property isOnline `true` quando existe uma rede com capacidade
 *   `NET_CAPABILITY_INTERNET` validada (ou em processo de validação
 *   — o callback `onAvailable` já basta para o banner).
 */
data class ConnectivityState(
    val isOnline: Boolean,
)

/**
 * Observador reativo de conectividade (E3.4, item 1).
 *
 * Registra um [ConnectivityManager.NetworkCallback] e expõe o estado
 * como [Flow] frio: o callback só é registrado enquanto houver
 * coletor (o banner da tela observando), e o canal fecha no fim da
 * coleta — sem listeners pendurados quando o app está em background.
 *
 * A capacidade exigida é `NET_CAPABILITY_INTERNET` via
 * [NetworkRequest]: redes "conectadas" sem internet real (cativo,
 * modo avião com Wi-Fi associado) não contam como online.
 *
 * Testes: Robolectric fornece um `ConnectivityManager` simulável via
 * `ShadowConnectivityManager`; a implementação é injetada via Hilt
 * (`@ApplicationContext`) e a interface [ConnectivityObserver] pode
 * ser falsificada nos testes de ViewModel.
 */
interface ConnectivityObserver {

    /** Emite o estado atual e cada mudança subsequente. */
    fun observe(): Flow<ConnectivityState>
}

@Singleton
class AndroidConnectivityObserver @Inject constructor(
    @ApplicationContext private val context: Context,
) : ConnectivityObserver {

    override fun observe(): Flow<ConnectivityState> = callbackFlow {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(ConnectivityState(isOnline = true))
            }

            override fun onLost(network: Network) {
                // onLost por rede: só consideramos offline quando
                // não resta nenhuma rede ativa (multi-rede: Wi-Fi +
                // dados móveis — a perda de uma não é ficar offline).
                val hasActiveNetwork = connectivityManager.activeNetwork != null
                if (!hasActiveNetwork) {
                    trySend(ConnectivityState(isOnline = false))
                }
            }
        }

        // Estado inicial: consulta pontual antes de registrar o
        // callback, para que o banner já apareça correto na primeira
        // composição (o app pode abrir já em modo avião).
        val initialActive = connectivityManager.activeNetwork
        val initialOnline = initialActive?.let { network ->
            connectivityManager
                .getNetworkCapabilities(network)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } ?: false
        trySend(ConnectivityState(isOnline = initialOnline))

        connectivityManager.registerNetworkCallback(request, callback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
}
