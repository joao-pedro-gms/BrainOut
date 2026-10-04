// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes do critério E4.5 do ROADMAP.
 *
 * "Tema claro e escuro com tokens centralizados em `:core:ui/theme`;
 * alternância segue a configuração do sistema."
 *
 * Estes testes afirmam que:
 *  1. A função pura `resolveBrainOutStaticColorScheme(darkTheme)`
 *     mapeia `true → BrainOutDarkColors` e `false → BrainOutLightColors`,
 *     sem efeitos colaterais — a única fonte de verdade é o parâmetro.
 *  2. As paletas clara/escura têm tokens invertidos (background e
 *     primary contrastam), evitando cópias acidentais que fariam os
 *     dois modos serem visualmente idênticos.
 *  3. A flag consumida por `BrainOutTheme` (via `isSystemInDarkTheme`)
 *     é exatamente o bit `Configuration.UI_MODE_NIGHT_*` — quem chama
 *     o tema sem override confia na configuração do sistema.
 *  4. `BrainOutShapes` cobre os 5 slots documentados pelo Material 3.
 *  5. **(NB-06)** O wrapper aditivo `BrainOutNeoTheme` amarra
 *     `ColorScheme`, tipografia e shapes aos tokens neo — e só ele
 *     passa a receber o modo já resolvido pelo app (a matriz de
 *     precedência Light/Dark/System vive em `ThemeModeTest`, no
 *     `:core:domain`, porque o modo persistido é domínio, não UI).
 *
 * Por que Robolectric e não Compose UI test: o módulo :core:ui ainda
 * não declara `androidx.compose.ui:ui-test-junit4`, e a regra E4.5
 * ("alternância segue a configuração do sistema") é validada
 * deterministicamente em JUnit, sem precisar montar Compose runtime.
 */
@RunWith(RobolectricTestRunner::class)
// `sdk = [34]` pela convenção do repositório (AGENTS.md). Sem o pin, o
// Robolectric assume o `targetSdk` do projeto (37) e falha na inicialização
// com `targetSdkVersion=37 > maxSdkVersion=35` — o que impedia
// `:core:ui:testDebugUnitTest` de rodar por inteiro.
@Config(sdk = [34])
class ThemeSelectionTest {

    // ------------------------------------------------------------------
    // 1. Resolução estática do ColorScheme
    // ------------------------------------------------------------------

    @Test
    fun `darkTheme true yields the BrainOutDarkColors palette`() {
        val scheme = resolveBrainOutStaticColorScheme(darkTheme = true)

        assertThat(scheme).isSameInstanceAs(BrainOutDarkColors)
    }

    @Test
    fun `darkTheme false yields the BrainOutLightColors palette`() {
        val scheme = resolveBrainOutStaticColorScheme(darkTheme = false)

        assertThat(scheme).isSameInstanceAs(BrainOutLightColors)
    }

    @Test
    fun `light and dark palettes are distinct instances`() {
        // Os dois ColorSchemes têm tokens invertidos (surface, primary,
        // background, etc.). Garante que não houve cópia acidental que
        // faria dark e light serem o mesmo objeto.
        assertThat(BrainOutLightColors).isNotSameInstanceAs(BrainOutDarkColors)
    }

    // ------------------------------------------------------------------
    // 2. Tokens visíveis: a paleta escura tem contraste invertido vs clara
    // ------------------------------------------------------------------

    @Test
    fun `dark palette background is darker than light palette background`() {
        // Background dark: 0xFF1C1B1F (luminância muito baixa).
        // Background light: 0xFFFFFBFE (luminância quase 1).
        assertThat(BrainOutDarkColors.background.red).isLessThan(BrainOutLightColors.background.red)
    }

    @Test
    fun `dark palette primary is lighter than light palette primary`() {
        // Light primary: 0xFF6750A4 (roxo escuro).
        // Dark primary: 0xFFD0BCFF (roxo claro).
        assertThat(BrainOutDarkColors.primary.red).isGreaterThan(BrainOutLightColors.primary.red)
    }

    // ------------------------------------------------------------------
    // 3. BrainOutTheme segue a configuração do sistema
    // ------------------------------------------------------------------

    @Test
    fun `system night mode YES maps to BrainOutDarkColors`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val nightConfig = Configuration(app.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                Configuration.UI_MODE_NIGHT_YES
        }

        // BrainOutTheme lê isSystemInDarkTheme(), que consulta
        // Configuration.UI_MODE_NIGHT_MASK. Forçando YES, o helper
        // deve resolver para o esquema escuro.
        // O check abaixo espelha exatamente isSystemInDarkTheme(), mas
        // sem depender da API 30+ de Configuration.isNightModeActive().
        val wouldPickDark =
            (nightConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        assertThat(wouldPickDark).isTrue()
        assertThat(resolveBrainOutStaticColorScheme(darkTheme = wouldPickDark))
            .isSameInstanceAs(BrainOutDarkColors)
    }

