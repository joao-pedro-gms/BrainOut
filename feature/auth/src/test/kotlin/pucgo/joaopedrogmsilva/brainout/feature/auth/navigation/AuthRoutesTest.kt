// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.auth.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Garante que as rotas expostas pelo módulo :feature:auth estão alinhadas
 * com as declaradas em `BrainOutRoutes` (no :app). Evita drift silencioso.
 */
class AuthRoutesTest {

    @Test
    fun `splash route is the canonical name`() {
        assertThat(AuthRoutes.Splash).isEqualTo("splash")
    }

    @Test
    fun `login route is the canonical name`() {
        assertThat(AuthRoutes.Login).isEqualTo("login")
    }

    @Test
    fun `register route is the canonical name`() {
        assertThat(AuthRoutes.Register).isEqualTo("register")
    }
}
