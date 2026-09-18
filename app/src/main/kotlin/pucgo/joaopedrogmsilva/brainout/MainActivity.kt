// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.navigation.BrainOutNavHost

/**
 * Activity única do BrainOut.
 *
 * Hospeda o `NavHost` entregue no marco E1.3 que conecta as 6 telas:
 * Splash → Login → Home → ProjectDetail/Settings → Login (via "Sair").
 *
 * O `NavController` é criado via `rememberNavController()` para sobreviver
 * a recomposições enquanto a Activity existir; cada destino individual
 * recebe callbacks `lambda` em vez de acessar o controller diretamente,
 * o que facilita testes isolados de cada tela.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
                    BrainOutNavHost(navController = navController)
                }
            }
        }
    }
}
