// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Rotas de navegação do módulo :feature:auth (Splash, Login, Register).
// Mantidas como strings constantes para evitar typos entre NavHost e o
// chamador das actions em :app.

package pucgo.joaopedrogmsilva.brainout.feature.auth.navigation

/**
 * Rotas públicas do fluxo de autenticação.
 *
 * Usar [route] ao chamar `navigate(...)` e [routePattern] ao declarar as
 * entradas no `NavHost`. Quando o destino aceita argumentos (nenhum por
 * enquanto), o `routePattern` inclui placeholders `{arg}`.
 */
object AuthRoutes {
    const val Splash: String = "splash"
    const val Login: String = "login"
    const val Register: String = "register"

    /** Padrões exatamente iguais a [Splash]/[Login]/[Register] — mantido
     *  para simetria com módulos que precisam de argumentos. */
    const val SplashPattern: String = "splash"
    const val LoginPattern: String = "login"
    const val RegisterPattern: String = "register"
}