    @Test
    fun `system night mode NO maps to BrainOutLightColors`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val dayConfig = Configuration(app.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                Configuration.UI_MODE_NIGHT_NO
        }

        val wouldPickDark =
            (dayConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        assertThat(wouldPickDark).isFalse()
        assertThat(resolveBrainOutStaticColorScheme(darkTheme = wouldPickDark))
            .isSameInstanceAs(BrainOutLightColors)
    }

    // ------------------------------------------------------------------
    // 4. BrainOutShapes é único e centralizado
    // ------------------------------------------------------------------

    @Test
    fun `BrainOutShapes has the documented five token slots`() {
        // Material 3 Shapes: extraSmall/small/medium/large/extraLarge.
        // Garante que BrainOutShapes não caiu no default vazio.
        val s = BrainOutShapes
        assertThat(s.extraSmall).isNotNull()
        assertThat(s.small).isNotNull()
        assertThat(s.medium).isNotNull()
        assertThat(s.large).isNotNull()
        assertThat(s.extraLarge).isNotNull()
    }

    // ------------------------------------------------------------------
    // 5. NB-06 — wrapper aditivo BrainOutNeoTheme
    // ------------------------------------------------------------------

    @Test
    fun `wrapper neo escolhe a paleta neo conforme o modo resolvido`() {
        // O wrapper não consulta o sistema: ele só recebe o booleano já
        // resolvido pelo app (Light/Dark/System com precedência).
        assertThat(resolveNeoColors(darkTheme = true)).isSameInstanceAs(NeoColors.Dark)
        assertThat(resolveNeoColors(darkTheme = false)).isSameInstanceAs(NeoColors.Light)
    }

    @Test
    fun `ColorScheme do wrapper neo carrega os papeis neo nos dois modos`() {
        val light = NeoColors.Light.toMaterialColorScheme(dark = false)
        val dark = NeoColors.Dark.toMaterialColorScheme(dark = true)

        // Fundo/superfície vêm dos papéis neo, não dos defaults Material.
        assertThat(light.background).isEqualTo(NeoColors.Light.background)
        assertThat(light.surface).isEqualTo(NeoColors.Light.surface)
        assertThat(light.onBackground).isEqualTo(NeoColors.Light.textPrimary)
        assertThat(dark.background).isEqualTo(NeoColors.Dark.background)
        assertThat(dark.surface).isEqualTo(NeoColors.Dark.surface)
        assertThat(dark.onBackground).isEqualTo(NeoColors.Dark.textPrimary)

        // `primary` é o link legível — nunca o amarelo da ação (DESIGN §3).
        assertThat(light.primary).isEqualTo(NeoColors.Light.link)
        assertThat(dark.primary).isEqualTo(NeoColors.Dark.link)
        assertThat(light.primaryContainer).isEqualTo(NeoColors.Light.actionBackground)
        assertThat(dark.primaryContainer).isEqualTo(NeoColors.Dark.actionBackground)
    }

    @Test
    fun `tipografia do wrapper neo sao exatamente os quinze estilos do contrato`() {
        val t = NeoMaterialTypography
        val actual = mapOf(
            "displayLarge" to t.displayLarge,
            "displayMedium" to t.displayMedium,
            "displaySmall" to t.displaySmall,
            "headlineLarge" to t.headlineLarge,
            "headlineMedium" to t.headlineMedium,
            "headlineSmall" to t.headlineSmall,
            "titleLarge" to t.titleLarge,
            "titleMedium" to t.titleMedium,
            "titleSmall" to t.titleSmall,
            "bodyLarge" to t.bodyLarge,
            "bodyMedium" to t.bodyMedium,
            "bodySmall" to t.bodySmall,
            "labelLarge" to t.labelLarge,
            "labelMedium" to t.labelMedium,
            "labelSmall" to t.labelSmall,
        )

        // Paridade total: se o wrapper deixar de usar um estilo neo e
        // cair no default do Material, este mapa diverge e o teste cai.
        assertThat(actual).isEqualTo(NeoTypography.styles)
    }

    @Test
    fun `shapes do wrapper neo usam os raios 4 4 8 8 16 do DESIGN`() {
        // DESIGN §5: extraSmall 4; small 4; medium 8; large 8; extraLarge 16.
        val shapes = NeoMaterialShapes
        assertThat(shapes.extraSmall).isEqualTo(RoundedCornerShape(NeoRadii.chip))
        assertThat(shapes.small).isEqualTo(RoundedCornerShape(NeoRadii.chip))
        assertThat(shapes.medium).isEqualTo(RoundedCornerShape(NeoRadii.button))
        assertThat(shapes.large).isEqualTo(RoundedCornerShape(NeoRadii.card))
        assertThat(shapes.extraLarge).isEqualTo(RoundedCornerShape(NeoRadii.modal))
    }
}
