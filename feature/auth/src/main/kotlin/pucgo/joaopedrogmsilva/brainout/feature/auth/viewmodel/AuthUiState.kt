// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel

/** Estado dos formulários de identidade local. */
data class AuthUiState(
    val username: String = "",
    val password: String = "",
    val passwordConfirmation: String = "",
    val isLoading: Boolean = false,
    val usernameError: AuthErrorType? = null,
    val passwordError: AuthErrorType? = null,
    val passwordConfirmationError: AuthErrorType? = null,
    val errorMessage: AuthErrorType? = null,
)
