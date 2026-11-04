// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import java.time.Instant

/** Identidade persistente; índice BINARY preserva a distinção entre Joao e joao. */
@Entity(tableName = "users", indices = [Index(value = ["username"], name = "idx_users_username", unique = true)])
data class UserEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "username", collate = ColumnInfo.BINARY) val username: String,
    @ColumnInfo(name = "password_hash") val passwordHash: String,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "legacy_login", defaultValue = "NULL") val legacyLogin: String? = null,
) {
    fun toDomain(): User = User(id, username, passwordHash, createdAt)

    companion object {
        fun fromDomain(user: User): UserEntity = UserEntity(user.id, user.username, user.passwordHash, user.createdAt)
    }
}
