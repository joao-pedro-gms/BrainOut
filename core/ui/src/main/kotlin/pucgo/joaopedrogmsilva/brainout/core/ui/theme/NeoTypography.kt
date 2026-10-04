// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// NB-05: famílias locais e contrato tipográfico (DESIGN.md §4).

package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import pucgo.joaopedrogmsilva.brainout.core.ui.R

/**
 * Famílias tipográficas empacotadas no APK.
 *
 * Archivo cobre títulos e controles enfáticos; Public Sans cobre leitura.
 * Ambas são SIL OFL 1.1, licença em `core/ui/licenses/`, e os binários são
 * **instâncias estáticas** derivadas das variantes variáveis do
 * `google/fonts` — ver `docs/design/ASSETS.md` para origem, ferramenta,
 * versão e checksums.
 *
 * Por que estático: `minSdk 24` inclui API 24/25, onde suporte a fonte
 * variável não existe. Um `FontFamily` cujo único arquivo fosse
 * `Archivo[wdth,wght].ttf` cairia no fallback nessas versões. Cada `Font`
 * declara o seu peso, é o que o `minikin` usa para casar dentro da família
 * — sem negrito sintético: os pesos 400/600/700/800 existem como glifos
 * reais nos arquivos.
 *
 * Glifos ausentes caem no fallback do sistema (`FontFamily.Default` é a
 * rede de segurança do Android, não uma família declarada aqui).
 */
object NeoFonts {

    /** Pesos reais empacotados: 600 (fichas), 700 (títulos), 800 (display). */
    val Archivo: FontFamily = FontFamily(
        Font(R.font.archivo_semi_bold, weight = FontWeight.W600),
        Font(R.font.archivo_bold, weight = FontWeight.W700),
        Font(R.font.archivo_extra_bold, weight = FontWeight.W800),
    )

    /** Pesos reais empacotados: 400 (corpo), 600 (rótulos e metadados). */
    val PublicSans: FontFamily = FontFamily(
        Font(R.font.public_sans_regular, weight = FontWeight.W400),
        Font(R.font.public_sans_semi_bold, weight = FontWeight.W600),
    )
}

/**
 * Os quinze estilos do contrato `DESIGN.md` §4 — todos os papéis Material
 * definidos de propósito, para não sobrar fonte/tamanho Material residual
 * em nenhum canto da UI.
 *
 * Regras que este objeto materializa:
 *
 *  1. **Nada de fonte remota.** Toda família vem de [NeoFonts], que só
 *     referencia `R.font.*` locais — primeira abertura 100% offline.
 *  2. **Escala sem clamp.** Tamanhos, linhas e tracking são `sp`, então
 *     seguem a escala do usuário (1.0/1.3/2.0) sem limite artificial.
 *     Quem limitar `fontScale` para «consertar» layout quebra esta regra.
 *  3. **Metadados 12sp, leitura ≥14sp.** `labelSmall` (12/16) é metadado
 *     auxiliar e `bodySmall` (13/20) é texto secundário: são os únicos
 *     papéis abaixo de 14sp. Erros, ações e prazos nunca podem usá-los —
 *     usam `labelMedium` (14/20), `labelLarge` (15/20), `bodyMedium`
 *     (14/22) ou maiores.
 *  4. **Uso previsto:** `titleMedium` nas fichas, `bodyLarge` nos inputs,
 *     `labelMedium` em chips, `titleLarge`/`headlineMedium` em títulos de
 *     tela. Truncamento de título a duas linhas só onde o texto completo
 *     está acessível no detalhe.
 *
 * Ambas as famílias exportam o recurso `tnum` (dígitos tabulares),
 * verificado em `docs/design/ASSETS.md` — o Painel usa
 * `fontFeatureSettings = "tnum"` sem depender de alinhamento por acaso.
 */
object NeoTypography {

    val displayLarge: TextStyle = TextStyle(
        fontFamily = NeoFonts.Archivo,
        fontWeight = FontWeight.W800,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp,
    )

    val displayMedium: TextStyle = TextStyle(
        fontFamily = NeoFonts.Archivo,
        fontWeight = FontWeight.W800,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp,
    )

    val displaySmall: TextStyle = TextStyle(
        fontFamily = NeoFonts.Archivo,
        fontWeight = FontWeight.W800,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
    )

    val headlineLarge: TextStyle = TextStyle(
        fontFamily = NeoFonts.Archivo,
        fontWeight = FontWeight.W700,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    )

    val headlineMedium: TextStyle = TextStyle(
        fontFamily = NeoFonts.Archivo,
        fontWeight = FontWeight.W700,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    )

    val headlineSmall: TextStyle = TextStyle(
        fontFamily = NeoFonts.Archivo,
        fontWeight = FontWeight.W700,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    )

    val titleLarge: TextStyle = TextStyle(
        fontFamily = NeoFonts.Archivo,
        fontWeight = FontWeight.W700,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    )

    val titleMedium: TextStyle = TextStyle(
        fontFamily = NeoFonts.Archivo,
        fontWeight = FontWeight.W600,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
    )

    val titleSmall: TextStyle = TextStyle(
        fontFamily = NeoFonts.Archivo,
        fontWeight = FontWeight.W600,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    )

    val bodyLarge: TextStyle = TextStyle(
        fontFamily = NeoFonts.PublicSans,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    )

    val bodyMedium: TextStyle = TextStyle(
        fontFamily = NeoFonts.PublicSans,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    )

    val bodySmall: TextStyle = TextStyle(
        fontFamily = NeoFonts.PublicSans,
        fontWeight = FontWeight.W400,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    )

    val labelLarge: TextStyle = TextStyle(
        fontFamily = NeoFonts.PublicSans,
        fontWeight = FontWeight.W600,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    )

    val labelMedium: TextStyle = TextStyle(
        fontFamily = NeoFonts.PublicSans,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    )

    val labelSmall: TextStyle = TextStyle(
        fontFamily = NeoFonts.PublicSans,
        fontWeight = FontWeight.W600,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp,
    )

    /**
     * Os quinze estilos na ordem do contrato, para o teste de paridade e
     * para a galeria de componentes consultar por papel Material.
     */
    val styles: Map<String, TextStyle> = linkedMapOf(
        "displayLarge" to displayLarge,
        "displayMedium" to displayMedium,
        "displaySmall" to displaySmall,
        "headlineLarge" to headlineLarge,
        "headlineMedium" to headlineMedium,
        "headlineSmall" to headlineSmall,
        "titleLarge" to titleLarge,
        "titleMedium" to titleMedium,
        "titleSmall" to titleSmall,
        "bodyLarge" to bodyLarge,
        "bodyMedium" to bodyMedium,
        "bodySmall" to bodySmall,
        "labelLarge" to labelLarge,
        "labelMedium" to labelMedium,
        "labelSmall" to labelSmall,
    )
}
