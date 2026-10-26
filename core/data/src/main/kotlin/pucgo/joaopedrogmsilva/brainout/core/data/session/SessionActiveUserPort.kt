// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.session

import pucgo.joaopedrogmsilva.brainout.core.domain.error.OwnershipException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ActiveUserPort
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementação de [ActiveUserPort] sobre a sessão persistida.
 *
 * A origem da verdade é o [ActiveUserProvider], que já combina o
 * `SessionStore` (DataStore) com o `UserRepository` (Room) e sabe
 * limpar sessão órfã. Aqui só o ponto é exposto ao domínio — a
 * camada de dados não conhece regra de permissão; ela só informa
 * **quem** está logado, e o domínio decide o que essa pessoa pode
 * fazer.
 */
@Singleton
class SessionActiveUserPort
    @Inject
    constructor(
        private val activeUserProvider: ActiveUserProvider,
        private val session: SessionStore,
    ) : ActiveUserPort {
        override suspend fun currentUser(): User? = activeUserProvider.currentActiveUser()

        override suspend fun runIfCurrent(
            userId: String,
            action: () -> Unit,
        ): Boolean =
            try {
                session.withUser(userId) {
                    action()
                    true
                }
            } catch (error: OwnershipException) {
                false
            }
    }
