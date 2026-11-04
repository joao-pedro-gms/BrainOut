// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.repository

import pucgo.joaopedrogmsilva.brainout.core.domain.model.User

class FakeActiveUserPort : ActiveUserPort {
    var user: User? = User.create("Teste", "hash").copy(id = "u1")

    override suspend fun currentUser(): User? = user

    override suspend fun runIfCurrent(
        userId: String,
        action: () -> Unit,
    ): Boolean {
        if (user?.id != userId) return false
        action()
        return true
    }
}
