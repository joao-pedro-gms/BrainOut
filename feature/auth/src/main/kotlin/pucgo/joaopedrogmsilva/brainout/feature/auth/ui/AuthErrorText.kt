// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import pucgo.joaopedrogmsilva.brainout.feature.auth.R
import pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel.AuthErrorType

/**
 * Texto localizado de um [AuthErrorType].
 *
 * O ViewModel emite o **tipo** do erro; a escolha do texto é feita
 * aqui, na camada de UI — assim o mesmo erro aparece em pt/en sem
 * traduzir mensagem técnica nem duplicar texto na ViewModel.
 */
@Composable
internal fun AuthErrorType.text(): String = stringResource(messageRes())

/** Recurso de string que descreve este erro. */
internal fun AuthErrorType.messageRes(): Int =
    when (this) {
        AuthErrorType.EmptyUsername -> R.string.auth_error_empty_username
        AuthErrorType.InvalidUsername -> R.string.auth_error_invalid_username
        AuthErrorType.EmptyPassword -> R.string.auth_error_empty_pwd
        AuthErrorType.ShortPassword -> R.string.auth_error_short_pwd
        AuthErrorType.InvalidCredentials -> R.string.auth_error_invalid_credentials
        AuthErrorType.DuplicateUsername -> R.string.auth_error_duplicate_username
        AuthErrorType.EmptyPasswordConfirmation -> R.string.auth_error_empty_pwd_confirmation
        AuthErrorType.PasswordMismatch -> R.string.auth_error_pwd_mismatch
        AuthErrorType.DomainRejected -> R.string.auth_error_domain
        AuthErrorType.Unexpected -> R.string.auth_error_unexpected
    }
