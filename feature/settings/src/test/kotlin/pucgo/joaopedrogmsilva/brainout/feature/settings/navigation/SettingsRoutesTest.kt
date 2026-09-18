// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.settings.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SettingsRoutesTest {

    @Test
    fun `settings route is the canonical name`() {
        assertThat(SettingsRoutes.Settings).isEqualTo("settings")
    }
}
