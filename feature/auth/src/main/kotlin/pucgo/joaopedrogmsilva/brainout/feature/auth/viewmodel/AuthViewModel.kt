// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pucgo.joaopedrogmsilva.brainout.core.data.session.SessionStore
import pucgo.joaopedrogmsilva.brainout.core.data.util.maskEmail
import pucgo.joaopedrogmsilva.brainout.core.domain.error.DomainException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.DuplicateEmailException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidCredentialsException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.AuthenticateUserUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CreateUserUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.MIN_PASSWORD_LENGTH
import javax.inject.Inject

private const val TAG = "BrainOut:AuthVM"

/**
 * ViewModel único para os fluxos de Login e Cadastro (E1.6).
 *
 * Mantém um único `StateFlow<AuthUiState>` que tanto a `LoginScreen`
 * quanto a `RegisterScreen` consomem — os formulários são distintos,
 * mas o estado é comum para evitar duplicação de regras de validação.
 *
 * Após login/cadastro bem-sucedido, o id é persistido em [SessionStore]
 * para que `MainActivity` possa restaurar a sessão no próximo start
 * (E1.8). A navegação é disparada pelo evento [AuthEvent.NavigateHome];
 * o estado **não** carrega um sinal de sucesso redundante.
 *
 * A persistência da sessão é responsabilidade do ViewModel porque ele
 * é o único ponto que conhece o `User` recém-criado/autenticado.
 */
