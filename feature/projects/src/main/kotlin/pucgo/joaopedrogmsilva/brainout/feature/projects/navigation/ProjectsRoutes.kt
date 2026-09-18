// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Rotas do módulo :feature:projects.

package pucgo.joaopedrogmsilva.brainout.feature.projects.navigation

object ProjectsRoutes {
    const val Home: String = "home"
    const val HomePattern: String = "home"

    /** Argumento da rota de detalhe de projeto. */
    const val ProjectIdArg: String = "projectId"

    /** Rota parametrizada para o detalhe. */
    const val ProjectDetailPattern: String = "project/{$ProjectIdArg}"

    /** Helper para construir a rota concreta a partir do id. */
    fun projectDetailRoute(projectId: String): String = "project/$projectId"
}
