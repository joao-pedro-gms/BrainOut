// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.material3.Typography

/**
 * Os quinze papéis do Material 3 preenchidos com o contrato tipográfico
 * Neo (`DESIGN.md` §4, NB-05).
 *
 * É este objeto — e não [NeoTypography] diretamente — que entra no
 * `MaterialTheme`: [NeoTypography] é a fonte da verdade, com um
 * `TextStyle` por papel e em ordem; aqui eles viram os quinze slots
 * nomeados que o Material 3 lê. Os quinze slots são preenchidos de
 * propósito: deixar qualquer um no default faria sobrar tipografia
 * Material (Roboto, 57sp) em algum canto da UI.
 *
 * Efeito prático: as cinco fontes empacotadas em `res/font` passam a ser
 * carregadas de verdade na primeira abertura, offline, porque [NeoFonts]
 * referencia `R.font.*` em vez de `FontFamily.Default`. Os pesos
 * 400/600/700/800 são glifos reais nos arquivos — nada de negrito
 * sintético (ver [NeoTypography] para o porquê do arquivo estático).
 */
val NeoMaterialTypography: Typography =
    Typography(
        displayLarge = NeoTypography.displayLarge,
        displayMedium = NeoTypography.displayMedium,
        displaySmall = NeoTypography.displaySmall,
        headlineLarge = NeoTypography.headlineLarge,
        headlineMedium = NeoTypography.headlineMedium,
        headlineSmall = NeoTypography.headlineSmall,
        titleLarge = NeoTypography.titleLarge,
        titleMedium = NeoTypography.titleMedium,
        titleSmall = NeoTypography.titleSmall,
        bodyLarge = NeoTypography.bodyLarge,
        bodyMedium = NeoTypography.bodyMedium,
        bodySmall = NeoTypography.bodySmall,
        labelLarge = NeoTypography.labelLarge,
        labelMedium = NeoTypography.labelMedium,
        labelSmall = NeoTypography.labelSmall,
    )
