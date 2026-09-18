// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.model

/**
 * Ações que podem ser executadas contra um [Project] ou um [Task].
 *
 * Cada [UserRole] declara a lista de permissões que concede ao usuário.
 * A checagem efetiva é feita por [pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CanPerformActionUseCase].
 *
 * Marco E1.4 — modelagem inicial. Mais permissões (ex.: gerenciamento de
 * papéis, leitura de relatórios) podem entrar em ciclos futuros.
 */
enum class Permission {
    CREATE_PROJECT,
    EDIT_PROJECT,
    DELETE_PROJECT,
    CREATE_TASK,
    EDIT_TASK,
    DELETE_TASK,
    INVITE_MEMBER,
}
