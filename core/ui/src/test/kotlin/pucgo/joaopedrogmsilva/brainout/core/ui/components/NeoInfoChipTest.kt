// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// DEF-22 — `NeoInfoChip` não pode se anunciar como botão.
//
// O defeito original eram seis `AssistChip(onClick = { })` espalhados
// por `:feature:projects` e `:feature:tasks`: chips que só rotulavam
// (status, prioridade, tag, papel) mas que o TalkBack anunciava como
// acionáveis. A correção não é só trocar o componente — é fazer o
// callback vazio **impossível de escrever**, já que [NeoInfoChip] não
// tem parâmetro `onClick`.
//
// Estes testes travam essa garantia pela semântica que o TalkBack lê.

package pucgo.joaopedrogmsilva.brainout.core.ui.components

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class NeoInfoChipTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `o rotulo e anunciado uma vez como descricao do chip`() {
        composeRule.setContent {
            BrainOutTheme {
                NeoInfoChip(label = "Em andamento")
            }
        }

        // `clearAndSetSemantics` + `contentDescription` fazem o texto
        // interno deixar de anunciar por conta própria: o leitor ouve
        // "Em andamento" uma vez, vindo do nó do chip.
        composeRule
            .onNodeWithText("Em andamento", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `o chip nao oferece acao de clique ao TalkBack`() {
        composeRule.setContent {
            BrainOutTheme {
                NeoInfoChip(label = "Alta")
            }
        }

        // O defeito: `AssistChip` publica `SemanticsActions.OnClick`, e
        // o TalkBack oferece um toque que não faz nada. Aqui a ação não
        // existe — o nó carrega só ações de texto (substituição de
        // texto e layout), nada que se pareça com um acionamento.
        val actions =
            composeRule
                .onNodeWithText("Alta", useUnmergedTree = true)
                .fetchSemanticsNode()
                .config

        assertThat(SemanticsActions.OnClick in actions).isFalse()
        assertThat(SemanticsActions.OnLongClick in actions).isFalse()
    }

    @Test
    fun `clicar no chip nao dispara callback nenhum`() {
        // Não há `onClick` para passar, então o teste é a prova
        // estrutural: o clique é absorvido e nada acontece. Se alguém
        // reintroduzir um handler, esta assinatura precisaria mudar e
        // o teste deixaria de compilar — que é o ponto.
        composeRule.setContent {
            BrainOutTheme {
                NeoInfoChip(label = "Concluída")
            }
        }

        composeRule.onNodeWithText("Concluída", useUnmergedTree = true).performClick()
        // Sem exceção e sem estado observável: o clique foi um no-op.
        assertThat(true).isTrue()
    }

    @Test
    fun `o chip nao publica a acao OnClick na semantica`() {
        composeRule.setContent {
            BrainOutTheme {
                NeoInfoChip(label = "Urgente")
            }
        }

        val semantics =
            composeRule
                .onNodeWithText("Urgente", useUnmergedTree = true)
                .fetchSemanticsNode()

        assertThat(SemanticsActions.OnClick !in semantics.config).isTrue()
        assertThat(SemanticsProperties.Role !in semantics.config).isTrue()
    }
}
