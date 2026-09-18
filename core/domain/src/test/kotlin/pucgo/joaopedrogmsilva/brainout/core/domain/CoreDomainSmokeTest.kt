// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole

/**
 * Smoke test do módulo :core:domain.
 *
 * Após a entrega de E1.4 o marcador de pacote foi promovido para uma
 * constante de produção dentro do próprio objeto. Este teste garante
 * que o módulo continua íntegro após mudanças de empacotamento.
 */
class CoreDomainSmokeTest {

    @Test
    fun `core domain package constant points to documented root`() {
        assertThat(CORE_DOMAIN_PACKAGE).isEqualTo("pucgo.joaopedrogmsilva.brainout.core.domain")
    }

    @Test
    fun `module exposes Owner and Member roles`() {
        assertThat(UserRole.entries).containsExactly(UserRole.OWNER, UserRole.MEMBER)
    }
}
