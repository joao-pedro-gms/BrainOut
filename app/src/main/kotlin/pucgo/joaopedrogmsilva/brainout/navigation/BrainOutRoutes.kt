// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Registro central de rotas do NavHost do app.

package pucgo.joaopedrogmsilva.brainout.navigation

/**
 * Rotas top-level do NavHost do BrainOut.
 *
 * Mantidas como strings constantes para evitar typos ao navegar entre
 * destinos. O esqueleto E1.3 define o conjunto mínimo exigido pelo
 * roadmap; novos destinos serão acrescentados em iterações futuras.
 */
object BrainOutRoutes {
    const val Splash: String = "splash"
    const val Login: String = "login"
    const val Register: String = "register"
    const val Home: String = "home"
    const val Settings: String = "settings"
    const val ProjectDetailPattern: String = "project/{projectId}"
    const val ProjectIdArg: String = "projectId"

    /** Constrói a rota concreta do ProjectDetail a partir do id. */
    fun projectDetail(projectId: String): String = "project/$projectId"
}
