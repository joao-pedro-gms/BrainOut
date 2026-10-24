// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// NB-04: paridade entre a especificação (tokens.json) e a implementação Kotlin.

package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Paridade `tokens.json` × Kotlin.
 *
 * O JSON é a especificação; o Kotlin é a implementação. Este teste é a
 * ponte: lê o arquivo (nunca em runtime do app), resolve cada papel pelo
 * **nome** e compara com a cor que o Kotlin entrega. Se alguém trocar um
 * hex no Kotlin "para ficar melhor", o teste reprova — a decisão de cor
 * mora no documento, não no código.
 *
 * Nada aqui replica constantes: os valores esperados vêm do arquivo.
 * Cada grupo é lido por inteiro e comparado com o conjunto de tokens do
 * Kotlin — um token novo no contrato sem o par correspondente (e vice-versa)
 * reprova, e o nome do token no código vira a chave da comparação.
 */
class NeoTokenParityTest {
    private val json: String = readTokensJson()

    private fun hexToColor(hex: String): Color {
        val value = hex.removePrefix("#").toLong(16)
        return Color(0xFF000000L or value)
    }

    /**
     * Compara um grupo numérico do JSON com os tokens Kotlin, pelo nome.
     *
     * Os dois lados precisam ter exatamente os mesmos nomes: token novo
     * no Kotlin sem par no contrato (e vice-versa) reprova.
     */
    private fun assertDpParity(
        declared: Map<String, Int>,
        implemented: Map<String, Dp>,
    ) {
        assertThat(implemented.keys).containsExactlyElementsIn(declared.keys)
        declared.forEach { (name, value) ->
            assertThat(implemented.getValue(name)).isEqualTo(value.dp)
        }
    }

    @Test
    fun `todo primitivo do JSON existe no Kotlin com o mesmo valor`() {
        val primitives = primitivesFrom(json)
        assertThat(primitives).hasSize(37)
        primitives.forEach { (name, hex) ->
            assertThat(neoPrimitive(name)).isEqualTo(hexToColor(hex))
        }
    }

    @Test
    fun `todo papel do tema claro resolve para o primitivo declarado`() {
        val roles = themeFrom(json, "light")
        assertThat(roles).hasSize(39)
        roles.forEach { (role, primitiveName) ->
            assertThat(neoRole(NeoColors.Light, role)).isEqualTo(neoPrimitive(primitiveName))
        }
    }

    @Test
    fun `todo papel do tema escuro resolve para o primitivo declarado`() {
        val roles = themeFrom(json, "dark")
        assertThat(roles).hasSize(39)
        roles.forEach { (role, primitiveName) ->
            assertThat(neoRole(NeoColors.Dark, role)).isEqualTo(neoPrimitive(primitiveName))
        }
    }

    @Test
    fun `os dois temas declaram exatamente os mesmos papeis`() {
        assertThat(themeFrom(json, "light").keys).isEqualTo(themeFrom(json, "dark").keys)
    }

    @Test
    fun `escala de espacamento e identica a do JSON`() {
        val declared =
            json
                .substringAfter("\"spacing\"")
                .substringBefore("]")
                .let { Regex("\\d+").findAll(it).map { m -> m.value.toInt() }.toList() }
        val implemented = NeoSpacing.scale.map { it.value.toInt() }

        assertThat(implemented).isEqualTo(declared)
        assertThat(implemented).isEqualTo(listOf(0, 4, 8, 12, 16, 20, 24, 32, 40, 48, 64))
    }

    @Test
    fun `raios declarados batem com os tokens`() {
        val declared = numericGroupFrom(json, "radii")
        assertThat(declared).hasSize(7)
        val implemented =
            mapOf(
                "none" to NeoRadii.none,
                "chip" to NeoRadii.chip,
                "field" to NeoRadii.field,
                "button" to NeoRadii.button,
                "card" to NeoRadii.card,
                "menu" to NeoRadii.menu,
                "modal" to NeoRadii.modal,
            )
        assertDpParity(declared, implemented)
    }

    @Test
    fun `bordas declaradas batem com os tokens`() {
        val declared = numericGroupFrom(json, "borders")
        assertThat(declared).hasSize(4)
        val implemented =
            mapOf(
                "subtle" to NeoBorders.subtle,
                "default" to NeoBorders.default,
                "focus" to NeoBorders.focus,
                "focusGap" to NeoBorders.focusGap,
            )
        assertDpParity(declared, implemented)
    }

