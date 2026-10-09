// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// NB-04: paridade entre a especificação (tokens.json) e a implementação Kotlin.

package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.ui.graphics.Color
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
 */
class NeoTokenParityTest {
    private val json: String = readTokensJson()

    private fun hexToColor(hex: String): Color {
        val value = hex.removePrefix("#").toLong(16)
        return Color(0xFF000000L or value)
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
        assertThat(NeoRadii.none).isEqualTo(0.dp)
        assertThat(NeoRadii.chip).isEqualTo(4.dp)
        assertThat(NeoRadii.field).isEqualTo(4.dp)
        assertThat(NeoRadii.button).isEqualTo(8.dp)
        assertThat(NeoRadii.card).isEqualTo(8.dp)
        assertThat(NeoRadii.menu).isEqualTo(8.dp)
        assertThat(NeoRadii.modal).isEqualTo(16.dp)
    }

    @Test
    fun `bordas declaradas batem com os tokens`() {
        assertThat(NeoBorders.subtle).isEqualTo(1.dp)
        assertThat(NeoBorders.default).isEqualTo(2.dp)
        assertThat(NeoBorders.focus).isEqualTo(3.dp)
        assertThat(NeoBorders.focusGap).isEqualTo(2.dp)
    }

    @Test
    fun `sombra rigida nao tem blur nem spread no JSON nem no Kotlin`() {
        val shadows = listOf(NeoShadows.none, NeoShadows.small, NeoShadows.medium, NeoShadows.display)
        shadows.forEach { shadow ->
            assertThat(shadow.blur).isEqualTo(0.dp)
            assertThat(shadow.spread).isEqualTo(0.dp)
        }
        assertThat(listOf(NeoShadows.none, NeoShadows.small, NeoShadows.medium, NeoShadows.display))
            .containsExactly(
                NeoShadow(0.dp, 0.dp, 0.dp, 0.dp),
                NeoShadow(2.dp, 2.dp, 0.dp, 0.dp),
                NeoShadow(4.dp, 4.dp, 0.dp, 0.dp),
                NeoShadow(6.dp, 6.dp, 0.dp, 0.dp),
            )
        // O JSON declara exatamente 4 sombras (none/small/medium/display).
        val declaredShadowNames =
            json
                .substringAfter("\"shadows\"")
                .substringBefore("\"sizes\"")
                .let { Regex("\"(\\w+)\"\\s*:\\s*\\{").findAll(it).map { m -> m.groupValues[1] }.toList() }
        assertThat(declaredShadowNames).containsExactly("none", "small", "medium", "display")
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
        assertThat(NeoSizes.icon).isEqualTo(24.dp)
        assertThat(NeoSizes.iconMeta).isEqualTo(20.dp)
        assertThat(NeoSizes.iconEmpty).isEqualTo(32.dp)
        assertThat(NeoSizes.badgeMinHeight).isEqualTo(28.dp)
        assertThat(NeoSizes.navMinHeight).isEqualTo(80.dp)
    }

    @Test
    fun `breakpoints e margens batem com o JSON`() {
        assertThat(NeoLayout.mediumMinWidth).isEqualTo(600.dp)
        assertThat(NeoLayout.expandedMinWidth).isEqualTo(840.dp)
        assertThat(NeoLayout.largeMinWidth).isEqualTo(1200.dp)
        assertThat(NeoLayout.compactHeightMaxExclusive).isEqualTo(480.dp)
        assertThat(NeoLayout.compactMargin).isEqualTo(16.dp)
        assertThat(NeoLayout.mediumMargin).isEqualTo(24.dp)
        assertThat(NeoLayout.expandedMargin).isEqualTo(32.dp)
        assertThat(NeoLayout.formMaxWidth).isEqualTo(480.dp)
        assertThat(NeoLayout.contentMaxWidth).isEqualTo(1200.dp)
    }

    @Test
    fun `scrim e 56 por cento`() {
        assertThat(NeoScrim.alpha).isWithin(0.0001f).of(0.56f)
    }
}