@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        private val createUserUseCase: CreateUserUseCase,
        private val authenticateUserUseCase: AuthenticateUserUseCase,
        private val sessionStore: SessionStore,
    ) : ViewModel() {
        private val _state: MutableStateFlow<AuthUiState> = MutableStateFlow(AuthUiState())
        val state: StateFlow<AuthUiState> = _state.asStateFlow()

        private val _events = Channel<AuthEvent>(capacity = Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        // --- Field updates --------------------------------------------------

        fun onEmailChange(value: String) {
            _state.update { it.copy(email = value, emailError = null, errorMessage = null) }
        }

        fun onPasswordChange(value: String) {
            _state.update {
                it.copy(
                    password = value,
                    passwordError = null,
                    errorMessage = null,
                )
            }
        }

        fun onNameChange(value: String) {
            _state.update { it.copy(name = value, nameError = null, errorMessage = null) }
        }

        fun onPasswordConfirmationChange(value: String) {
            _state.update {
                it.copy(
                    passwordConfirmation = value,
                    passwordConfirmationError = null,
                    errorMessage = null,
                )
            }
        }

        fun onRoleChange(role: UserRole) {
            _state.update { it.copy(selectedRole = role) }
        }

        // --- Submission -----------------------------------------------------

        /**
         * Valida o estado atual e, se estiver OK, executa o caso de uso
         * de login. Em sucesso, persiste o id no [SessionStore] e emite o
         * evento [AuthEvent.NavigateHome].
         */
        fun submitLogin() {
            val current = _state.value
            if (current.isLoading) return
            Log.d(TAG, "Submetendo login para e-mail: ${current.email.maskEmail()}")
            val errors = validateLoginFields(current)
            if (errors.hasError) {
                Log.w(TAG, "Validação de login falhou: ${errors.failedFields}")
                // `errorMessage` (banner) — o erro já aparece no campo.
                // O banner fica reservado para erros de domínio não-campo.
                _state.update {
                    it.copy(
                        emailError = errors.email,
                        passwordError = errors.password,
                        isLoading = false,
                    )
                }
                return
            }
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            viewModelScope.launch {
                try {
                    val user: User =
                        authenticateUserUseCase(
                            email = current.email,
                            rawPassword = current.password,
                        )
                    Log.d(TAG, "Autenticação bem-sucedida para userId=${user.id}")
                    persistSessionAndNavigate(user)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: InvalidCredentialsException) {
                    Log.w(TAG, "Credenciais inválidas para o e-mail: ${current.email.maskEmail()}")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            passwordError = AuthErrorType.InvalidCredentials,
                        )
                    }
                } catch (error: DomainException) {
                    Log.w(TAG, "Exceção de domínio em submitLogin: ${error.message}")
                    _state.update {
                        it.copy(isLoading = false, errorMessage = AuthErrorType.DomainRejected)
                    }
                } catch (error: Exception) {
                    // Exceções inesperadas (ex.: IOException, SQLite) não devem
                    // propagar para a UI; exibimos uma mensagem canônica e
                    // logamos a stack trace para diagnóstico.
                    Log.e(TAG, "Falha inesperada em submitLogin", error)
                    _state.update {
                        it.copy(isLoading = false, errorMessage = AuthErrorType.Unexpected)
                    }
                }
            }
        }

        /**
         * Valida o estado atual e, se estiver OK, executa o caso de uso de
         * cadastro. Em sucesso, persiste o id no [SessionStore] e emite
         * [AuthEvent.NavigateHome].
         */
        fun submitRegister() {
            val current = _state.value
            if (current.isLoading) return
            Log.d(TAG, "Submetendo cadastro com role=${current.selectedRole}, e-mail=${current.email.maskEmail()}")
            val errors = validateRegisterFields(current)
            if (errors.hasError) {
                Log.w(TAG, "Validação de cadastro falhou: ${errors.failedFields}")
                // para o campo; o banner fica para erros de domínio não-campo.
                _state.update {
                    it.copy(
                        nameError = errors.name,
                        emailError = errors.email,
                        passwordError = errors.password,
                        passwordConfirmationError = errors.passwordConfirmation,
                        isLoading = false,
                    )
                }
                return
            }
            // Marca `isLoading` sincronamente antes de despachar a coroutine
            // — assim uma segunda chamada de `submitRegister` é bloqueada
            // mesmo antes do `viewModelScope.launch` rodar (importante em
            // testes com `StandardTestDispatcher`, onde o dispatch é
            // preguiçoso).
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            viewModelScope.launch {
                try {
                    val user: User =
                        createUserUseCase(
                            name = current.name,
                            email = current.email,
                            rawPassword = current.password,
                            role = current.selectedRole,
                        )
                    Log.d(TAG, "Cadastro bem-sucedido para userId=${user.id}")
                    persistSessionAndNavigate(user)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: DuplicateEmailException) {
                    Log.w(TAG, "E-mail duplicado ao cadastrar: ${current.email.maskEmail()}")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            emailError = AuthErrorType.DuplicateEmail,
                        )
                    }
                } catch (error: DomainException) {
                    Log.w(TAG, "Exceção de domínio em submitRegister: ${error.message}")
                    _state.update {
                        it.copy(isLoading = false, errorMessage = AuthErrorType.DomainRejected)
                    }
                } catch (error: Exception) {
                    Log.e(TAG, "Falha inesperada em submitRegister", error)
                    _state.update {
                        it.copy(isLoading = false, errorMessage = AuthErrorType.Unexpected)
                    }
                }
            }
        }

        // --- Internals ------------------------------------------------------

        private suspend fun persistSessionAndNavigate(user: User) {
            sessionStore.saveUserId(user.id)
            _state.update { it.copy(isLoading = false) }
            _events.send(AuthEvent.NavigateHome)
        }

        /**
         * Validação leve (apenas o que pode ser decidido client-side).
         * A validação autoritativa de e-mail continua sendo a do
         * `User.requireValidEmail` no caso de uso — esta função apenas
         * decide se a submissão deve prosseguir ou se devemos exibir
         * erro inline.
         */
        private fun validateLoginFields(state: AuthUiState): FieldErrors {
            val emailError = validateEmailFormat(state.email)
            val passwordError = validatePasswordLength(state.password)
            return when {
                emailError != null -> FieldErrors(email = emailError)
                passwordError != null -> FieldErrors(password = passwordError)
                else -> FieldErrors()
            }
        }

        /**
         * Mesma regra de [validateLoginFields] para o formulário de
         * cadastro, que acrescenta nome e confirmação de senha. O
         * primeiro campo inválido na ordem (nome, e-mail, senha,
         * confirmação) define o erro exibido.
         */
        private fun validateRegisterFields(state: AuthUiState): FieldErrors {
            val nameError = validateName(state.name)
            val emailError = validateEmailFormat(state.email)
            val passwordError = validatePasswordLength(state.password)
            val confirmationError = validatePasswordConfirmation(state.password, state.passwordConfirmation)
            return when {
                nameError != null -> FieldErrors(name = nameError)
                emailError != null -> FieldErrors(email = emailError)
                passwordError != null -> FieldErrors(password = passwordError)
                confirmationError != null -> FieldErrors(passwordConfirmation = confirmationError)
                else -> FieldErrors()
            }
        }

        private fun validateEmailFormat(email: String): AuthErrorType? {
            val trimmed = email.trim()
            return when {
                trimmed.isEmpty() -> AuthErrorType.EmptyEmail
                !User.EMAIL_REGEX.matches(trimmed) -> AuthErrorType.InvalidEmail
                else -> null
            }
        }

        private fun validatePasswordLength(password: String): AuthErrorType? =
            when {
                password.isEmpty() -> AuthErrorType.EmptyPassword
                password.length < MIN_PASSWORD_LENGTH -> AuthErrorType.ShortPassword
                else -> null
            }

        private fun validateName(name: String): AuthErrorType? {
            val trimmed = name.trim()
            return when {
                trimmed.isEmpty() -> AuthErrorType.EmptyName
                trimmed.length > User.MAX_NAME_LENGTH -> AuthErrorType.LongName
                else -> null
            }
        }

        private fun validatePasswordConfirmation(
            password: String,
            confirmation: String,
        ): AuthErrorType? =
            when {
                confirmation.isEmpty() -> AuthErrorType.EmptyPasswordConfirmation
                password != confirmation -> AuthErrorType.PasswordMismatch
                else -> null
            }

        /**
         * Erros de validação por campo — `null` significa "campo válido".
         *
         * A validação devolve **o erro já no campo a que pertence**, o
         * que dispensa uma segunda passagem para redistribuí-lo.
         */
        private data class FieldErrors(
            val name: AuthErrorType? = null,
            val email: AuthErrorType? = null,
            val password: AuthErrorType? = null,
            val passwordConfirmation: AuthErrorType? = null,
        ) {
            val hasError: Boolean
                get() = name != null || email != null || password != null || passwordConfirmation != null

            /** Campos com erro — usado apenas no log de diagnóstico. */
            val failedFields: String
                get() =
                    listOfNotNull(
                        name?.let { "name" },
                        email?.let { "email" },
                        password?.let { "password" },
                        passwordConfirmation?.let { "passwordConfirmation" },
                    ).joinToString(",")
        }
    }

/**
 * Eventos one-shot que a tela deve consumir (efeitos colaterais que
 * não cabem em [AuthUiState]).
 */
sealed interface AuthEvent {
    /** Dispara a navegação para a tela inicial. */
    data object NavigateHome : AuthEvent
}
