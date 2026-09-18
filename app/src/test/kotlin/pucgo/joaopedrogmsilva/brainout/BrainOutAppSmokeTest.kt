// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Smoke test do módulo :app.
 *
 * Apenas verifica que o pacote raiz e a constante de módulo existem —
 * testes reais virão conforme features forem entregues.
 */
class BrainOutAppSmokeTest {

    @Test
    fun `app package constant is the documented root package`() {
        assertThat(BuildConfig.APPLICATION_ID).isEqualTo("pucgo.joaopedrogmsilva.brainout.debug")
    }

    @Test
    fun `BASE_URL is loaded from local properties at build time`() {
        // O valor default no app/build.gradle.kts é http://10.0.2.2:8000/
        // (coberto quando local.properties não define BASE_URL).
        assertThat(BuildConfig.BASE_URL).startsWith("http")
    }
}
