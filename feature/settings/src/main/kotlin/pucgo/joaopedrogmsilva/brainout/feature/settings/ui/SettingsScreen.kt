// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Tela de Configurações — marco E1.3 (esqueleto navegável).

package pucgo.joaopedrogmsilva.brainout.feature.settings.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.LocalNeoWindowInfo
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSpacing
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.neoContentWidth
import pucgo.joaopedrogmsilva.brainout.feature.settings.R
import pucgo.joaopedrogmsilva.brainout.core.ui.R as UiR

/** Opacidade do conteudo de uma linha desabilitada. */
private const val DISABLED_ALPHA: Float = 0.38f

/**
 * Tela de configurações.
 *
 * @param onOptionClicked disparado quando o usuário toca em uma linha.
 *  Recebe o [SettingsActionType] correspondente. O `NavHost` decide o que
 *  fazer — no E1.3 apenas `SignOut` tem efeito (limpar backstack e voltar
 *  para `login`); os demais ficam reservados para E2.x.
 * @param backupContent área reservada à seção de backup local
 *  (issue #214 — LOCAL-03). Fica como *slot* para que a tela continue
 *  sem depender do ViewModel de backup — a própria [BackupSection]
 *  resolve o seu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOptionClicked: (SettingsActionType) -> Unit,
    modifier: Modifier = Modifier,
    backupContent: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .neoContentWidth()
                    .padding(horizontal = LocalNeoWindowInfo.current.margin, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsIdentityHeader()

            SettingsSections.forEach { section ->
                Text(
                    text = stringResource(id = section.titleRes).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        section.options.forEachIndexed { index, option ->
                            SettingsRow(
                                option = option,
                                onClick = { onOptionClicked(option.type) },
                            )
                            if (index < section.options.lastIndex) {
                                // DEF-22 — divisor com o token cheio:
                                // `outline` puro marca 16.10:1 / 15.95:1;
                                // com `alpha = 0.3f` a separação entre
                                // linhas caía para 1.93:1 / 2.59:1, abaixo
                                // do mínimo de 3:1 (WCAG §1.4.11).
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }
                }
            }

            backupContent()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Cabeçalho de identidade da tela: a mesma marca que é o ícone do launcher
 * (`brand/brainout-icon.svg`), com o nome do app e o subtítulo.
 *
 * A figura é decorativa — o nome do app já está em texto ao lado, então a
 * imagem não repete informação para o TalkBack.
 */
@Composable
private fun SettingsIdentityHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NeoSpacing.md),
    ) {
        Image(
            painter = painterResource(UiR.drawable.neo_art_app_logo),
            contentDescription = null,
            modifier = Modifier.size(96.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(NeoSpacing.xxs)) {
            Text(
                text = stringResource(id = R.string.settings_app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(id = R.string.settings_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun SettingsRow(
    option: SettingsOption,
    onClick: () -> Unit,
    testTag: String = "settings_option_${option.type.name.lowercase()}",
    enabled: Boolean = true,
) {
    val label = stringResource(id = option.labelRes)
    val isSignOut = option.type == SettingsActionType.SignOut
    val labelColor =
        when {
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DISABLED_ALPHA)
            isSignOut -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurface
        }
    val iconColor =
        when {
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DISABLED_ALPHA)
            isSignOut -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = enabled, onClick = onClick)
                .testTag(testTag)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = labelColor,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isSignOut) {
                Icon(
                    painter = painterResource(UiR.drawable.neo_ic_logout),
                    contentDescription = null,
                    tint = iconColor,
                )
            } else {
                Icon(
                    painter = painterResource(UiR.drawable.neo_ic_chevron_right),
                    contentDescription = null,
                    tint = iconColor,
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SettingsScreenPreview() {
    pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme {
        SettingsScreen(onOptionClicked = {})
    }
}
