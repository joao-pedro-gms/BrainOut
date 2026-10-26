// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.UserEntity

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username COLLATE BINARY LIMIT 1")
    suspend fun findByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity)

    @Query("SELECT * FROM users WHERE legacy_login IS NOT NULL ORDER BY created_at, id")
    suspend fun legacyLogins(): List<UserEntity>

    @Query("UPDATE users SET legacy_login = NULL")
    suspend fun dismissLegacyLogins()
}
