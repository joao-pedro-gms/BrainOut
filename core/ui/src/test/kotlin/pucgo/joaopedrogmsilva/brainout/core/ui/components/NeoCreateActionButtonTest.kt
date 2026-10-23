// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.LocalNeoWindowInfo
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoWindowInfo
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.fromSize

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NeoCreateActionButtonTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `acao compacta permanece nomeada e acionavel em janela estreita`() {
        var clicks = 0
        compose.setContent {
            CompositionLocalProvider(LocalNeoWindowInfo provides NeoWindowInfo.fromSize(320.dp, 480.dp)) {
                BrainOutTheme { NeoCreateActionButton("Nova tarefa", { clicks++ }) }
            }
        }
        compose.onNodeWithContentDescription("Nova tarefa").assertHasClickAction().performClick()
        compose.runOnIdle { assertThat(clicks).isEqualTo(1) }
    }
}
