// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pucgo.joaopedrogmsilva.brainout.core.data.session.SessionStore
import pucgo.joaopedrogmsilva.brainout.core.domain.error.DomainException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.DuplicateEmailException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidCredentialsException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.AuthenticateUserUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CreateUserUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.MIN_PASSWORD_LENGTH

/**
 * ViewModel único para os fluxos de Login e Cadastro (E1.6).
 *
 * Mantém um único `StateFlow<AuthUiState>` que tanto a `LoginScreen`
 * quanto a `RegisterScreen` consomem — os formulários são distintos,
 * mas o estado é comum para evitar duplicação de regras de validação.
 *
 * Após login/cadastro bem-sucedido, o id é persistido em [SessionStore]
 * para que `MainActivity` possa restaurar a sessão no próximo start
 * (E1.8).
 *
 * A persistência da sessão é responsabilidade do ViewModel porque ele
 * é o único ponto que conhece o `User` recém-criado/autenticado. A
 * tela apenas observa `state.success` e navega.
 */
@Suppress("TooManyFunctions", "SwallowedException")
@HiltViewModel
class AuthViewModel @Inject constructor(
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
        val errors = validateLoginFields(current)
        if (errors != null) {
            val (emailErrorValue, passwordErrorValue) = splitLoginErrors(errors)
            // `errorMessage` (banner) — o erro já aparece no campo.
            // O banner fica reservado para erros de domínio não-campo.
            _state.update {
                it.copy(
                    emailError = emailErrorValue,
                    passwordError = passwordErrorValue,
                    isLoading = false,
                )
            }
            return
        }
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val user: User = authenticateUserUseCase(
                    email = current.email,
                    rawPassword = current.password,
                )
                persistSessionAndNavigate(user)
            } catch (error: InvalidCredentialsException) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        passwordError = INVALID_CREDENTIALS_MESSAGE,
                    )
                }
            } catch (error: DomainException) {
                _state.update { it.copy(isLoading = false, errorMessage = error.message) }
            } catch (@Suppress("TooGenericExceptionCaught") error: Throwable) {
                // Exceções inesperadas (ex.: IOException, SQLite) não devem
                // propagar para a UI; exibimos uma mensagem canônica e
                // logamos a stack trace para diagnóstico.
                @Suppress("SwallowedException")
                val swallowed = error
                android.util.Log.e(
                    "AuthViewModel",
                    "Falha inesperada em submitLogin",
                    swallowed,
                )
                _state.update {
                    it.copy(isLoading = false, errorMessage = UNEXPECTED_ERROR_MESSAGE)
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
        val errors = validateRegisterFields(current)
        if (errors != null) {
            val split = splitRegisterErrors(errors)
            // para o campo; o banner fica para erros de domínio não-campo.
            _state.update {
                it.copy(
                    nameError = split.name,
                    emailError = split.email,
                    passwordError = split.password,
                    passwordConfirmationError = split.passwordConfirmation,
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
                val user: User = createUserUseCase(
                    name = current.name,
                    email = current.email,
                    rawPassword = current.password,
                    role = current.selectedRole,
                )
                persistSessionAndNavigate(user)
            } catch (error: DuplicateEmailException) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        emailError = DUPLICATE_EMAIL_MESSAGE,
                    )
                }
            } catch (error: DomainException) {
                _state.update { it.copy(isLoading = false, errorMessage = error.message) }
            } catch (@Suppress("TooGenericExceptionCaught") error: Throwable) {
                android.util.Log.e(
                    "AuthViewModel",
                    "Falha inesperada em submitRegister",
                    error,
                )
                _state.update {
                    it.copy(isLoading = false, errorMessage = UNEXPECTED_ERROR_MESSAGE)
                }
            }
        }
    }

    /**
     * Limpa o estado. Útil quando a tela é desfeita (logout) ou quando
     * o usuário volta da tela de Cadastro para Login (evita que o nome
     * digitado persista na próxima abertura da tela de Login).
     */
    fun reset() {
        _state.value = AuthUiState()
    }

    // --- Internals ------------------------------------------------------

    private suspend fun persistSessionAndNavigate(user: User) {
        sessionStore.saveUserId(user.id)
        _state.update { it.copy(isLoading = false, success = true) }
        _events.send(AuthEvent.NavigateHome)
    }

    /**
     * Validação leve (apenas o que pode ser decidido client-side).
     * A validação autoritativa de e-mail continua sendo a do
     * `User.requireValidEmail` no caso de uso — esta função apenas
     * decide se a submissão deve prosseguir ou se devemos exibir
     * erro inline.
     *
     * @return um [LoginValidationResult] carregando a mensagem geral
     *  e o [firstInvalidField], ou `null` se o estado é válido.
     */
    @Suppress("ReturnCount")
    private fun validateLoginFields(state: AuthUiState): LoginValidationResult? {
        val emailError = validateEmailFormat(state.email)
        if (emailError != null) {
            return LoginValidationResult(emailError, AuthField.Email)
        }
        val passwordError = validatePasswordLength(state.password)
        if (passwordError != null) {
            return LoginValidationResult(passwordError, AuthField.Password)
        }
        return null
    }

    /**
     * Dado o resultado da validação, deriva os pares `(campo, mensagem)`
     * para popular os campos específicos do estado. Usado por
     * [submitLogin] para re-aplicar a mensagem no campo certo depois
     * que `validateLoginFields` decidiu onde está o erro.
     */
    private fun splitLoginErrors(
        errors: LoginValidationResult,
    ): Pair<String?, String?> = when (errors.firstInvalidField) {
        AuthField.Email -> errors.general to null
        AuthField.Password -> null to errors.general
        else -> null to null
    }

    @Suppress("ReturnCount")
    private fun validateRegisterFields(state: AuthUiState): LoginValidationResult? {
        val nameError = validateName(state.name)
        if (nameError != null) {
            return LoginValidationResult(nameError, AuthField.Name)
        }
        val emailError = validateEmailFormat(state.email)
        if (emailError != null) {
            return LoginValidationResult(emailError, AuthField.Email)
        }
        val passwordError = validatePasswordLength(state.password)
        if (passwordError != null) {
            return LoginValidationResult(passwordError, AuthField.Password)
        }
        val confirmationError = validatePasswordConfirmation(state.password, state.passwordConfirmation)
        if (confirmationError != null) {
            return LoginValidationResult(confirmationError, AuthField.PasswordConfirmation)
        }
        return null
    }

    private fun splitRegisterErrors(
        errors: LoginValidationResult,
    ): RegisterErrorSplit = when (errors.firstInvalidField) {
        AuthField.Name -> RegisterErrorSplit(name = errors.general)
        AuthField.Email -> RegisterErrorSplit(email = errors.general)
        AuthField.Password -> RegisterErrorSplit(password = errors.general)
        AuthField.PasswordConfirmation -> RegisterErrorSplit(passwordConfirmation = errors.general)
        else -> RegisterErrorSplit()
    }

    @Suppress("ReturnCount")
    private fun validateEmailFormat(email: String): String? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> EMPTY_EMAIL_MESSAGE
            !User.EMAIL_REGEX.matches(trimmed) -> INVALID_EMAIL_MESSAGE
            else -> null
        }
    }

    @Suppress("ReturnCount")
    private fun validatePasswordLength(password: String): String? = when {
        password.isEmpty() -> EMPTY_PASSWORD_MESSAGE
        password.length < MIN_PASSWORD_LENGTH -> SHORT_PASSWORD_MESSAGE
        else -> null
    }

    @Suppress("ReturnCount")
    private fun validateName(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> EMPTY_NAME_MESSAGE
            trimmed.length > User.MAX_NAME_LENGTH -> LONG_NAME_MESSAGE
            else -> null
        }
    }

    @Suppress("ReturnCount")
    private fun validatePasswordConfirmation(
        password: String,
        confirmation: String,
    ): String? = when {
        confirmation.isEmpty() -> EMPTY_PASSWORD_CONFIRMATION_MESSAGE
        password != confirmation -> PASSWORD_MISMATCH_MESSAGE
        else -> null
    }

    /**
     * Resultado de uma validação que falhou. Carrega a mensagem geral
     * (a ser exibida em destaque) e o primeiro campo inválido (para
     * mover o foco).
     */
    private data class LoginValidationResult(
        val general: String,
        val firstInvalidField: AuthField,
    )

    companion object {
        // Mensagens embutidas no ViewModel em vez de strings.xml —
        // são frases de validação técnica que não dependem de
        // localização para funcionar; a camada de UI substitui se
        // necessário. Manter aqui evita inflar `Context` em testes
        // JVM e simplifica o contrato do ViewModel.
        const val EMPTY_EMAIL_MESSAGE: String = "Informe seu e-mail."
        const val INVALID_EMAIL_MESSAGE: String = "E-mail inválido."
        const val EMPTY_PASSWORD_MESSAGE: String = "Informe sua senha."
        const val SHORT_PASSWORD_MESSAGE: String =
            "A senha deve ter pelo menos ${MIN_PASSWORD_LENGTH} caracteres."
        const val INVALID_CREDENTIALS_MESSAGE: String =
            "E-mail ou senha incorretos."
        const val DUPLICATE_EMAIL_MESSAGE: String =
            "Já existe uma conta cadastrada com este e-mail."
        const val EMPTY_NAME_MESSAGE: String = "Informe seu nome."
        const val LONG_NAME_MESSAGE: String =
            "O nome deve ter no máximo ${User.MAX_NAME_LENGTH} caracteres."
        const val EMPTY_PASSWORD_CONFIRMATION_MESSAGE: String =
            "Confirme sua senha."
        const val PASSWORD_MISMATCH_MESSAGE: String =
            "As senhas não coincidem."
        const val UNEXPECTED_ERROR_MESSAGE: String =
            "Não foi possível concluir a operação. Tente novamente."
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

/** Identifica um campo do formulário para focar / sinalizar erro. */
enum class AuthField { Email, Password, Name, PasswordConfirmation }

/**
 * Resultado da [pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel.AuthViewModel.splitRegisterErrors]
 * — distribui a mensagem de erro de validação para o campo correto
 * (name, email, password ou passwordConfirmation).
 */
internal data class RegisterErrorSplit(
    val name: String? = null,
    val email: String? = null,
    val password: String? = null,
    val passwordConfirmation: String? = null,
)
