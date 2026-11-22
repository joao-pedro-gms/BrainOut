// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Testes do seletor de cor de tag por amostras (`NeoColorSwatches`).
//
// Travam o contrato que a tela de edição de tag consome: as doze
// amostras curadas existem e são encontráveis pelo testTag estável, o
// toque devolve o hex `#RRGGBB` exato, a amostra selecionada se anuncia
// como selecionada e toda amostra carrega o nome da cor como
// `contentDescription` — sem isso o TalkBack leria caixas mudas.

package pucgo.joaopedrogmsilva.brainout.core.ui.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "pt-rBR")
class NeoColorSwatchesTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `exibe exatamente as 12 amostras curadas`() {
        composeRule.setContent {
            BrainOutTheme {
                NeoColorSwatches(selected = NEO_TAG_COLORS.first(), onSelect = {})
            }
        }

        assertThat(NEO_TAG_COLORS).hasSize(12)
        NEO_TAG_COLORS.forEach { hex ->
            composeRule.onNodeWithTag(neoColorSwatchTag(hex)).assertExists()
        }
    }

    @Test
    fun `toque na amostra dispara onSelect com o hex exato`() {
        val target = NEO_TAG_COLORS[2]
        var selected: String? = null
        composeRule.setContent {
            BrainOutTheme {
                NeoColorSwatches(selected = NEO_TAG_COLORS.first(), onSelect = { selected = it })
            }
        }

        composeRule.onNodeWithTag(neoColorSwatchTag(target)).performClick()

        assertThat(selected).isEqualTo(target)
    }

    @Test
    fun `amostra selecionada se anuncia como selecionada`() {
        val selected = NEO_TAG_COLORS[1]
        composeRule.setContent {
            BrainOutTheme {
                NeoColorSwatches(selected = selected, onSelect = {})
            }
        }

        val node = composeRule.onNodeWithTag(neoColorSwatchTag(selected))
        node.assertIsSelected()
        assertThat(node.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
            .isEqualTo("Selecionada")
    }

    @Test
    fun `toda amostra carrega o nome da cor como descricao de conteudo`() {
        composeRule.setContent {
            BrainOutTheme {
                NeoColorSwatches(selected = NEO_TAG_COLORS.first(), onSelect = {})
            }
        }

        NEO_TAG_COLORS.forEach { hex ->
            val config = composeRule.onNodeWithTag(neoColorSwatchTag(hex)).fetchSemanticsNode().config
            val description = config[SemanticsProperties.ContentDescription].joinToString("")
            assertThat(description).isNotEmpty()
        }
    }
}
