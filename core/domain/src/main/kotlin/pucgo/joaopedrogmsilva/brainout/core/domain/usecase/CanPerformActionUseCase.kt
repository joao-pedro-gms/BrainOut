// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import pucgo.joaopedrogmsilva.brainout.core.domain.model.Permission
import pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole

/**
 * Caso de uso que verifica se um [UserRole] possui uma [Permission].
 *
 * Implementação pura: nenhuma dependência de framework, IO ou
 * estado global. Idempotente e adequada para ser invocada em ViewModels.
 *
 * Exemplo de uso:
 * ```
 * val canDelete = canPerformAction(UserRole.MEMBER, Permission.DELETE_PROJECT)
 * // -> false
 * ```
 */
class CanPerformActionUseCase {

    /**
     * @param role Papel do usuário.
     * @param permission Permissão a verificar.
     * @return `true` se [role] concede [permission]; `false` caso contrário.
     */
    operator fun invoke(role: UserRole, permission: Permission): Boolean =
        role.hasPermission(permission)
}
