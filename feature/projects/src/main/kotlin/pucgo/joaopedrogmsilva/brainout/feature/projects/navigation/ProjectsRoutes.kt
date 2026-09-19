// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Rotas e `NavGraphBuilder` do módulo :feature:projects.
//
// Este arquivo concentra as constantes de rota consumidas pelo
// `BrainOutNavHost` em :app (e pelos testes do :app — `ProjectsRoutesTest`)
// e a documentação das extensões de [NavGraphBuilder] usadas para
// registrar as telas do módulo.
//
// A tela de "Tarefas" mora em :feature:tasks e expõe seu próprio
// `tasksGraph()` em
// [pucgo.joaopedrogmsilva.brainout.feature.tasks.navigation.tasksGraph].
// O :app é responsável por registrá-lo no mesmo `NavHost` — ele não
// faz parte do grafo de Projects porque sua responsabilidade é outra
// (listagem global de tarefas), mas compartilha a navegação raiz.

package pucgo.joaopedrogmsilva.brainout.feature.projects.navigation

/** Constantes de rota consumidas pelo `BrainOutNavHost` em :app. */
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
