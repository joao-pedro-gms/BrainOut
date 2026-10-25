// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Campo de texto da identidade Neo.
 *
 * A receita — `OutlinedTextField` com rótulo, placeholder, estado de
 * erro e transformação visual — estava escrita **quatro** vezes: no
 * campo de nome e no de e-mail do cadastro, no de e-mail e no de senha
 * do login (`:feature:auth`) e no de senha do backup
 * (`:feature:settings`). O `:core:ui` é o dono das receitas visuais,
 * então ela mora aqui uma vez.
 *
 * Os textos chegam **já resolvidos** (`String`), porque o `:core:ui`
 * não tem string de interface — o mesmo padrão do [NeoEmptyState] e do
 * [NeoErrorBanner]. O `modifier` também fica com o chamador: cada
 * campo tem seu `testTag` e seu `FocusRequester`.
 *
 * @param value texto atual do campo.
 * @param onValueChange callback de digitação.
 * @param label rótulo do campo.
 * @param modifier modificador do campo.
 * @param placeholder texto de apoio quando o campo está vazio.
 * @param supportingText texto de apoio abaixo do campo (o erro).
 * @param isError se o campo está em estado de erro.
 * @param singleLine se o campo é de uma linha só.
 * @param visualTransformation transformação do texto (ex.: senha).
 * @param keyboardOptions opções do teclado.
 * @param keyboardActions ações do teclado (IME).
 * @param trailingIcon ícone à direita do campo.
 */
@Composable
fun NeoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(text = label) },
        placeholder = placeholder?.let { text -> { Text(text = text) } },
        singleLine = singleLine,
        isError = isError,
        supportingText = supportingText?.let { text -> { Text(text = text) } },
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        trailingIcon = trailingIcon,
    )
}
