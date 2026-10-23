// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// O diálogo da feature de projetos.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui

import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

/**
 * O diálogo da feature de projetos: título, conteúdo, confirmar e
 * fechar.
 *
 * A estrutura estava repetida em cinco diálogos — o de criar projeto, o
 * de criar tag, o de renomear tarefa, o de mudar a prioridade e o de
 * excluir projeto. Com ela, o botão de fechar deixou de ser copiado
 * (cinco cópias do mesmo `TextButton` com o mesmo rótulo).
 *
 * O que continua sendo de cada diálogo: o título, o conteúdo e o botão
 * de confirmar — são eles que carregam o sentido de cada caso.
 *
 * DEF-46 — o `imePadding` mora aqui, e não em cada diálogo: a janela do
 * Compose 1.12 não redimensiona com o teclado
 * (`DialogProperties.decorFitsSystemWindows` não existe mais), e o
 * padding é o que tira os botões de baixo do teclado. Três dos cinco
 * diálogos já faziam isso na mão; o de excluir projeto não tem campo de
 * texto, e para ele o padding é inerte.
 */
@Composable
internal fun ProjectDialog(
    titleRes: Int,
    onDismiss: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    text: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = titleRes)) },
        text = text,
        confirmButton = confirmButton,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.common_close))
            }
        },
        modifier = Modifier.imePadding().then(modifier),
    )
}
