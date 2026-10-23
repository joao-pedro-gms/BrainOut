// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme

/**
 * O cartão Neo renderiza o conteúdo que recebe.
 *
 * A receita saiu de três telas (o cartão de estados do Painel, o gráfico
 * de prioridades e o cartão de tarefa da Lista). O que este teste cobre é
 * o contrato do componente novo: o conteúdo chega dentro de uma coluna
 * que preenche o cartão, com o respiro e o espaçamento da identidade.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NeoCardTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `o cartao mostra o conteudo que recebe`() {
        compose.setContent {
            BrainOutTheme {
                NeoCard {
                    Text(text = "conteudo do cartao")
                }
            }
        }

        compose.onNodeWithText("conteudo do cartao").assertIsDisplayed()
    }

    @Test
    fun `o cartao empilha os blocos que recebe`() {
        compose.setContent {
            BrainOutTheme {
                NeoCard {
                    Text(text = "primeiro bloco")
                    Text(text = "segundo bloco")
                }
            }
        }

        compose.onNodeWithText("primeiro bloco").assertIsDisplayed()
        compose.onNodeWithText("segundo bloco").assertIsDisplayed()
    }
}
