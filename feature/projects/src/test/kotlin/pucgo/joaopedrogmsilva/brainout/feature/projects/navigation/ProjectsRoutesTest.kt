// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.projects.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Garante que as rotas expostas pelo módulo :feature:projects estão
 * alinhadas com o esperado pelo `BrainOutNavHost` em :app.
 */
class ProjectsRoutesTest {

    @Test
    fun `home route is the canonical name`() {
        assertThat(ProjectsRoutes.Home).isEqualTo("home")
    }

    @Test
    fun `project detail pattern contains project id placeholder`() {
        assertThat(ProjectsRoutes.ProjectDetailPattern).contains("{projectId}")
        assertThat(ProjectsRoutes.ProjectIdArg).isEqualTo("projectId")
    }

    @Test
    fun `project detail route builder injects the project id`() {
        assertThat(ProjectsRoutes.projectDetailRoute("42")).isEqualTo("project/42")
    }
}
