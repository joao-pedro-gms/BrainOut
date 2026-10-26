// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.session

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import pucgo.joaopedrogmsilva.brainout.core.data.local.LocalTransaction
import pucgo.joaopedrogmsilva.brainout.core.domain.error.OwnershipException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.UserNotFoundException
import javax.inject.Inject

/** Fronteira única entre sessão e dados pessoais; escritas também bloqueiam a troca de conta. */
class AccountAccess
    @Inject
    constructor(
        private val session: SessionStore,
        private val transaction: LocalTransaction,
    ) {
        suspend fun currentId(): String = session.currentUserId() ?: throw UserNotFoundException()

        suspend fun owns(ownerId: String): Boolean = session.currentUserId() == ownerId

        suspend fun requireOwner(ownerId: String) {
            if (!owns(ownerId)) throw OwnershipException()
        }

        suspend fun <T> write(
            ownerId: String,
            block: suspend () -> T,
        ): T = session.withUser(ownerId) { transaction.run(block) }

        @OptIn(ExperimentalCoroutinesApi::class)
        fun <T> observe(
            empty: T,
            source: suspend (String) -> Flow<T>,
        ): Flow<T> =
            session.observeUserId().flatMapLatest { id ->
                if (id == null) flowOf(empty) else source(id).map { if (owns(id)) it else empty }
            }

        fun <T> observeOwner(
            ownerId: String,
            empty: T,
            source: () -> Flow<T>,
        ): Flow<T> = observe(empty) { id -> if (id == ownerId) source() else flowOf(empty) }
    }
