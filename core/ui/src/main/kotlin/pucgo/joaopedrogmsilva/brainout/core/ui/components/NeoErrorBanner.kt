// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.ui.R
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoRadii
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSizes
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSpacing

/**
 * Banner de erro da identidade Neo.
 *
 * Substitui as quatro cópias que existiam em `:feature:projects` (Home e
 * Detalhe de Projeto) e `:feature:tasks` (Tarefas e Painel) — o esqueleto
 * era idêntico e só mudavam as tags de teste e o texto das ações.
 *
 * A **mensagem já vem resolvida** pelo chamador: cada módulo traduz a
 * chave canônica do seu ViewModel para o texto localizado, e entrega o
 * texto final. O componente não conhece chaves de ViewModel — ele sabe
 * desenhar.
 *
 * @param message texto do erro, já localizado.
 * @param onRetry ação "Tentar novamente".
 * @param onDismiss ação "Dispensar".
 * @param modifier modificador do cartão.
 * @param minHeight altura mínima do cartão; `0.dp` remove a reserva.
 * @param messageTextAlign alinhamento da mensagem; `Start` é o padrão
 *  herdado das telas de lista, `Center` o do cartão do Painel.
 */
@Composable
fun NeoErrorBanner(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 140.dp,
    messageTextAlign: TextAlign = TextAlign.Start,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .testTag(NeoErrorBannerTestTags.BANNER),
        shape = RoundedCornerShape(NeoRadii.modal),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(NeoSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(NeoSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = messageTextAlign,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(NeoSpacing.sm)) {
                TextButton(
                    onClick = onRetry,
                    modifier =
                        Modifier
                            .testTag(NeoErrorBannerTestTags.RETRY)
                            // E4.4: 48dp mínimo WCAG 2.5.5.
                            .heightIn(min = NeoSizes.touchMin),
                ) {
                    Text(text = stringResource(id = R.string.neo_error_retry))
                }
                TextButton(
                    onClick = onDismiss,
                    modifier =
                        Modifier
                            .testTag(NeoErrorBannerTestTags.DISMISS)
                            .heightIn(min = NeoSizes.touchMin),
                ) {
                    Text(text = stringResource(id = R.string.neo_error_dismiss))
                }
            }
        }
    }
}

/** Identificadores usados por testes Compose do [NeoErrorBanner]. */
object NeoErrorBannerTestTags {
    const val BANNER: String = "neo_error_banner"
    const val RETRY: String = "neo_error_retry"
    const val DISMISS: String = "neo_error_dismiss"
}
