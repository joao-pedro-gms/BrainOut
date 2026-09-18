// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import pucgo.joaopedrogmsilva.brainout.core.data.session.ActiveUserProvider
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User

/**
 * ViewModel da [HomeScreen] (E1.7).
 *
 * Observa o [User] ativo via [ActiveUserProvider] e expõe o resultado
 * como [HomeUiState]. O id da sessão é resolvido em [User] completo
 * (nome, e-mail, papel) — a partir daí a tela deriva o monograma, a
 * saudação e o badge de papel sem precisar conhecer o id.
 *
 * O FAB "Criar projeto" precisa do `User.role` para decidir se é
 * habilitado (Owner) ou dispara o diálogo de upgrade (Member).
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    activeUserProvider: ActiveUserProvider,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = activeUserProvider.observeActiveUser()
        .let { userFlow ->
            kotlinx.coroutines.flow.flow {
                userFlow.collect { user ->
                    emit(user.toHomeUiState())
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = HomeUiState.Loading,
        )

    private val _upgradeDialogVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val upgradeDialogVisible: StateFlow<Boolean> = _upgradeDialogVisible.asStateFlow()

    /** Sinaliza que o Member tocou no FAB desabilitado. */
    fun onMemberFabClicked() {
        _upgradeDialogVisible.value = true
    }

    /** Fecha o diálogo de upgrade após o usuário tocar em "Entendi". */
    fun dismissUpgradeDialog() {
        _upgradeDialogVisible.value = false
    }

    private fun User?.toHomeUiState(): HomeUiState = when (this) {
        null -> HomeUiState.SignedOut
        else -> HomeUiState.SignedIn(
            displayName = name,
            initials = computeInitials(name),
            role = role.toHomeRole(),
        )
    }
}

/** Estado da [HomeScreen] derivado do [User] ativo. */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object SignedOut : HomeUiState
    data class SignedIn(
        val displayName: String,
        val initials: String,
        val role: HomeUserRole,
    ) : HomeUiState
}

/** Mapeia [pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole]
 *  para a enumeração visual [HomeUserRole]. */
internal fun pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole.toHomeRole(): HomeUserRole =
    when (this) {
        pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole.OWNER -> HomeUserRole.Owner
        pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole.MEMBER -> HomeUserRole.Member
    }

/**
 * Gâce o monograma do nome. Quando o nome tem mais de uma palavra,
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
