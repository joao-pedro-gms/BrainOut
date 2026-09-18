// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.navigation.BrainOutNavHost
import pucgo.joaopedrogmsilva.brainout.navigation.BrainOutRoutes
import javax.inject.Inject

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
 * O NavController é criado via `rememberNavController()` para sobreviver
 * a recomposições enquanto a Activity existir; cada destino individual
 * recebe callbacks `lambda` em vez de acessar o controller diretamente,
 * o que facilita testes isolados de cada tela.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var activeUserProvider: ActiveUserProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrainOutTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    MainRoot(
                        navController = navController,
                        activeUserProvider = activeUserProvider,
                    )
                }
            }
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
        val hasActiveUser: Boolean = withContext(Dispatchers.IO) {
            activeUserProvider.currentActiveUser() != null
        }
        startDestination = if (hasActiveUser) {
            BrainOutRoutes.Home
        } else {
            BrainOutRoutes.Splash
        }
    }

    when (val current = startDestination) {
        null -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        else -> BrainOutNavHost(
            navController = navController,
            activeUserProvider = activeUserProvider,
            startDestination = current,
        )
    }
}
