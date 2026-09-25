// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Mappers do HomeViewModel: conversão entre modelos de domínio
// (User/UserRole/Tag) e modelos de UI (HomeUserState/HomeUserRole/
// TagChip), mais mapeamento de exceções em mensagens canônicas
// (E2.8). Mantidos `internal` por serem detalhe de implementação do
// pacote da feature.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole

/** Mapeia [UserRole] (domínio) para a enumeração visual [HomeUserRole]. */
internal fun UserRole.toHomeRole(): HomeUserRole = when (this) {
    UserRole.OWNER -> HomeUserRole.Owner
    UserRole.MEMBER -> HomeUserRole.Member
}

/** Converte um [User] ativo em [HomeUserState] consumido pela UI. */
internal fun User?.toHomeUserState(): HomeUserState = when (this) {
    null -> HomeUserState.SignedOut
    else -> HomeUserState.SignedIn(
        displayName = name,
        initials = computeInitials(name),
        role = role.toHomeRole(),
    )
}

/** Converte uma [Tag] do domínio no [TagChip] usado pelo card. */
internal fun Tag.toChip(): TagChip = TagChip(id = id, name = name, color = color)

/**
 * Converte uma [Throwable] vinda do Room em uma mensagem canônica
 * para a UI (E2.8).
 */
internal fun Throwable.toHomeErrorMessage(): String = HomeViewModel.ERROR_LOAD_FAILED

/** Mesmo mapeamento para ações (CRUD) — placeholder para evolução futura. */
internal fun Throwable.toHomeActionErrorMessage(): String = HomeViewModel.ERROR_ACTION_FAILED

/**
 * Gera o monograma do nome. Quando o nome tem mais de uma palavra,
 * usa a primeira letra do primeiro e do último nome. Quando tem
 * apenas uma palavra, usa as duas primeiras letras. Se estiver vazio
 * (não deve acontecer — `User` valida `name` em `init`), devolve "?".
 */
internal fun computeInitials(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "?"
    val parts = trimmed.split(' ').filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> {
            val first = parts.first().first().uppercaseChar()
            val last = parts.last().first().uppercaseChar()
            "$first$last"
        }
        else -> trimmed.take(2).uppercase()
    }
}
