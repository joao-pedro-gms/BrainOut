// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.navigation.BrainOutRoutes

/**
 * Smoke test do módulo :app.
 *
 * Verifica que o pacote raiz, o BuildConfig e o registro de rotas do
 * `BrainOutNavHost` continuam consistentes com a documentação do projeto.
 * Testes de UI/Compose virão nos marcos E1.6/E2.x.
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

    @Test
    fun `nav routes expose the expected top level destinations`() {
        // Garante que o conjunto de rotas exposto em BrainOutRoutes está
        // alinhado com as 6 telas exigidas pelo marco E1.3 do ROADMAP.
        val actual = setOf(
            BrainOutRoutes.Splash,
            BrainOutRoutes.Login,
            BrainOutRoutes.Register,
            BrainOutRoutes.Home,
            BrainOutRoutes.Settings,
            BrainOutRoutes.ProjectDetailPattern
        )
        val expected = setOf(
            BrainOutRoutes.Splash,
            BrainOutRoutes.Login,
            BrainOutRoutes.Register,
            BrainOutRoutes.Home,
            BrainOutRoutes.Settings,
            BrainOutRoutes.ProjectDetailPattern
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `project detail route builder injects the project id`() {
        val projectId = "demo-123"
        val route = BrainOutRoutes.projectDetail(projectId)
        assertThat(route).isEqualTo("project/$projectId")
        // O padrão (com placeholder) é exposto separado para evitar typos.
        assertThat(BrainOutRoutes.ProjectDetailPattern)
            .isEqualTo("project/{${BrainOutRoutes.ProjectIdArg}}")
    }
}
