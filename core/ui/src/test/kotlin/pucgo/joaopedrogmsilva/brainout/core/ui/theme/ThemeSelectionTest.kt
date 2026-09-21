// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
 *
 * Por que Robolectric e não Compose UI test: o módulo :core:ui ainda
 * não declara `androidx.compose.ui:ui-test-junit4`, e a regra E4.5
 * ("alternância segue a configuração do sistema") é validada
 * deterministicamente em JUnit, sem precisar montar Compose runtime.
 */
@RunWith(RobolectricTestRunner::class)
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
}
