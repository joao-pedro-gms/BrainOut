// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Registro central de rotas do NavHost do app.

package pucgo.joaopedrogmsilva.brainout.navigation

import pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail.ProjectDetailViewModel

/**
 * Rotas top-level do NavHost do BrainOut.
 *
 * Mantidas como strings constantes para evitar typos ao navegar entre
 * destinos. O esqueleto E1.3 define o conjunto mínimo exigido pelo
 * roadmap; novos destinos serão acrescentados em iterações futuras.
 */
object BrainOutRoutes {
    const val MainGraph: String = "main"
    const val ProjectsGraph: String = "projects_graph"
    const val TasksGraph: String = "tasks_graph"
    const val DashboardGraph: String = "dashboard_graph"
    const val SettingsGraph: String = "settings_graph"
    const val Splash: String = "splash"
    const val Login: String = "login"
    const val Register: String = "register"
    const val Home: String = "home"
    const val Settings: String = "settings"
    private const val PROJECT_DETAIL_PREFIX: String = "project"

    /**
     * Nome do argumento que leva o id do projeto na rota do detalhe.
     *
     * O valor vem do [ProjectDetailViewModel], que é quem **lê** o
     * argumento: o registro central tem de honrar a chave que o
     * destino consome. Declarar o nome nos dois lados permitia que
     * divergissem — e nenhum teste pegaria, porque os dois lados do
     * teste citariam a própria constante.
     */
    const val ProjectIdArg: String = ProjectDetailViewModel.PROJECT_ID_ARG

    const val ProjectDetailPattern: String = "$PROJECT_DETAIL_PREFIX/{$ProjectIdArg}"

    /** Constrói a rota concreta do ProjectDetail a partir do id. */
    fun projectDetail(projectId: String): String = "$PROJECT_DETAIL_PREFIX/$projectId"
}
