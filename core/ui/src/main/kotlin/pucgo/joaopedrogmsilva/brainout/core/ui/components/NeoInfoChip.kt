// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Chip **informativo** (DEF-22).
//
// `AssistChip` exige um `onClick`. Quem usava chips só para rotular
// status/prioridade/tag/badge passava `onClick = { }` — seis sites no
// app. O efeito é pior do que um callback inerte: o TalkBack anuncia o
// chip como "botão", o leitor oferece um toque que não faz nada, e o
// alvo de toque dentro do card engole o clique que abriria o card.
//
// `DESIGN.md` §6 é explícito: «Chips informativos não são botões.
// Seletores usam `selectable`/`toggleable`, grupos e `stateDescription`;
// não callbacks vazios.»
//
// Aqui a correção é **estrutural**: [NeoInfoChip] não tem parâmetro
// `onClick`, então não há como reintroduzir o callback vazio por
// engano. Quem precisa de um chip acionável usa `FilterChip` (seleção)
// ou um botão — nunca esta função.

package pucgo.joaopedrogmsilva.brainout.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoBorders
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoRadii

/**
 * Rótulo visual de um atributo (status, prioridade, tag, papel).
 *
 * Não é interativo por construção: sem `onClick`, sem `Modifier.clickable`
 * e sem `minimumInteractiveComponentSize`. O conteúdo é declarado como
 * `contentDescription` do nó inteiro, então o TalkBack anuncia o texto
 * uma vez e não o trata como ação.
 *
 * A borda usa o token `outline` cheio — 16.10:1 no claro e 15.95:1 no
 * escuro. Ela existe porque o preenchimento (`surfaceVariant` puro) só
 * dá 1.15:1 / 1.33:1 contra o fundo: é a **fronteira**, não o fundo, que
 * faz a região ser perceptível, e é a fronteira que o WCAG §1.4.11 exige.
 *
 * @param label texto do atributo. Não precisa de cor própria: herda a
 *  `labelColor` padrão do tema.
 * @param modifier mods do nó; o `semantics` é aplicado por último.
 * @param containerColor preenchimento; o padrão é `surfaceVariant`.
 * @param labelColor cor do texto; o padrão é `onSurfaceVariant`.
 * @param borderColor cor da borda; o padrão é `outline`.
 * @param borderWidth espessura da borda; o padrão é `NeoBorders.subtle`.
 */
@Composable
fun NeoInfoChip(
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = NeoBorders.subtle,
    shape: Shape = RoundedCornerShape(NeoRadii.chip),
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        border = BorderStroke(borderWidth, borderColor),
    ) {
        // `clearAndSetSemantics` + `contentDescription` no próprio nó:
        // o Text interno deixa de anunciar por conta própria e o chip
        // inteiro é lido como um rótulo, sem o papel de botão.
        Box(modifier = Modifier.clearAndSetSemantics { contentDescription = label }) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NeoInfoChipPreview() {
    BrainOutTheme {
        NeoInfoChip(label = "Em andamento")
    }
}
