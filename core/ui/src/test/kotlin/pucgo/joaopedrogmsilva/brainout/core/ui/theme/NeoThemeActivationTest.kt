// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Ativação do tema Neo (NB-32): o que o app renderiza é o design system
// especificado, e não a paleta legada.

package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * O tema que o app realmente renderiza usa a identidade Neo.
 *
 * Por que este teste existe: entre a data em que a paleta Neo foi escrita
 * em `NeoColor.kt`/`NeoTypography.kt` e a ativação, o design system inteiro
 * era **código morto** — `NeoColors`, `NeoTypography` e `NeoRadii` eram
 * exercitados por testes, mas nenhuma linha de produção os lia, e o app
 * mostrava o roxo padrão do Material (`#6750A4`) com a fonte do sistema.
 * Os testes de paridade, contraste e assets continuariam verdes durante
 * toda essa janela, porque verificam a especificação contra ela mesma,
 * não contra o que a tela mostra.
 *
 * A lacuna que este arquivo fecha é a ligação: ele afirma que o
 * `ColorScheme`, a `Typography` e os `Shapes` que `BrainOutTheme` entrega
 * são os mesmos objetos construídos a partir de `NeoColors`,
 * `NeoTypography` e `NeoRadii`. Se alguém religar a paleta legada, trocar
 * uma fonte por `FontFamily.Default` ou restaurar 28dp em `extraLarge`,
 * a falha nomeia o token exato.
 *
 * Roda em JVM puro: o que se verifica são objetos `Color`/`TextStyle`/
 * `CornerSize`, que não precisam de Context nem de árvore de composição.
 * O wrapper `BrainOutNeoTheme` em si (a injeção de `LocalNeoColors`) é
 * exercitado pelos testes de UI das features, que já o montam via
 * `BrainOutTheme`.
 *
 * O runner é o do Robolectric pelo mesmo motivo de `ThemeSelectionTest`:
 * instanciar `ColorScheme`/`Color` da Compose toca `Resources` no caminho
 * da classe, e sem os recursos Android mesclados no classpath de teste o
 * JVM puro não sobe. `sdk = [34]` é a convenção do repositório — sem o
 * pin, o Robolectric assume o `targetSdk` 37 e falha no `android-all`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NeoThemeActivationTest {
    // ------------------------------------------------------------------
    // 1. Resolução do tema (o mesmo objeto que o wrapper injeta)
    // ------------------------------------------------------------------

    @Test
    fun `a resolucao devolve o tema claro completo e na mesma instancia`() {
        val spec = resolveNeoThemeSpec(darkTheme = false)

        assertThat(spec).isSameInstanceAs(NeoLightTheme)
        assertThat(spec.neoColors).isSameInstanceAs(NeoColors.Light)
    }

    @Test
    fun `a resolucao devolve o tema escuro completo e na mesma instancia`() {
        val spec = resolveNeoThemeSpec(darkTheme = true)

        assertThat(spec).isSameInstanceAs(NeoDarkTheme)
        assertThat(spec.neoColors).isSameInstanceAs(NeoColors.Dark)
    }

    @Test
    fun `a resolucao e pura e devolve os mesmos objetos em chamadas seguidas`() {
        // Uma resolução que constrói `ColorScheme`/`Typography`/`Shapes`
        // novos a cada chamada funcionaria na tela (o valor é o mesmo) e
        // ainda assim quebraria a identidade da composição local. Comparar
        // por instância impede que o singleton volte a ser rebuilt.
        assertThat(resolveNeoThemeSpec(darkTheme = false))
            .isSameInstanceAs(resolveNeoThemeSpec(darkTheme = false))
        assertThat(resolveNeoThemeSpec(darkTheme = true))
            .isSameInstanceAs(resolveNeoThemeSpec(darkTheme = true))
    }

    @Test
    fun `os dois temas declaram os mesmos slots tipograficos e de forma`() {
        listOf(NeoLightTheme, NeoDarkTheme).forEach { spec ->
            assertThat(spec.typography).isSameInstanceAs(NeoMaterialTypography)
            assertThat(spec.shapes).isSameInstanceAs(NeoShapes)
        }
    }

    // ------------------------------------------------------------------
    // 2. A paleta Neo chegou no ColorScheme do Material (sem roxo residual)
    // ------------------------------------------------------------------

    @Test
    fun `o scheme claro usa os papeis Neo e nao a paleta roxa do Material`() {
        val light = NeoColors.Light
        val scheme = NeoLightTheme.colorScheme

        assertThat(scheme.background).isEqualTo(light.background)
        assertThat(scheme.onBackground).isEqualTo(light.textPrimary)
        assertThat(scheme.surface).isEqualTo(light.surface)
        assertThat(scheme.onSurface).isEqualTo(light.textPrimary)
        assertThat(scheme.surfaceVariant).isEqualTo(light.surfaceAlt)
        assertThat(scheme.onSurfaceVariant).isEqualTo(light.textMuted)
        assertThat(scheme.primary).isEqualTo(light.link)
        assertThat(scheme.onPrimary).isEqualTo(light.onLink)
        assertThat(scheme.primaryContainer).isEqualTo(light.actionBackground)
        assertThat(scheme.onPrimaryContainer).isEqualTo(light.actionText)
        assertThat(scheme.error).isEqualTo(light.error)
        assertThat(scheme.onError).isEqualTo(light.onError)
        assertThat(scheme.errorContainer).isEqualTo(light.errorContainer)
        assertThat(scheme.outline).isEqualTo(light.borderDefault)
    }

    @Test
    fun `o scheme escuro usa os papeis Neo e nao a paleta roxa do Material`() {
        val dark = NeoColors.Dark
        val scheme = NeoDarkTheme.colorScheme

        assertThat(scheme.background).isEqualTo(dark.background)
        assertThat(scheme.onBackground).isEqualTo(dark.textPrimary)
        assertThat(scheme.surface).isEqualTo(dark.surface)
        assertThat(scheme.onSurface).isEqualTo(dark.textPrimary)
        assertThat(scheme.surfaceVariant).isEqualTo(dark.surfaceAlt)
        assertThat(scheme.onSurfaceVariant).isEqualTo(dark.textMuted)
        assertThat(scheme.primary).isEqualTo(dark.link)
        assertThat(scheme.onPrimary).isEqualTo(dark.onLink)
        assertThat(scheme.primaryContainer).isEqualTo(dark.actionBackground)
        assertThat(scheme.onPrimaryContainer).isEqualTo(dark.actionText)
        assertThat(scheme.error).isEqualTo(dark.error)
        assertThat(scheme.onError).isEqualTo(dark.onError)
        assertThat(scheme.errorContainer).isEqualTo(dark.errorContainer)
        assertThat(scheme.outline).isEqualTo(dark.borderDefault)
    }

    @Test
    fun `nenhum slot do scheme entrega a semente roxa do Material 6750A4`() {
        val seed = Color(0xFF6750A4)
        val leaked =
            neoMaterialColorSlots()
                .filter { (_, color) -> color == seed }
                .map { (slot) -> slot }

        assertThat(leaked).isEmpty()
    }

    @Test
    fun `surfaceTint scrim e niveis de superficie vem dos papeis Neo`() {
        listOf(
            NeoLightTheme.colorScheme to NeoColors.Light,
            NeoDarkTheme.colorScheme to NeoColors.Dark,
        ).forEach { (scheme, colors) ->
            assertThat(scheme.surfaceTint).isEqualTo(colors.link)
            assertThat(scheme.inverseSurface).isEqualTo(colors.inverseBackground)
            assertThat(scheme.inverseOnSurface).isEqualTo(colors.inverseText)
            assertThat(scheme.outlineVariant).isEqualTo(colors.borderDefault)
            assertThat(scheme.scrim).isEqualTo(colors.scrim)
            assertThat(scheme.surfaceContainerLowest).isEqualTo(colors.background)
            assertThat(scheme.surfaceContainerLow).isEqualTo(colors.surface)
            assertThat(scheme.surfaceContainer).isEqualTo(colors.surface)
            assertThat(scheme.surfaceContainerHigh).isEqualTo(colors.surfaceAlt)
            assertThat(scheme.surfaceContainerHighest).isEqualTo(colors.surfaceAlt)
        }
    }

    @Test
    fun `secondary e tertiary carregam os papeis Neo de urgente e sucesso`() {
        val light = NeoColors.Light
        assertThat(NeoLightTheme.colorScheme.secondary).isEqualTo(light.secondary)
        assertThat(NeoLightTheme.colorScheme.onSecondary).isEqualTo(light.onSecondary)
        assertThat(NeoLightTheme.colorScheme.secondaryContainer)
            .isEqualTo(light.secondaryContainer)
        assertThat(NeoLightTheme.colorScheme.onSecondaryContainer)
            .isEqualTo(light.secondaryText)
        assertThat(NeoLightTheme.colorScheme.tertiary).isEqualTo(light.success)
        assertThat(NeoLightTheme.colorScheme.onTertiary).isEqualTo(light.onSuccess)
        assertThat(NeoLightTheme.colorScheme.tertiaryContainer)
            .isEqualTo(light.successContainer)
        assertThat(NeoLightTheme.colorScheme.onTertiaryContainer)
            .isEqualTo(light.successText)

        val dark = NeoColors.Dark
        assertThat(NeoDarkTheme.colorScheme.secondary).isEqualTo(dark.secondary)
        assertThat(NeoDarkTheme.colorScheme.tertiary).isEqualTo(dark.success)
        assertThat(NeoDarkTheme.colorScheme.tertiaryContainer)
            .isEqualTo(dark.successContainer)
    }

    @Test
    fun `o scheme Neo e visualmente distinto do legado nos dois modos`() {
        // Se alguém religar `resolveBrainOutStaticColorScheme` no wrapper,
        // o scheme do tema volta a ser o roxo do Material. Comparar os
        // fundos e os primários pega isso sem precisar montar Compose.
        assertThat(NeoLightTheme.colorScheme.background)
            .isNotEqualTo(BrainOutLightColors.background)
        assertThat(NeoDarkTheme.colorScheme.background)
            .isNotEqualTo(BrainOutDarkColors.background)
        assertThat(NeoLightTheme.colorScheme.primary)
            .isNotEqualTo(BrainOutLightColors.primary)
        assertThat(NeoDarkTheme.colorScheme.primary)
            .isNotEqualTo(BrainOutDarkColors.primary)
    }

    // ------------------------------------------------------------------
    // 3. A tipografia Neo chegou nos quinze slots do Material
    // ------------------------------------------------------------------

    @Test
    fun `os quinze slots do Material recebem os estilos de NeoTypography`() {
        val typography = NeoLightTheme.typography
        val material =
            listOf(
                "displayLarge" to typography.displayLarge,
                "displayMedium" to typography.displayMedium,
                "displaySmall" to typography.displaySmall,
                "headlineLarge" to typography.headlineLarge,
                "headlineMedium" to typography.headlineMedium,
                "headlineSmall" to typography.headlineSmall,
                "titleLarge" to typography.titleLarge,
                "titleMedium" to typography.titleMedium,
                "titleSmall" to typography.titleSmall,
                "bodyLarge" to typography.bodyLarge,
                "bodyMedium" to typography.bodyMedium,
                "bodySmall" to typography.bodySmall,
                "labelLarge" to typography.labelLarge,
                "labelMedium" to typography.labelMedium,
                "labelSmall" to typography.labelSmall,
            )

        assertThat(material.map { (role, _) -> role })
            .containsExactlyElementsIn(NeoTypography.styles.keys)
            .inOrder()
        material.forEach { (role, style) ->
            assertThat(style).isSameInstanceAs(NeoTypography.styles.getValue(role))
        }
    }

    @Test
    fun `nenhum slot tipografico do tema Neo cai na fonte do sistema`() {
        // A regressão que importa: `BrainOutTypography` usava
        // `FontFamily.Default` em todos os papéis, o que deixava as cinco
        // fontes empacotadas em `res/font` sem nunca serem carregadas.
        val families =
            NeoTypography.styles.values
                .map { style -> style.fontFamily }
                .toSet()

        assertThat(families).containsExactly(NeoFonts.Archivo, NeoFonts.PublicSans)
        assertThat(NeoLightTheme.typography.displayLarge.fontFamily)
            .isSameInstanceAs(NeoFonts.Archivo)
        assertThat(NeoLightTheme.typography.bodyMedium.fontFamily)
            .isSameInstanceAs(NeoFonts.PublicSans)
    }

    // ------------------------------------------------------------------
    // 4. Os raios Neo chegaram nos cinco slots do Material
    // ------------------------------------------------------------------

    @Test
    fun `os cinco slots de forma recebem os raios declarados na especificacao`() {
        val shapes = NeoLightTheme.shapes

        assertThat(shapes.extraSmall).isEqualTo(RoundedCornerShape(NeoRadii.chip))
        assertThat(shapes.small).isEqualTo(RoundedCornerShape(NeoRadii.field))
        assertThat(shapes.medium).isEqualTo(RoundedCornerShape(NeoRadii.button))
        assertThat(shapes.large).isEqualTo(RoundedCornerShape(NeoRadii.card))
        assertThat(shapes.extraLarge).isEqualTo(RoundedCornerShape(NeoRadii.modal))
    }

    @Test
    fun `o tema Neo nao herda o arredondamento de 28dp da identidade legada`() {
        assertThat(NeoLightTheme.shapes.extraLarge)
            .isNotEqualTo(BrainOutShapes.extraLarge)
        assertThat(NeoDarkTheme.shapes.extraLarge)
            .isNotEqualTo(BrainOutShapes.extraLarge)
    }
}

