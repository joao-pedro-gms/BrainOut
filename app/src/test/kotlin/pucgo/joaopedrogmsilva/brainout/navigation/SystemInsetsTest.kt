// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// DEF-42 — quem consome o inseto de sistema em edge-to-edge.
//
// O `MainActivity` liga `enableEdgeToEdge()`, e com `targetSdk 37` o
// edge-to-edge é imposto pela plataforma. A arquitetura atual é:
//
//   - o `BrainOutShell` (Scaffold do app) declara
//     `contentWindowInsets = WindowInsets(0, 0, 0, 0)` — ele NÃO
//     consome inseto nenhum, para não tirar de quem está abaixo;
//   - cada tela tem o seu próprio `Scaffold`, cujo
//     `contentWindowInsets` padrão é `WindowInsets.systemBars` — é
//     ela que cede espaço para a barra de status e para a barra de
//     gestos.
//
// Este teste publica um inseto de barra de status de verdade na
// `ComposeView` (Robolectric + `dispatchApplyWindowInsets`) e
// verifica a propriedade geométrica que o bug quebrava: o conteúdo do
// destino começa **abaixo** da barra de status.
//
// Se o shell passasse a consumir o inseto, o teste continuaria verde
// (o conteúdo desceria ainda mais); o que ele trava é o outro lado —
// o conteúdo NÃO pode começar em cima da barra de status.

package pucgo.joaopedrogmsilva.brainout.navigation

import android.view.ViewGroup
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.feature.tasks.navigation.TasksRoutes
import android.graphics.Insets as AndroidInsets

/** Altura da barra de status simulada, em px do espaço da raiz. */
private const val STATUS_BAR_PX = 50

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "pt-rBR")
class SystemInsetsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `o conteudo da tela comeca abaixo da barra de status`() {
        compose.setContent {
            val nav: NavHostController = rememberNavController()
            BrainOutTheme {
                BrainOutShell(nav) { modifier ->
                    NavHost(nav, BrainOutRoutes.MainGraph, modifier) {
                        mainDestinations(
                            projects = {
                                composable(BrainOutRoutes.Home) {
                                    // Reproduz a estrutura das telas
                                    // reais: Scaffold próprio, que é
                                    // quem consome o inseto.
                                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                                        Text(
                                            text = "Lista de projetos",
                                            modifier =
                                                Modifier
                                                    .padding(innerPadding)
                                                    .testTag(CONTENT_TAG),
                                        )
                                    }
                                }
                            },
                            tasks = { composable(TasksRoutes.TASKS) { Text("Tarefas") } },
                            dashboard = { composable(TasksRoutes.DASHBOARD) { Text("Painel") } },
                            settings = { composable(BrainOutRoutes.Settings) { Text("Preferências") } },
                        )
                    }
                }
            }
        }

        showStatusBar()

        val top =
            compose
                .onNodeWithTag(CONTENT_TAG)
                .fetchSemanticsNode()
                .boundsInRoot.top
        check(top >= STATUS_BAR_PX) {
            "conteúdo invadiu a barra de status: top=$top < $STATUS_BAR_PX"
        }
    }

    /**
     * Publica um inseto de barra de status na `ComposeView`.
     *
     * Tem que ser na `ComposeView` — despachar no `decorView` não
     * chega no listener de insets do Compose e o teste passaria vazio
     * (o mesmo cuidado do `AuthImeInsetsTest`).
     */
    private fun showStatusBar() {
        compose
            .activity
            .window
            .decorView
            .findViewById<ViewGroup>(android.R.id.content)
            .getChildAt(0)
            .dispatchApplyWindowInsets(
                WindowInsets
                    .Builder()
                    .setInsets(WindowInsets.Type.statusBars(), AndroidInsets.of(0, STATUS_BAR_PX, 0, 0))
                    .setVisible(WindowInsets.Type.statusBars(), true)
                    .build(),
            )
        compose.waitForIdle()
    }

    private companion object {
        const val CONTENT_TAG = "system_insets_content"
    }
}
