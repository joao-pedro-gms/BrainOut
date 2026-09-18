// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import pucgo.joaopedrogmsilva.brainout.core.data.local.converter.InstantConverter
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.UserDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.UserEntity

/**
 * Banco Room do BrainOut.
 *
 * Mantém a infraestrutura de persistência local da camada `:core:data`.
 * Novas entidades devem ser adicionadas à lista [entities] e à lista
 * de [autoMigrations] (preferencialmente) — ou exigir uma [Migration]
 * explícita quando o esquema mudar de forma incompatível.
 *
 * O esquema inicial não usa `fallbackToDestructiveMigration`:
 * migrações precisam ser escritas para preservar dados de usuários
 * já cadastrados em campo.
 *
 * @see UserEntity
 * @see UserDao
 */
@Database(
    entities = [UserEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(InstantConverter::class)
abstract class BrainOutDatabase : RoomDatabase() {

    /** DAO de usuários ([UserEntity]). */
    abstract fun userDao(): UserDao

    companion object {
        const val DATABASE_NAME: String = "brainout.db"
    }
}
