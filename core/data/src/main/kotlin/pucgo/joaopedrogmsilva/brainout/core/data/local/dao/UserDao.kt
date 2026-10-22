// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.UserEntity

/**
 * DAO da tabela `users`.
 *
 * As pesquisas retornam [UserEntity] (e não o modelo de domínio
 * `User`) por design: o mapeamento Entity ↔ Domain é responsabilidade
 * do repositório, mantendo o DAO agnóstico ao contrato de domínio.
 */
@Dao
interface UserDao {
    // --- Reads ---

    /** Busca pontual por `id`. Retorna `null` se não existir. */
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): UserEntity?

    /**
     * Busca pontual por `email` (case-insensitive).
     *
     * Importante: o domínio normaliza e-mails com `trim` mas preserva
     * o case original. Aqui normalizamos via `LOWER()` para tolerar
     * variações de digitação comuns na UI — `Joao@x.com` e
     * `joao@x.com` são tratados como o mesmo usuário.
     */
    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    // --- Writes ---

    /**
     * Insere um novo usuário. Lança `SQLiteConstraintException` em caso
     * de PK duplicada.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity)
}
