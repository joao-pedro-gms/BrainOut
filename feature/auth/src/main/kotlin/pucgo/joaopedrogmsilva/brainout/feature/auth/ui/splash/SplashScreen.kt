// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Tela Splash do BrainOut — marco E1.3.
// Apresentação inicial com identidade do app e CTA "Começar" que leva à tela
// de login. Não há animação automatizada; a navegação é manual via botão.

package pucgo.joaopedrogmsilva.brainout.feature.auth.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSpacing
import pucgo.joaopedrogmsilva.brainout.feature.auth.R
import pucgo.joaopedrogmsilva.brainout.core.ui.R as UiR

/**
 * Tela inicial do app.
 *
 * @param onStartClicked disparado quando o usuário toca em "Começar".
 *  Deve navegar para a rota `login` no `NavHost` (configurado em `:app`).
 */
@Composable
fun SplashScreen(
    onStartClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = NeoSpacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NeoSpacing.mdPlus),
        ) {
            // A marca do app: a mesma arte que é o ícone do launcher
            // (`brand/brainout-icon.svg`), aqui sobre o fundo do tema.
            Image(
                painter = painterResource(UiR.drawable.neo_art_app_logo),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(160.dp)
                        .testTag(SplashScreenTestTags.LOGO),
            )
            Text(
                text = stringResource(id = R.string.splash_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(id = R.string.splash_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onStartClicked,
                modifier = Modifier.padding(top = NeoSpacing.xs),
            ) {
                Text(
                    text = stringResource(id = R.string.splash_cta),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/**
 * Tags de teste da tela de abertura.
 *
 * `LOGO` marca a marca do app — o mesmo glifo que é o ícone do launcher.
 */
object SplashScreenTestTags {
    const val LOGO: String = "splash_logo"
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SplashScreenPreview() {
    BrainOutTheme {
        SplashScreen(onStartClicked = {})
    }
}
