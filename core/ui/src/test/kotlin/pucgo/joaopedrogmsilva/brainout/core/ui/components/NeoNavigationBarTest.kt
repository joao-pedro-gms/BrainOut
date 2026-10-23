// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.R
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.LocalNeoWindowInfo
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoWindowInfo
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.fromSize

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NeoNavigationBarTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `fonte dupla em janela estreita mantem quatro abas em duas linhas`() {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(
                LocalNeoWindowInfo provides NeoWindowInfo.fromSize(320.dp, 800.dp),
                LocalDensity provides Density(density, fontScale = 2f),
            ) {
                BrainOutTheme {
                    NeoNavigationBar(
                        listOf(
                            NeoNavigationItem("projects", "Projetos", R.drawable.neo_ic_folder),
                            NeoNavigationItem("tasks", "Tarefas", R.drawable.neo_ic_assignment),
                            NeoNavigationItem("dashboard", "Painel", R.drawable.neo_ic_bar_chart),
                            NeoNavigationItem("settings", "Configurações", R.drawable.neo_ic_settings),
                        ),
                        "projects",
                        {},
                    )
                }
            }
        }
        listOf("projects", "tasks", "dashboard", "settings").forEach {
            compose.onNodeWithTag("main_tab_$it").assertIsDisplayed()
        }
        val first = compose.onNodeWithTag("main_tab_projects").fetchSemanticsNode().boundsInRoot
        val last = compose.onNodeWithTag("main_tab_settings").fetchSemanticsNode().boundsInRoot
        assertThat(last.top).isAtLeast(first.bottom)
    }
}
