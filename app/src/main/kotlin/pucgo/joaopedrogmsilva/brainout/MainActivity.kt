// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
 * Permissão de notificações (E3.6): na primeira composição da Home
 * pede `POST_NOTIFICATIONS` (Android 13+) uma única vez — a flag
 * "já pedido" é persistida em [NotificationPermissionStore]
 * (SessionStore/DataStore). Negativa mostra um toast explicativo e
 * não insiste nas próximas entradas.
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
                        )
                        NotificationPermissionRequest(
                            permissionStore = notificationPermissionStore,
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
 * Pede a permissão `POST_NOTIFICATIONS` uma única vez (E3.6).
 *
 * Regras:
 * - Só dispara no Android 13+ (API < 33 não exige pedido runtime).
 * - Pergunta ao [NotificationPermissionStore] se já pedimos; se sim,
 *   não pede novamente (não insistir após negativa).
 * - Negativa → toast explicativo com o texto do E3.6.
 */
@Composable
private fun NotificationPermissionRequest(permissionStore: NotificationPermissionStore) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = androidx.compose.ui.platform.LocalContext.current
    val deniedToast = stringResource(R.string.deadline_permission_denied_toast)
    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            Log.d(TAG, "Resultado da permissão POST_NOTIFICATIONS: concedida=$granted")
            if (!granted) {
                Toast
                    .makeText(
                        context,
                        deniedToast,
                        Toast.LENGTH_LONG,
                    ).show()
            }
            (context as? ComponentActivity)?.lifecycleScope?.launch {
                permissionStore.markAsked()
            }
        }

    LaunchedEffect(Unit) {
        val alreadyGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) {
            Log.d(TAG, "Permissão POST_NOTIFICATIONS já concedida previamente")
            return@LaunchedEffect
        }
        val askedBefore = permissionStore.wasAsked()
        if (!askedBefore) {
            Log.d(TAG, "Solicitando permissão POST_NOTIFICATIONS...")
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Log.d(TAG, "Permissão POST_NOTIFICATIONS negada anteriormente; não solicitando novamente")
        }
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
                startDestination = current,
            )
    }
}
