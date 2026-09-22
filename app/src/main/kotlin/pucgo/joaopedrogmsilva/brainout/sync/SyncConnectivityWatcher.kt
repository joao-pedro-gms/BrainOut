// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reconciliação automática ao voltar a conectividade (E3.4, item 3).
 *
 * Registrado no `onCreate` da [pucgo.joaopedrogmsilva.brainout.BrainOutApplication]
 * junto com o trabalho periódico do E3.3: quando o sistema entrega
 * `onAvailable` para uma rede com capacidade de internet, enfileira um
 * `OneTimeWorkRequest` de sincronização via [SyncScheduler].
 *
 * O OneTime usa `ExistingWorkPolicy.KEEP` com constraint
 * `NetworkType.CONNECTED`: chamadas repetidas durante uma drenagem em
 * curso são no-op e, offline, o trabalho fica retido até a constraint
 * ser satisfeita — enfileirar "a mais" é inofensivo. A fila
 * `pending_ops` é drenada em ordem e o banner da UI reflete o
 * resultado pela contagem do `PendingSyncMonitor`.
 *
 * O callback é registrado para o ciclo de vida inteiro do processo:
 * é o caminho mais barato para cumprir "reconcilia ao voltar a rede"
 * mesmo com o app em segundo plano (o WorkManager executa o trabalho
 * quando a constraint se satisfaz).
 */
@Singleton
class SyncConnectivityWatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncScheduler: SyncScheduler,
) {

    private var registered = false

    /**
     * Registra o callback de rede (idempotente). Chamado no
     * `Application.onCreate()`.
     */
    @Synchronized
    fun start() {
        if (registered) return
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val request = NetworkRequest.Builder()
            .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Drena imediatamente: reconcile a fila assim que a
                // rede volta. KEEP deduplica com drenagens em curso.
                syncScheduler.requestImmediateSync()
            }
        }

        connectivityManager.registerNetworkCallback(request, callback)
        registered = true
    }
}
