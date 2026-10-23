// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// O contrato da receita de cor da barra superior Neo.

package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * O contrato da [neoTopAppBarColors].
 *
 * A receita saiu de quatro cópias — Home, Detalhe de projeto, Lista de
 * tarefas e Configurações — e a diferença entre elas é o que este teste
 * prende:
 *
 * - o fundo da barra e a cor do título vêm do tema;
 * - quem **não** define a cor de um ícone herda o padrão do Material, e
 *   não um valor fixo — é o que mantém a barra que não tem aquele ícone
 *   igual à de antes da extração;
 * - quem define, define.
 *
 * A herança é o ponto que mais facilmente quebra: `Color.Unspecified` é o que
 * o Material 3 entende como "mantenha o valor anterior". Trocar o padrão
 * por um `Color` fixo faria a Home e a tela de Configurações mudarem de
 * cor sem que ninguém percebesse o motivo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NeoTopBarTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `a barra usa o fundo e a cor de titulo do tema`() {
        lateinit var colors: TopAppBarColors
        lateinit var scheme: ColorScheme
        compose.setContent {
            BrainOutNeoTheme(darkTheme = false) {
                scheme = MaterialTheme.colorScheme
                colors = neoTopAppBarColors()
            }
        }

        compose.runOnIdle {
            assertThat(colors.containerColor).isEqualTo(scheme.background)
            assertThat(colors.titleContentColor).isEqualTo(scheme.onBackground)
        }
    }

    @Test
    fun `quem nao define a cor de um icone herda o padrao do tema`() {
        lateinit var colors: TopAppBarColors
        lateinit var defaults: TopAppBarColors
        compose.setContent {
            BrainOutNeoTheme(darkTheme = false) {
                defaults = TopAppBarDefaults.topAppBarColors()
                colors = neoTopAppBarColors()
            }
        }

        compose.runOnIdle {
            assertThat(colors.navigationIconContentColor).isEqualTo(defaults.navigationIconContentColor)
            assertThat(colors.actionIconContentColor).isEqualTo(defaults.actionIconContentColor)
        }
    }

    @Test
    fun `a cor de icone passada na chamada e a que vale`() {
        lateinit var colors: TopAppBarColors
        compose.setContent {
            BrainOutNeoTheme(darkTheme = false) {
                colors = neoTopAppBarColors(navigationIconContentColor = Color.Red)
            }
        }

        compose.runOnIdle {
            assertThat(colors.navigationIconContentColor).isEqualTo(Color.Red)
            // O outro ícone não foi pedido: continua herdando.
            assertThat(colors.actionIconContentColor).isNotEqualTo(Color.Red)
        }
    }
}
