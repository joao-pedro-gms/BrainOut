// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.permission

import pucgo.joaopedrogmsilva.brainout.core.domain.error.PermissionDeniedException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.UserNotFoundException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Permission
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ActiveUserPort

/**
 * Exige que o usuário ativo tenha a [permission] exigida pela operação.
 *
 * É o **único** ponto onde a matriz `UserRole`/`Permission` é
 * consultada em produção — antes a matriz existia e tinha teste, mas
 * nenhuma mutação a olhava (o `CanPerformActionUseCase` foi removido
 * por não ter caller; a regra voltou aqui, dentro do domínio, onde a
 * mutação acontece).
 *
 * Usado pelos casos de uso destrutivos (`DeleteProjectUseCase`,
 * `DeleteTaskUseCase`, `UpdateProjectUseCase`): esconder o botão na
 * UI é defesa secundária, não a correção — a regra tem de valer
 * mesmo que alguém chame o caso de uso direto.
 */
internal suspend fun ActiveUserPort.requirePermission(permission: Permission) {
    val user = currentUser() ?: throw UserNotFoundException()
    if (!user.role.hasPermission(permission)) {
        throw PermissionDeniedException(role = user.role, permission = permission)
    }
}
