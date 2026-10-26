// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.repository

import pucgo.joaopedrogmsilva.brainout.core.domain.model.User

/** Identidade ativa e fronteira atômica para efeitos locais vinculados à sessão. */
interface ActiveUserPort {
    /**
     * Usuário autenticado no momento, ou `null` quando não há sessão.
     */
    suspend fun currentUser(): User?

    /** Executa efeitos síncronos somente enquanto a conta capturada permanece ativa. */
    suspend fun runIfCurrent(
        userId: String,
        action: () -> Unit,
    ): Boolean
}
