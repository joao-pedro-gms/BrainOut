// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local

import androidx.room.withTransaction
import javax.inject.Inject

/** Unidade atômica de persistência local, independente de fila ou rede. */
interface LocalTransaction {
    suspend fun <T> run(block: suspend () -> T): T
}

class RoomLocalTransaction
    @Inject
    constructor(
        private val database: BrainOutDatabase,
    ) : LocalTransaction {
        override suspend fun <T> run(block: suspend () -> T): T = database.withTransaction(block)
    }
