// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// O cartão de linhas de configuração.

package pucgo.joaopedrogmsilva.brainout.feature.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoRadii

/**
 * O cartão que agrupa as linhas de configuração.
 *
 * A receita (superfície elevada, tom da paleta, raio do card) estava
 * escrita duas vezes: no cartão do backup e no cartão das seções de
 * Configurações. Aqui ela mora uma vez.
 *
 * Fica no módulo, e não no `:core:ui`, porque é um cartão **de
 * Configurações** — não a receita de cartão do design system (que é o
 * `NeoCard`). A elevação tonal é a que o app usa hoje nestas duas
 * superfícies; a questão de unificar a elevação com a sombra rígida da
 * identidade está registrada como **DEF-76**.
 */
@Composable
internal fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(NeoRadii.card),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}
