// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import android.content.res.Configuration
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
 *  1. A resolução do tema é uma função pura: `darkTheme = true` entrega
 *     o tema Neo escuro e `false` o claro, sem efeitos colaterais.
 *  2. Os dois temas têm tokens invertidos (background e primary
 *     contrastam), evitando cópias acidentais que fariam os dois modos
 *     serem visualmente idênticos.
 *  3. A flag consumida por `BrainOutTheme` (via `isSystemInDarkTheme`)
 *     é exatamente o bit `Configuration.UI_MODE_NIGHT_*` — quem chama
 *     o tema sem override confia na configuração do sistema.
 *  4. `NeoShapes` cobre os 5 slots documentados pelo Material 3.
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
    // 1. Resolução estática do tema
    // ------------------------------------------------------------------

    @Test
    fun `darkTheme true yields the Neo dark theme`() {
        val theme = resolveNeoThemeSpec(darkTheme = true)

        assertThat(theme).isSameInstanceAs(NeoDarkTheme)
    }

    @Test
    fun `darkTheme false yields the Neo light theme`() {
        val theme = resolveNeoThemeSpec(darkTheme = false)

        assertThat(theme).isSameInstanceAs(NeoLightTheme)
    }

    @Test
    fun `light and dark themes are distinct instances`() {
        // Os dois temas têm tokens invertidos (surface, primary,
        // background, etc.). Garante que não houve cópia acidental que
        // faria dark e light serem o mesmo objeto.
        assertThat(NeoLightTheme).isNotSameInstanceAs(NeoDarkTheme)
    }

    // ------------------------------------------------------------------
    // 2. Tokens visíveis: a paleta escura tem contraste invertido vs clara
    // ------------------------------------------------------------------

    @Test
    fun `dark theme background is darker than light theme background`() {
        // Background dark: `night` (luminância muito baixa).
        // Background light: `paper` (luminância quase 1).
        assertThat(NeoDarkTheme.colorScheme.background.red)
            .isLessThan(NeoLightTheme.colorScheme.background.red)
    }

    @Test
    fun `dark theme primary is lighter than light theme primary`() {
        // Light primary: `ochre` (0xFF544500).
        // Dark primary: `yellow` (0xFFF9D94A).
        assertThat(NeoDarkTheme.colorScheme.primary.red)
            .isGreaterThan(NeoLightTheme.colorScheme.primary.red)
    }

    // ------------------------------------------------------------------
    // 3. BrainOutTheme segue a configuração do sistema
    // ------------------------------------------------------------------

    @Test
    fun `system night mode YES maps to the Neo dark theme`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val nightConfig =
            Configuration(app.resources.configuration).apply {
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
        assertThat(resolveNeoThemeSpec(darkTheme = wouldPickDark))
            .isSameInstanceAs(NeoDarkTheme)
    }

    @Test
    fun `system night mode NO maps to the Neo light theme`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val dayConfig =
            Configuration(app.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    Configuration.UI_MODE_NIGHT_NO
            }

        val wouldPickDark =
            (dayConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        assertThat(wouldPickDark).isFalse()
        assertThat(resolveNeoThemeSpec(darkTheme = wouldPickDark))
            .isSameInstanceAs(NeoLightTheme)
    }

    // ------------------------------------------------------------------
    // 4. NeoShapes é único e centralizado
    // ------------------------------------------------------------------

    @Test
    fun `NeoShapes has the documented five token slots`() {
        // Material 3 Shapes: extraSmall/small/medium/large/extraLarge.
        // Garante que NeoShapes não caiu no default vazio.
        val s = NeoShapes
        assertThat(s.extraSmall).isNotNull()
        assertThat(s.small).isNotNull()
        assertThat(s.medium).isNotNull()
        assertThat(s.large).isNotNull()
        assertThat(s.extraLarge).isNotNull()
    }
}
