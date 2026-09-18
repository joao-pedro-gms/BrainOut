// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Smoke test do módulo :core:domain.
 *
 * Verifica apenas o marcador de pacote por enquanto. Entidades e use
 * cases chegam em E1.4 e E1.7 com testes próprios.
 */
class CoreDomainSmokeTest {

    @Test
    fun `domain package constant points to documented root`() {
        assertThat(CORE_DOMAIN_PACKAGE).isEqualTo("pucgo.joaopedrogmsilva.brainout.core.domain")
    }
}
