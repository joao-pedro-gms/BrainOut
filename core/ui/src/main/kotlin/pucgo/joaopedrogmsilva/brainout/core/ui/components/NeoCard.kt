// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// O cartão Neo: a superfície elevada que a identidade usa em listas e no painel.

package pucgo.joaopedrogmsilva.brainout.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoRadii
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSpacing

/**
 * O cartão Neo.
 *
 * A receita estava copiada em três telas — o cartão de estados do Painel,
 * o gráfico de prioridades e o cartão de tarefa da Lista — com os mesmos
 * literais de cor, elevação, raio e respiro. Centralizar aqui faz o
 * cartão acompanhar a identidade em um lugar só, em vez de três.
 *
 * O raio vem de [NeoRadii.card] (8 dp), que é o que a identidade
 * especifica para cards; o `NeoCardRadiusTest` impede que uma tela volte
 * a escrever o raio por conta própria.
 */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(NeoSpacing.md),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(NeoSpacing.xs),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(NeoRadii.card),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
            verticalArrangement = verticalArrangement,
            content = content,
        )
    }
}
