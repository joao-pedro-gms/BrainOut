// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.components

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme

/**
 * O botão que alterna a revelação da senha.
 *
 * A receita estava escrita duas vezes — no campo de senha do login e no
 * do backup — e **nenhuma** das duas tinha teste. O que se verifica aqui
 * é a descrição acessível: ela acompanha o estado (revelar/ocultar) e
 * é o que o TalkBack anuncia.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "pt-rBR")
class NeoVisibilityToggleTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `a descricao anuncia como revelar quando a senha esta oculta`() {
        compose.setContent { toggle(visible = false) }

        compose.onNode(hasContentDescription(SHOW)).assertExists()
    }

    @Test
    fun `a descricao anuncia como ocultar quando a senha esta revelada`() {
        compose.setContent { toggle(visible = true) }

        compose.onNode(hasContentDescription(HIDE)).assertExists()
    }

    @Test
    fun `o botao e acionavel`() {
        var clicks = 0
        compose.setContent { toggle(visible = false, onToggle = { clicks++ }) }

        compose.onNode(hasContentDescription(SHOW)).assertHasClickAction().performClick()
        compose.runOnIdle { assertThat(clicks).isEqualTo(1) }
    }

    @androidx.compose.runtime.Composable
    private fun toggle(
        visible: Boolean,
        onToggle: () -> Unit = {},
    ) {
        BrainOutTheme {
            NeoVisibilityToggle(
                visible = visible,
                showLabel = SHOW,
                hideLabel = HIDE,
                onToggle = onToggle,
            )
        }
    }

    private companion object {
        const val SHOW: String = "Mostrar senha"
        const val HIDE: String = "Ocultar senha"
    }
}
