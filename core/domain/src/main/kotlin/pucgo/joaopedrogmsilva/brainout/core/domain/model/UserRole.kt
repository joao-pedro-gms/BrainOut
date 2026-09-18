// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.model

/**
 * Papel de um [User] dentro do BrainOut.
 *
 * - [Owner]: papel administrativo, possui todas as permissões. Indicado
 *   para o criador de um [Project].
 * - [Member]: papel regular, possui permissões limitadas. Pode
 *   visualizar e trabalhar em tarefas, mas não administra o projeto.
 *
 * A lista concreta de permissões concedida por cada papel é exposta via
 * [permissions]. A checagem em tempo de execução é responsabilidade de
 * [pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CanPerformActionUseCase].
 */
enum class UserRole(val permissions: Set<Permission>) {
    OWNER(
        setOf(
            Permission.CREATE_PROJECT,
            Permission.EDIT_PROJECT,
            Permission.DELETE_PROJECT,
            Permission.CREATE_TASK,
            Permission.EDIT_TASK,
            Permission.DELETE_TASK,
            Permission.INVITE_MEMBER,
        ),
    ),
    MEMBER(
        setOf(
            Permission.CREATE_TASK,
            Permission.EDIT_TASK,
        ),
    ),
    ;

    /** Indica se o papel concede a [permission] informada. */
    fun hasPermission(permission: Permission): Boolean = permissions.contains(permission)
}
