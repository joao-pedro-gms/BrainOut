// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.LocalNeoWindowInfo
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoBorders
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSpacing
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.neoPageGutter
import pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel.AuthErrorType

// Peças compartilhadas pelos formulários de Login e Cadastro.
//
// As duas telas tinham o mesmo esqueleto escrito duas vezes — coluna
// rolável, subtítulo, banner de erro, botão de envio com indicador de
// carregamento, botão secundário e o foco no primeiro campo inválido.
// Estas funções são a implementação única; cada tela passa só os
// textos e as tags que diferem.

/**
 * O esqueleto da tela: `Scaffold` com a barra superior e o fundo do tema.
 *
 * Login e Cadastro tinham o mesmo `Scaffold` escrito duas vezes — o que
 * muda entre as duas telas é só o título da barra.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AuthScreenScaffold(
    titleRes: Int,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = titleRes),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        content = content,
    )
}

/**
 * Esqueleto comum dos formulários.
 *
 * [Modifier.imePadding] (DEF-27) vem **antes** do `verticalScroll` para que
 * a altura do teclado entre nas restrições da área rolável: em edge-to-edge
 * o IME não redimensiona a janela, então sem isso os últimos campos e o
 * botão de envio ficariam atrás do teclado em paisagem ou em tela baixa.
 */
@Composable
internal fun AuthFormScaffold(
    scrollTestTag: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .imePadding()
                .verticalScroll(rememberScrollState())
                .neoPageGutter(margin = LocalNeoWindowInfo.current.margin, form = true)
                .testTag(scrollTestTag),
        verticalArrangement = Arrangement.spacedBy(NeoSpacing.md),
        content = content,
    )
}

/** Subtítulo de apresentação do formulário. */
@Composable
internal fun AuthSubtitle(textRes: Int) {
    Text(
        text = stringResource(id = textRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Banner de erro do formulário; nada é desenhado quando não há erro. */
@Composable
internal fun AuthErrorBanner(
    errorType: AuthErrorType?,
    testTag: String,
) {
    errorType?.let { type ->
        Text(
            text = type.text(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(testTag),
        )
    }
}

/**
 * Botão de envio do formulário.
 *
 * Durante o envio, o rótulo é substituído pelo indicador de progresso
 * (o botão fica desabilitado).
 */
@Composable
internal fun AuthSubmitButton(
    labelRes: Int,
    isLoading: Boolean,
    testTag: String,
    loadingTestTag: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(testTag),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier =
                    Modifier
                        .height(NeoSpacing.mdPlus)
                        .testTag(loadingTestTag),
                strokeWidth = NeoBorders.default,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(
                text = stringResource(id = labelRes),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/** Botão secundário do formulário ("criar conta" / "já tenho conta"). */
@Composable
internal fun AuthSecondaryButton(
    labelRes: Int,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(testTag),
    ) {
        Text(
            text = stringResource(id = labelRes),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/**
 * Devolve o foco ao primeiro campo inválido, como exige o roadmap E1.6.
 *
 * `FocusRequester.requestFocus()` lança `IllegalStateException` quando o
 * requester não está ligado a um campo; o `runCatching` cobre esse caso.
 * Antes o `when` repetia esse `runCatching` em cada tela.
 *
 * @param errors erros dos campos, **na ordem** em que os campos aparecem.
 * @param requesters requesters dos mesmos campos, na mesma ordem.
 */
@Composable
internal fun FocusFirstInvalidField(
    errors: List<AuthErrorType?>,
    requesters: List<FocusRequester>,
) {
    LaunchedEffect(errors) {
        val index = errors.indexOfFirst { it != null }
        if (index >= 0 && index < requesters.size) {
            runCatching { requesters[index].requestFocus() }
        }
    }
}
