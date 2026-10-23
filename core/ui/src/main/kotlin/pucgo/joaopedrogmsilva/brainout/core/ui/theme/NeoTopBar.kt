// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// A receita de cor da barra superior Neo, em um lugar só.

package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * As cores da barra superior Neo.
 *
 * A receita estava copiada em quatro telas — Home, Detalhe de projeto,
 * Lista de tarefas e Configurações — com os mesmos valores literais.
 * Centralizar aqui faz a barra acompanhar o tema em um lugar só, em vez
 * de quatro.
 *
 * Só o que é comum a todas as barras vem daqui: o fundo da barra e a cor
 * do título. A cor dos ícones é parâmetro, porque nem toda barra define
 * os mesmos ícones — e quem não define fica com o padrão do tema
 * ([Color.Unspecified] faz o Material 3 herdar o valor anterior), que é o
 * que as telas já exibiam.
 */
@Composable
fun neoTopAppBarColors(
    navigationIconContentColor: Color = Color.Unspecified,
    actionIconContentColor: Color = Color.Unspecified,
): TopAppBarColors =
    TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.background,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
        navigationIconContentColor = navigationIconContentColor,
        actionIconContentColor = actionIconContentColor,
    )
