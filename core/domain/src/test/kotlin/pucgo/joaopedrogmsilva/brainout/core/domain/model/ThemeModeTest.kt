// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Testes do [ThemeMode] — infraestrutura mínima de tema do NB-06
 * (issue #93, E4.5).
 *
 * O inventário auditado mostrou que a escolha manual persistida
 * (System/Light/Dark) não existia; NB-06 define o modo e a
 * precedência **sem** construir a tela de preferências (isso entra
 * em NB-27) e sem tocar em nenhuma tela existente.
 *
 * A matriz coberta aqui é o critério 4 do NB-06: Light/Dark/System,
 * inclusive «sistema escuro com escolha manual clara» e o inverso —
 * a precedência é pura, sem `isSystemInDarkTheme()`, que só é
 * consultado uma vez no `MainActivity`.
 */
class ThemeModeTest {

    // ------------------------------------------------------------------
    // Precedência: manual vence o sistema nos dois sentidos
    // ------------------------------------------------------------------

    @Test
    fun `escolha manual clara vence sistema escuro`() {
        assertThat(ThemeMode.LIGHT.resolvesToDark(systemInDarkTheme = true)).isFalse()
    }

    @Test
    fun `escolha manual escura vence sistema claro`() {
        assertThat(ThemeMode.DARK.resolvesToDark(systemInDarkTheme = false)).isTrue()
    }

    @Test
    fun `escolha manual clara nao muda com sistema claro`() {
        assertThat(ThemeMode.LIGHT.resolvesToDark(systemInDarkTheme = false)).isFalse()
    }

    @Test
    fun `escolha manual escura nao muda com sistema escuro`() {
        assertThat(ThemeMode.DARK.resolvesToDark(systemInDarkTheme = true)).isTrue()
    }

    @Test
    fun `modo system segue o sistema nos dois sentidos`() {
        assertThat(ThemeMode.SYSTEM.resolvesToDark(systemInDarkTheme = true)).isTrue()
        assertThat(ThemeMode.SYSTEM.resolvesToDark(systemInDarkTheme = false)).isFalse()
    }

    // ------------------------------------------------------------------
    // Chave de persistência (DataStore)
    // ------------------------------------------------------------------

    @Test
    fun `chave de persistencia faz ida e volta nos tres modos`() {
        for (mode in ThemeMode.entries) {
            assertThat(ThemeMode.fromStorageKey(mode.toStorageKey())).isEqualTo(mode)
        }
    }

    @Test
    fun `chave de persistencia e o literal em minusculas do modo`() {
        assertThat(ThemeMode.SYSTEM.toStorageKey()).isEqualTo("system")
        assertThat(ThemeMode.LIGHT.toStorageKey()).isEqualTo("light")
        assertThat(ThemeMode.DARK.toStorageKey()).isEqualTo("dark")
    }

    @Test
    fun `chave desconhecida ou ausente cai no system`() {
        // Defesa contra escrita externa/downgrade: nunca quebra a UI.
        assertThat(ThemeMode.fromStorageKey(null)).isEqualTo(ThemeMode.SYSTEM)
        assertThat(ThemeMode.fromStorageKey("")).isEqualTo(ThemeMode.SYSTEM)
        assertThat(ThemeMode.fromStorageKey("neon")).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun `os tres modos estao declarados`() {
        assertThat(ThemeMode.entries)
            .containsExactly(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
            .inOrder()
    }
}
