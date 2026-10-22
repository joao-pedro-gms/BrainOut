// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoBorders
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoRadii
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSpacing

/**
 * Cartão de estado vazio da identidade Neo.
 *
 * Substitui as cinco cópias que existiam em `:feature:projects` (Home —
 * lista vazia, lista de concluídos vazia e busca sem resultados) e
 * `:feature:tasks` (Tarefas e Painel). O esqueleto era idêntico; só
 * mudavam os textos e, em dois casos, o alinhamento do corpo.
 *
 * DEF-22 — o cartão ganha **fronteira** em vez de `alpha`.
 * `surfaceVariant.copy(alpha = 0.4f)` dava 1.06:1 no claro e 1.11:1 no
 * escuro: uma diferença invisível na tela. Tirar o alpha sozinho não
 * resolve — `surfaceVariant` puro marca 1.15:1 / 1.33:1, porque o papel
 * é uma *superfície alternativa*, quase da mesma luminância do fundo por
 * desenho. O que separa a região do fundo é a **fronteira**: `outline`
 * puro a 16.10:1 / 15.95:1. O texto interno (`onSurfaceVariant`) fica em
 * 6.56:1 / 9.26:1, acima de 4.5:1.
 *
 * @param title texto de título, já localizado.
 * @param body texto de apoio, já localizado.
 * @param modifier modificador do cartão.
 * @param bodyTextAlign alinhamento do corpo; `Start` é o padrão herdado
 *  das telas de lista, `Center` o do cartão dePainel.
 */
@Composable
fun NeoEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    bodyTextAlign: TextAlign = TextAlign.Start,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(NeoRadii.modal),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(NeoBorders.default, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(NeoSpacing.lg),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(NeoSpacing.xs))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = bodyTextAlign,
            )
        }
    }
}
