// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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

/**
 * Tipografia da identidade **legada** (E1.3): Material 3 com a fonte do
 * sistema, 57sp em `displayLarge` e nenhum `fontFamily` empacotado.
 *
 * Não é mais o que o app renderiza — [NeoMaterialTypography] é, desde a
 * ativação do tema Neo. O objeto permanece porque ainda faz parte do
 * contrato assinado por `ThemeSelectionTest`; removê-lo, junto com a
 * revisão desse teste, é tarefa declarada do NB-32 em
 * `docs/plans/2026-10-01-redesign-neobrutalista.md` — não uma limpeza
 * incidental de tema.
 */
val BrainOutTypography: Typography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 57.sp,
                lineHeight = 64.sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 32.sp,
                lineHeight = 40.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
    )
