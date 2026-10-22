// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pucgo.joaopedrogmsilva.brainout.core.data.session.ActiveUserProvider
import pucgo.joaopedrogmsilva.brainout.core.domain.model.ThemeMode
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ThemePreferencesRepository
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.navigation.BrainOutNavHost
import pucgo.joaopedrogmsilva.brainout.navigation.BrainOutRoutes
import pucgo.joaopedrogmsilva.brainout.notifications.NotificationPermissionStore
import javax.inject.Inject

private const val TAG = "BrainOut:MainActivity"

/**
 * Activity única do BrainOut.
 *
 * Hospeda o `NavHost` entregue no marco E1.3 que conecta as 6 telas:
 * Splash → Login → Home → ProjectDetail/Settings → Login (via "Sair").
 *
 * Decide a rota inicial com base no [ActiveUserProvider] (E1.8):
 * - Se houver um [pucgo.joaopedrogmsilva.brainout.core.domain.model.User]
 *   ativo persistido em `DataStore` (via [pucgo.joaopedrogmsilva.brainout.core.data.session.SessionStore]),
 *   inicia em `home` — pulando Splash/Login.
 * - Caso contrário, mantém o comportamento original (Splash → Login).
 *
 * Permissão de notificações (E3.6): a **primeira composição da Home**
 * pede `POST_NOTIFICATIONS` (Android 13+) uma única vez — a flag
 * "já pedido" é persistida em [NotificationPermissionStore]
 * (SessionStore/DataStore). Negativa mostra um toast explicativo e
 * não insiste nas próximas entradas. O pedido mora no destino Home do
 * NavHost, e não na raiz: em instalação limpa, a primeira composição
 * da árvore é a tela de splash, e o diálogo do sistema aparecia
 * antes do login (DEF-45).
 *
 * O NavController é criado via `rememberNavController()` para sobreviver
 * a recomposições enquanto a Activity existir; cada destino individual
 * recebe callbacks `lambda` em vez de acessar o controller diretamente,
 * o que facilita testes isolados de cada tela.
 *
 * Tema (NB-06, E4.5): esta Activity é o **ponto único de resolução**
 * do tema. Lê a preferência persistida
 * ([ThemePreferencesRepository] — System/Light/Dark), consulta
 * `isSystemInDarkTheme()` uma única vez e aplica a precedência
 * ([ThemeMode.resolvesToDark]); o booleano resultante é repassado ao
 * [BrainOutTheme], que o propaga para toda a árvore. Nenhum subtree
 * recalcula por conta própria (plano §3.2.2).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var activeUserProvider: ActiveUserProvider

    @Inject
    lateinit var notificationPermissionStore: NotificationPermissionStore

    @Inject
    lateinit var themePreferencesRepository: ThemePreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Inicializando MainActivity")
        enableEdgeToEdge()
        setContent {
            // Preferência de tema reativa: `null` = ainda não lida.
            val themeMode by
                themePreferencesRepository.observe().collectAsState(initial = null)
            val systemInDarkTheme = isSystemInDarkTheme()
            val resolvedDarkTheme = (themeMode ?: ThemeMode.SYSTEM).resolvesToDark(systemInDarkTheme)

            BrainOutTheme(darkTheme = resolvedDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()
                    if (themeMode == null) {
                        // Enquanto a preferência não chega não componhamos o
                        // app: evita o flash do tema errado na primeira abertura.
                        LoadingIndicator()
                    } else {
                        MainRoot(
                            navController = navController,
                            activeUserProvider = activeUserProvider,
                            notificationPermissionStore = notificationPermissionStore,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Spinner de tela cheia usado pelos estados de carregamento da raiz
 * (preferência de tema e rota inicial). Um componente para os dois —
 * mesma cor, mesmo alinhamento.
 */
@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

/**
 * Resolve a rota inicial antes de montar o `NavHost` e delega para
 * [BrainOutNavHost] quando pronta. Mostra um spinner enquanto a
 * leitura do `DataStore` está em voo.
 */
@Composable
private fun MainRoot(
    navController: androidx.navigation.NavHostController,
    activeUserProvider: ActiveUserProvider,
    notificationPermissionStore: NotificationPermissionStore,
) {
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val hasActiveUser: Boolean =
            withContext(Dispatchers.IO) {
                activeUserProvider.currentActiveUser() != null
            }
        // Rota inicial resolvida **uma** vez, direto para o grafo que o
        // `NavHost` entende. `Home` não existe como destino de topo — é
        // a tela de dentro do `MainGraph` —, então o sentinel "Home" não
        // precisa atravessar esta fronteira.
        val destination =
            if (hasActiveUser) {
                BrainOutRoutes.MainGraph
            } else {
                BrainOutRoutes.Splash
            }
        Log.d(TAG, "Sessão ativa encontrada=$hasActiveUser -> Rota inicial: $destination")
        startDestination = destination
    }

    when (val current = startDestination) {
        null -> LoadingIndicator()
        else ->
            BrainOutNavHost(
                navController = navController,
                activeUserProvider = activeUserProvider,
                notificationPermissionStore = notificationPermissionStore,
                startDestination = current,
            )
    }
}
