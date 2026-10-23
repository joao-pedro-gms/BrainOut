// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.session

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
    ) : ActiveUserPort {
        override suspend fun currentUser(): User? = activeUserProvider.currentActiveUser()
    }
