// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.converter

import androidx.room.TypeConverter
import java.time.Instant

/**
 * Conversor Room ↔ [Instant].
 *
 * `Instant` não tem representação primitiva em SQLite. Persistimos como
 * epoch millis (`Long`) — granularidade suficiente para `createdAt`,
 * timestamps de auditoria e ordenação via índice. O round-trip é
 * livre de perda porque o [Instant] original só carrega precisão de
 * nanossegundos em memória.
 */
class InstantConverter {

    @TypeConverter
    fun fromEpochMillis(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun toEpochMillis(instant: Instant?): Long? = instant?.toEpochMilli()
}
