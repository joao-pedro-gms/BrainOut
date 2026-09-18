// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Identidade do usuário exibida na Home — esqueleto (E1.3).
// A fonte de verdade real virá em E1.8 (integração Room).

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
    val role: HomeUserRole
)

enum class HomeUserRole { Owner, Member }

/** Placeholder do usuário fake enquanto não há sessão real. */
val DefaultHomeUser: HomeUser = HomeUser(
    displayName = "João Pedro",
    initials = "JP",
    role = HomeUserRole.Owner
)
