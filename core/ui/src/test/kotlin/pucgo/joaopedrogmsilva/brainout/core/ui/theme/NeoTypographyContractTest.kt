// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// NB-05: paridade entre a tabela tipográfica de DESIGN.md §4 e o Kotlin.

package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.ui.assets.expectedFontAssets

/**
 * Contrato dos quinze estilos tipográficos (NB-05, item 2).
 *
 * A tabela de `DESIGN.md` §4 é a especificação; `NeoTypography` é a
 * implementação. Este teste é a ponte: família, peso, tamanho, linha e
 * tracking de cada papel Material são declarados aqui como **valores
 * esperados revistos à mão na documentação**, então qualquer troca
 * silenciosa no Kotlin reprova.
 *
 * Também cobre as três regras de acessibilidade do item:
 *  1. nenhum papel Material pode cair no `FontFamily.Default` (a fonte é
 *     empacotada, nunca remota);
 *  2. todos os valores são `sp` — nada em `dp`, que ignoraria a escala de
 *     fonte do usuário (1.0/1.3/2.0, sem clamp);
 *  3. erros, ações e prazos nunca ficam abaixo de 14sp: o único papel
 *     menor é `labelSmall` (12sp), reservado a metadados auxiliares.
 */
class NeoTypographyContractTest {

    @Test
    fun `os quinze estilos batem com a tabela do DESIGN`() {
        contract.forEach { spec ->
            assertThat(spec.style.fontFamily).isSameInstanceAs(spec.family)
            assertThat(spec.style.fontWeight?.weight).isEqualTo(spec.weight)
            assertThat(spec.style.fontSize.value).isWithin(TOLERANCE).of(spec.sizeSp)
            assertThat(spec.style.lineHeight.value).isWithin(TOLERANCE).of(spec.lineSp)
            assertThat(spec.style.letterSpacing.value).isWithin(TOLERANCE).of(spec.trackingSp)
        }
    }

    @Test
    fun `a colecao exposta tem os quinze papeis na ordem do contrato`() {
        assertThat(NeoTypography.styles.keys)
            .containsExactlyElementsIn(contract.map { spec -> spec.role })
            .inOrder()
        assertThat(NeoTypography.styles).hasSize(15)
    }

    @Test
    fun `nenhum estilo declara italico e todos os valores sao sp`() {
        contract.forEach { spec ->
            assertThat(spec.style.fontStyle).isNotEqualTo(FontStyle.Italic)
            assertThat(spec.style.fontSize.isSp).isTrue()
            assertThat(spec.style.lineHeight.isSp).isTrue()
            assertThat(spec.style.letterSpacing.isSp).isTrue()
        }
        assertThat(NeoTypography.styles.values.map { it.fontFamily }.toSet())
            .containsExactly(NeoFonts.Archivo, NeoFonts.PublicSans)
    }

    @Test
    fun `so metadata e texto secundario ficam abaixo de 14sp`() {
        val belowMinimum = NeoTypography.styles
            .filterValues { style -> style.fontSize.value < MIN_READABLE_SP }
            .keys

        assertThat(belowMinimum).containsExactlyElementsIn(allowedBelowMinimum.keys)
        allowedBelowMinimum.forEach { (role, expected) ->
            assertThat(NeoTypography.styles.getValue(role).fontSize.value)
                .isEqualTo(expected)
        }
    }

    @Test
    fun `papeis de erro acao e prazo ficam todos com pelo menos 14sp`() {
        errorActionDeadlineRoles.forEach { role ->
            assertThat(NeoTypography.styles.getValue(role).fontSize.value)
                .isAtLeast(MIN_READABLE_SP)
        }
    }

    @Test
    fun `todo peso usado no contrato existe como arquivo empacotado`() {
        val used = NeoTypography.styles.values
            .mapNotNull { style -> style.fontWeight }
            .map { weight -> weight.weight }
            .toSet()
        val shipped = expectedFontAssets.map { asset -> asset.expectedWeight }.toSet()

        assertThat(used).containsExactlyElementsIn(shipped)
    }
}

private const val TOLERANCE = 0.001f
private const val MIN_READABLE_SP = 14f

/**
 * Únicos papéis abaixo de 14sp, cada um com a justificativa da spec:
 * `labelSmall` é metadado auxiliar (12sp) e `bodySmall` é texto
 * secundário (13sp). Erro, ação e prazo não usam nenhum dos dois.
 */
private val allowedBelowMinimum: Map<String, Float> = linkedMapOf(
    "labelSmall" to 12f,
    "bodySmall" to 13f,
)

/**
 * Papéis que carregam erro, ação ou prazo na UI (supporting text de campo,
 * botão, rótulo de navegação, painel de estado): todos ≥14sp, conforme
 * `DESIGN.md` §4 e `ESPECIFICACAO.md` §1.
 */
private val errorActionDeadlineRoles: List<String> = listOf(
    "labelLarge",
    "labelMedium",
    "bodyMedium",
    "bodyLarge",
)

private data class StyleSpec(
    val role: String,
    val style: TextStyle,
    val family: FontFamily,
    val weight: Int,
    val sizeSp: Float,
    val lineSp: Float,
    val trackingSp: Float,
)

/**
 * Transcrição literal da tabela de `DESIGN.md` §4. Transcrever é
 * deliberado: o teste falha com o valor **declarado na especificação**,
 * não com o que o Kotlin acha que declarou.
 */
private val contract: List<StyleSpec> = listOf(
    StyleSpec("displayLarge", NeoTypography.displayLarge, NeoFonts.Archivo, 800, 40f, 48f, -0.5f),
    StyleSpec("displayMedium", NeoTypography.displayMedium, NeoFonts.Archivo, 800, 36f, 44f, -0.5f),
    StyleSpec("displaySmall", NeoTypography.displaySmall, NeoFonts.Archivo, 800, 32f, 40f, -0.5f),
    StyleSpec("headlineLarge", NeoTypography.headlineLarge, NeoFonts.Archivo, 700, 32f, 40f, 0f),
    StyleSpec("headlineMedium", NeoTypography.headlineMedium, NeoFonts.Archivo, 700, 28f, 36f, 0f),
    StyleSpec("headlineSmall", NeoTypography.headlineSmall, NeoFonts.Archivo, 700, 24f, 32f, 0f),
    StyleSpec("titleLarge", NeoTypography.titleLarge, NeoFonts.Archivo, 700, 22f, 28f, 0f),
    StyleSpec("titleMedium", NeoTypography.titleMedium, NeoFonts.Archivo, 600, 18f, 26f, 0f),
    StyleSpec("titleSmall", NeoTypography.titleSmall, NeoFonts.Archivo, 600, 16f, 24f, 0f),
    StyleSpec("bodyLarge", NeoTypography.bodyLarge, NeoFonts.PublicSans, 400, 16f, 24f, 0f),
    StyleSpec("bodyMedium", NeoTypography.bodyMedium, NeoFonts.PublicSans, 400, 14f, 22f, 0f),
    StyleSpec("bodySmall", NeoTypography.bodySmall, NeoFonts.PublicSans, 400, 13f, 20f, 0f),
    StyleSpec("labelLarge", NeoTypography.labelLarge, NeoFonts.PublicSans, 600, 15f, 20f, 0.1f),
    StyleSpec("labelMedium", NeoTypography.labelMedium, NeoFonts.PublicSans, 600, 14f, 20f, 0.1f),
    StyleSpec("labelSmall", NeoTypography.labelSmall, NeoFonts.PublicSans, 600, 12f, 16f, 0.1f),
)
