// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Banner persistente de conectividade + fila de sincronização (E3.4),
// compartilhado entre Home e ProjectDetail. Aparece sempre que o
// dispositivo está offline; quando há alterações pendentes, acrescenta
// o indicador "X alterações aguardando sincronização" — visível também
// online enquanto a fila drena.
//
// Persistente: sem ação de dispensa — o banner some sozinho quando a
// conectividade volta (callback de rede, E3.4 item 1) e a fila zera
// (reconciliação, item 3). A testTag é aplicada pelo chamador via
// [modifier], pois cada tela expõe um identificador próprio.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

@Composable
internal fun OfflineBanner(
    showBanner: Boolean,
    pendingOps: Int,
    modifier: Modifier = Modifier,
) {
    if (!showBanner && pendingOps == 0) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (showBanner) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = if (showBanner) {
                    Icons.Outlined.CloudOff
                } else {
                    Icons.Outlined.CloudSync
                },
                contentDescription = null,
                tint = if (showBanner) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (showBanner) {
                    Text(
                        text = stringResource(id = R.string.home_offline_banner_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
                if (pendingOps > 0) {
                    Text(
                        text = stringResource(
                            id = if (pendingOps == 1) {
                                R.string.home_pending_ops_message
                            } else {
                                R.string.home_pending_ops_message_plural
                            },
                            pendingOps,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (showBanner) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                    )
                }
            }
        }
    }
}
