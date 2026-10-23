// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Cartão de métrica do Dashboard — contagem de projetos e taxa de
// conclusão usam o mesmo componente.

package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoRadii

/**
 * Cartão de uma métrica do painel: um valor em destaque e um rótulo.
 *
 * O contêiner de projetos (contagem) e o de taxa de conclusão
 * (percentual) tinham dois componentes quase idênticos — mesmo `Surface`,
 * mesmo `Column`, mesma hierarquia de texto — que diferiam só no valor
 * exibido, na altura mínima e no tamanho da tipografia do valor. Agora há
 * uma implementação só.
 *
 * @param value valor em destaque (contagem ou percentual formatado).
 * @param caption rótulo abaixo do valor.
 * @param containerColor fundo do cartão.
 * @param contentColor cor do texto.
 * @param testTag tag de teste.
 * @param minHeight altura mínima do cartão.
 * @param valueStyle tipografia do valor em destaque.
 */
@Composable
internal fun MetricCard(
    value: String,
    caption: String,
    containerColor: Color,
    contentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    minHeight: Dp = 72.dp,
    valueStyle: TextStyle = MaterialTheme.typography.headlineMedium,
) {
    Surface(
        modifier =
            modifier
                .testTag(testTag)
                .heightIn(min = minHeight),
        shape = RoundedCornerShape(NeoRadii.card),
        color = containerColor,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = value,
                style = valueStyle,
                color = contentColor,
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}
