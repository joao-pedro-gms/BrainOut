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
            launchSubmission(
                operation = "submitLogin",
                fieldError = { error ->
                    if (error is InvalidCredentialsException) {
                        Log.w(TAG, "Credenciais inválidas para o e-mail: ${current.email.maskEmail()}")
                        AuthFieldError(AuthField.Password, AuthErrorType.InvalidCredentials)
                    } else {
                        null
                    }
                },
            ) {
                val user =
                    authenticateUserUseCase(
                        email = current.email,
                        rawPassword = current.password,
                    )
                Log.d(TAG, "Autenticação bem-sucedida para userId=${user.id}")
                user
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
            launchSubmission(
                operation = "submitRegister",
                fieldError = { error ->
                    if (error is DuplicateEmailException) {
                        Log.w(TAG, "E-mail duplicado ao cadastrar: ${current.email.maskEmail()}")
                        AuthFieldError(AuthField.Email, AuthErrorType.DuplicateEmail)
                    } else {
                        null
                    }
                },
            ) {
                val user =
                    createUserUseCase(
                        name = current.name,
                        email = current.email,
                        rawPassword = current.password,
                        role = current.selectedRole,
                    )
                Log.d(TAG, "Cadastro bem-sucedido para userId=${user.id}")
                user
            }
        }

        // --- Internals ------------------------------------------------------

        private suspend fun persistSessionAndNavigate(user: User) {
            sessionStore.saveUserId(user.id)
            _state.update { it.copy(isLoading = false) }
            _events.send(AuthEvent.NavigateHome)
        }

        /**
         * Executa a submissão e traduz a falha em erro de UI.
         *
         * Login e cadastro compartilham o mesmo tratamento: o
         * cancelamento propaga, o erro de domínio vira
         * [AuthErrorType.DomainRejected] e a falha inesperada vira
         * [AuthErrorType.Unexpected]. O que muda entre os dois fluxos é
         * **qual** erro de domínio pertence a um campo — daí o
         * parâmetro [fieldError], que devolve `null` para os erros que
         * não são de campo (e que então viram erro de banner).
         *
         * O [operation] identifica o fluxo nos logs de diagnóstico.
         */
        private fun launchSubmission(
            operation: String,
            fieldError: (DomainException) -> AuthFieldError?,
            block: suspend () -> User,
        ) {
            viewModelScope.launch {
                try {
                    val user = block()
                    persistSessionAndNavigate(user)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: DomainException) {
                    val mapped = fieldError(error)
                    if (mapped == null) {
                        Log.w(TAG, "Exceção de domínio em $operation: ${error.message}")
                        _state.update {
                            it.copy(isLoading = false, errorMessage = AuthErrorType.DomainRejected)
                        }
                    } else {
                        _state.update { it.withFieldError(mapped) }
                    }
                } catch (error: Exception) {
                    // Exceções inesperadas (ex.: IOException, SQLite) não devem
                    // propagar para a UI; exibimos uma mensagem canônica e
                    // logamos a stack trace para diagnóstico.
                    Log.e(TAG, "Falha inesperada em $operation", error)
                    _state.update {
                        it.copy(isLoading = false, errorMessage = AuthErrorType.Unexpected)
                    }
                }
            }
        }
    }

/**
 * Campo do formulário que pode receber um erro vindo do domínio.
 */
private enum class AuthField {
    Name,
    Email,
    Password,
}

/**
 * Erro de domínio que pertence a um campo do formulário.
 *
 * @property field onde o erro é exibido.
 * @property type o erro mostrado no campo.
 */
private data class AuthFieldError(
    val field: AuthField,
    val type: AuthErrorType,
)

/**
 * Aplica o erro no campo correspondente e libera o formulário.
 *
 * A liberação do [AuthUiState.isLoading] faz parte do tratamento: um
 * erro de campo **é** a resposta da submissão — o formulário volta a
 * aceitar edição.
 */
private fun AuthUiState.withFieldError(error: AuthFieldError): AuthUiState =
    when (error.field) {
        AuthField.Name -> copy(nameError = error.type, isLoading = false)
        AuthField.Email -> copy(emailError = error.type, isLoading = false)
        AuthField.Password -> copy(passwordError = error.type, isLoading = false)
    }

/**
 * Eventos one-shot que a tela deve consumir (efeitos colaterais que
 * não cabem em [AuthUiState]).
 */
sealed interface AuthEvent {
    /** Dispara a navegação para a tela inicial. */
    data object NavigateHome : AuthEvent
}