    @Test
    fun `tamanhos declarados batem com os tokens`() {
        val declared = numericGroupFrom(json, "sizes")
        assertThat(declared).hasSize(11)
        val implemented =
            mapOf(
                "touchMin" to NeoSizes.touchMin,
                "touchGap" to NeoSizes.touchGap,
                "buttonMinHeight" to NeoSizes.buttonMinHeight,
                "fieldMinHeight" to NeoSizes.fieldMinHeight,
                "fabMinHeight" to NeoSizes.fabMinHeight,
                "rowMinHeight" to NeoSizes.rowMinHeight,
                "icon" to NeoSizes.icon,
                "iconMeta" to NeoSizes.iconMeta,
                "iconEmpty" to NeoSizes.iconEmpty,
                "badgeMinHeight" to NeoSizes.badgeMinHeight,
                "navMinHeight" to NeoSizes.navMinHeight,
            )
        assertDpParity(declared, implemented)
    }

    @Test
    fun `tamanhos minimos de controle nao ficam abaixo de 48dp`() {
        val sizes =
            listOf(
                NeoSizes.touchMin,
                NeoSizes.buttonMinHeight,
                NeoSizes.fieldMinHeight,
                NeoSizes.fabMinHeight,
                NeoSizes.rowMinHeight,
            )
        sizes.forEach { assertThat(it.value).isAtLeast(48f) }
    }

    @Test
    fun `breakpoints e margens batem com o JSON`() {
        val declared = numericGroupFrom(json, "layout")
        assertThat(declared).hasSize(9)
        val implemented =
            mapOf(
                "mediumMinWidth" to NeoLayout.mediumMinWidth,
                "expandedMinWidth" to NeoLayout.expandedMinWidth,
                "largeMinWidth" to NeoLayout.largeMinWidth,
                "compactHeightMaxExclusive" to NeoLayout.compactHeightMaxExclusive,
                "compactMargin" to NeoLayout.compactMargin,
                "mediumMargin" to NeoLayout.mediumMargin,
                "expandedMargin" to NeoLayout.expandedMargin,
                "formMaxWidth" to NeoLayout.formMaxWidth,
                "contentMaxWidth" to NeoLayout.contentMaxWidth,
            )
        assertDpParity(declared, implemented)
    }

    @Test
    fun `os slots de Shapes batem com o JSON`() {
        val declared = numericGroupFrom(json, "materialShapes")
        assertThat(declared).hasSize(5)
        val implemented =
            mapOf(
                "extraSmall" to NeoShapes.extraSmall,
                "small" to NeoShapes.small,
                "medium" to NeoShapes.medium,
                "large" to NeoShapes.large,
                "extraLarge" to NeoShapes.extraLarge,
            )
        assertThat(implemented.keys).containsExactlyElementsIn(declared.keys)
        declared.forEach { (name, value) ->
            assertThat(implemented.getValue(name)).isEqualTo(RoundedCornerShape(value.dp))
        }
    }

    @Test
    fun `sombra rigida nao tem blur nem spread no JSON nem no Kotlin`() {
        val declared = objectGroupFrom(json, "shadows")
        assertThat(declared).hasSize(4)
        val implemented =
            mapOf(
                "none" to NeoShadows.none,
                "small" to NeoShadows.small,
                "medium" to NeoShadows.medium,
                "display" to NeoShadows.display,
            )
        assertThat(implemented.keys).containsExactlyElementsIn(declared.keys)
        declared.forEach { (name, fields) ->
            val shadow = implemented.getValue(name)
            assertThat(shadow.x).isEqualTo(fields.getValue("x").dp)
            assertThat(shadow.y).isEqualTo(fields.getValue("y").dp)
            assertThat(shadow.blur).isEqualTo(fields.getValue("blur").dp)
            assertThat(shadow.spread).isEqualTo(fields.getValue("spread").dp)
            assertThat(shadow.blur).isEqualTo(0.dp)
            assertThat(shadow.spread).isEqualTo(0.dp)
        }
    }

    @Test
    fun `scrim e 56 por cento`() {
        assertThat(NeoScrim.alpha).isWithin(0.0001f).of(numberFrom(json, "scrimAlpha"))
    }
}
