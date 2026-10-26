// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel

/**
 * Erros do fluxo de autenticação classificados para a camada de
 * apresentação.
 *
 * A classificação acontece no [AuthViewModel] (que conhece as exceções
 * de domínio) e o texto é escolhido na tela — é o que garante que o
 * erro apareça em pt/en e que nenhuma mensagem técnica do domínio
 * vaze para a interface.
 */
enum class AuthErrorType {
    /** Campo de e-mail vazio. */
    EmptyUsername,

    /** E-mail fora do formato válido. */
    InvalidUsername,

    /** Campo de senha vazio. */
    EmptyPassword,

    /** Senha mais curta que o mínimo exigido pelo domínio. */
    ShortPassword,

    /** E-mail ou senha não conferem. */
    InvalidCredentials,

    /** Já existe uma conta cadastrada com este e-mail. */
    DuplicateUsername,

    /** Confirmação de senha vazia. */
    EmptyPasswordConfirmation,

    /** Senha e confirmação não coincidem. */
    PasswordMismatch,

    /** O domínio recusou os dados informados. */
    DomainRejected,

    /** Falha inesperada (IO, banco, etc.). */
    Unexpected,
}