/**
 * Todos os slots de cor do `ColorScheme` dos dois temas Neo, com o nome do
 * slot.
 *
 * Existe para o teste do resíduo roxo: varrer os pares à mão deixaria a
 * cobertura depender do que alguém lembrou de listar, e um slot novo do
 * Material (o scheme ganha um a cada versão) passaria despercebido com o
 * `#6750A4` do default.
 */
private fun neoMaterialColorSlots(): List<Pair<String, Color>> {
    fun slotsOf(
        prefix: String,
        scheme: ColorScheme,
    ): List<Pair<String, Color>> =
        listOf(
            "primary" to scheme.primary,
            "onPrimary" to scheme.onPrimary,
            "primaryContainer" to scheme.primaryContainer,
            "onPrimaryContainer" to scheme.onPrimaryContainer,
            "inversePrimary" to scheme.inversePrimary,
            "secondary" to scheme.secondary,
            "onSecondary" to scheme.onSecondary,
            "secondaryContainer" to scheme.secondaryContainer,
            "onSecondaryContainer" to scheme.onSecondaryContainer,
            "tertiary" to scheme.tertiary,
            "onTertiary" to scheme.onTertiary,
            "tertiaryContainer" to scheme.tertiaryContainer,
            "onTertiaryContainer" to scheme.onTertiaryContainer,
            "error" to scheme.error,
            "onError" to scheme.onError,
            "errorContainer" to scheme.errorContainer,
            "onErrorContainer" to scheme.onErrorContainer,
            "background" to scheme.background,
            "onBackground" to scheme.onBackground,
            "surface" to scheme.surface,
            "onSurface" to scheme.onSurface,
            "surfaceVariant" to scheme.surfaceVariant,
            "onSurfaceVariant" to scheme.onSurfaceVariant,
            "surfaceTint" to scheme.surfaceTint,
            "inverseSurface" to scheme.inverseSurface,
            "inverseOnSurface" to scheme.inverseOnSurface,
            "outline" to scheme.outline,
            "outlineVariant" to scheme.outlineVariant,
            "scrim" to scheme.scrim,
            "surfaceBright" to scheme.surfaceBright,
            "surfaceDim" to scheme.surfaceDim,
            "surfaceContainerLowest" to scheme.surfaceContainerLowest,
            "surfaceContainerLow" to scheme.surfaceContainerLow,
            "surfaceContainer" to scheme.surfaceContainer,
            "surfaceContainerHigh" to scheme.surfaceContainerHigh,
            "surfaceContainerHighest" to scheme.surfaceContainerHighest,
        ).map { (slot, color) -> "$prefix.$slot" to color }

    return slotsOf("light", NeoLightTheme.colorScheme) +
        slotsOf("dark", NeoDarkTheme.colorScheme)
}
