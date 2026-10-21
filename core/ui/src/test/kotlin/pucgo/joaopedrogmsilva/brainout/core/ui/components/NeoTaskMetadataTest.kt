// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "pt-rBR")
class NeoTaskMetadataTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `cinco prioridades anunciam nomes completos sem acao de clique`() {
        compose.setContent { BrainOutTheme { Column { (0..4).forEach { NeoPriorityIndicator(it) } } } }
        listOf("Baixa", "Média", "Alta", "Urgente", "Crítica").forEach { label ->
            val node = compose.onNodeWithContentDescription("Prioridade: $label").fetchSemanticsNode()
            assertThat(SemanticsActions.OnClick in node.config).isFalse()
        }
    }

    @Test fun `legenda expande por acao e recolhe sem remover seu controle`() {
        compose.setContent { BrainOutTheme { NeoPriorityLegend() } }
        compose.onNodeWithText("Crítica").assertDoesNotExist()
        compose.onNodeWithText("Legenda de prioridades").performClick()
        compose.onNodeWithText("Crítica").assertExists()
        compose.onNodeWithText("Legenda de prioridades").performClick()
        compose.onNodeWithText("Crítica").assertDoesNotExist()
    }
}
