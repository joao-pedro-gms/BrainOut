// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.auth.viewmodel

import pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole

/**
 * Estado de UI dos formulários de Login e Cadastro (E1.6 do ROADMAP).
 *
 * @property email texto digitado no campo e-mail.
 * @property password texto digitado no campo senha.
 * @property name texto digitado no campo nome (somente Cadastro).
 * @property passwordConfirmation texto digitado no campo "confirmar
 *  senha" (somente Cadastro).
 * @property selectedRole papel selecionado durante o Cadastro (Owner/Member).
 * @property isLoading indica que uma submissão está em andamento
 *  (desabilita os controles para evitar duplo submit).
 * @property emailError erro específico do e-mail (`null` quando o valor
 *  atual é válido).
 * @property passwordError erro específico da senha (`null` quando válida).
 * @property nameError erro específico do nome (`null` quando válido).
 * @property passwordConfirmationError erro específico da confirmação de
 *  senha (`null` quando válida).
 * @property errorMessage erro geral do fluxo (credenciais inválidas,
 *  e-mail duplicado, falha de IO). É exibido em destaque na tela.
 */
data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val passwordConfirmation: String = "",
    val selectedRole: UserRole = UserRole.OWNER,
    val isLoading: Boolean = false,
    val emailError: AuthErrorType? = null,
    val passwordError: AuthErrorType? = null,
    val nameError: AuthErrorType? = null,
    val passwordConfirmationError: AuthErrorType? = null,
    val errorMessage: AuthErrorType? = null,
)
