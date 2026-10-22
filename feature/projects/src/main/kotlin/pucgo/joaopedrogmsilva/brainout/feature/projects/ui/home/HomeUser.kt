// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Identidade do usuário exibida na Home (E1.7).
//
// A identidade vem da sessão real (E1.8): o `HomeUserState` da
// ViewModel carrega o nome, o monograma e o papel do usuário ativo.
// Enquanto não há sessão — carregando ou sem login — a TopAppBar
// mostra a saudação genérica, sem inventar um nome (DEF-72).

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

/**
 * Modelo leve da identidade do usuário mostrada na `HomeScreen`.
 *
 * @param displayName nome exibido na saudação da TopAppBar.
 * @param initials monograma mostrado no avatar circular.
 * @param role papel aplicado no badge ao lado do nome.
 */
data class HomeUser(
    val displayName: String,
    val initials: String,
    val role: HomeUserRole,
)

enum class HomeUserRole { Owner, Member }
