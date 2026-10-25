// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel

import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.MIN_PASSWORD_LENGTH

// Regras de validação dos formulários de login e cadastro.
//
// Ficam fora do [AuthViewModel] de propósito: são regras **de campo**,
// sem efeito colateral — não tocam o estado nem disparam evento. O
// ViewModel orquestra a submissão; estas funções apenas dizem qual
// campo está inválido.
//
// A validação autoritativa de e-mail continua sendo a do
// `User.requireValidEmail` no caso de uso — aqui só se decide se a
// submissão deve prosseguir ou se o erro aparece inline no campo.

/**
 * Valida o formulário de login.
 *
 * O primeiro campo inválido na ordem (e-mail, senha) define o erro
 * exibido.
 */
internal fun validateLoginFields(state: AuthUiState): FieldErrors {
    val emailError = validateEmailFormat(state.email)
    val passwordError = validatePasswordLength(state.password)
    return when {
        emailError != null -> FieldErrors(email = emailError)
        passwordError != null -> FieldErrors(password = passwordError)
        else -> FieldErrors()
    }
}

/**
 * Mesma regra de [validateLoginFields] para o formulário de cadastro,
 * que acrescenta nome e confirmação de senha. O primeiro campo inválido
 * na ordem (nome, e-mail, senha, confirmação) define o erro exibido.
 */
internal fun validateRegisterFields(state: AuthUiState): FieldErrors {
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

/** E-mail vazio ou fora do formato aceito pelo domínio. */
internal fun validateEmailFormat(email: String): AuthErrorType? {
    val trimmed = email.trim()
    return when {
        trimmed.isEmpty() -> AuthErrorType.EmptyEmail
        !User.EMAIL_REGEX.matches(trimmed) -> AuthErrorType.InvalidEmail
        else -> null
    }
}

/** Senha vazia ou mais curta que o mínimo exigido pelo domínio. */
internal fun validatePasswordLength(password: String): AuthErrorType? =
    when {
        password.isEmpty() -> AuthErrorType.EmptyPassword
        password.length < MIN_PASSWORD_LENGTH -> AuthErrorType.ShortPassword
        else -> null
    }

/** Nome vazio ou mais longo que o máximo aceito pelo domínio. */
internal fun validateName(name: String): AuthErrorType? {
    val trimmed = name.trim()
    return when {
        trimmed.isEmpty() -> AuthErrorType.EmptyName
        trimmed.length > User.MAX_NAME_LENGTH -> AuthErrorType.LongName
        else -> null
    }
}

/** Confirmação vazia ou diferente da senha informada. */
internal fun validatePasswordConfirmation(
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
 * A validação devolve **o erro já no campo a que pertence**, o que
 * dispensa uma segunda passagem para redistribuí-lo.
 */
internal data class FieldErrors(
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
