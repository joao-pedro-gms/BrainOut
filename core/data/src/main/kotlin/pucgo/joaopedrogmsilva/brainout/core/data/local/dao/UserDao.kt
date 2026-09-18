// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.UserEntity

/**
 * DAO da tabela `users`.
 *
 * Oferece tanto operações `suspend` para uso em repositórios quanto
 * [Flow]s para observação reativa na camada de apresentação.
 *
 * As pesquisas retornam [UserEntity] (e não o modelo de domínio
 * `User`) por design: o mapeamento Entity ↔ Domain é responsabilidade
 * do repositório, mantendo o DAO agnóstico ao contrato de domínio.
 */
@Dao
interface UserDao {

    // --- Reads ---

    /** Observa um usuário pelo `id`. Emite `null` se não existir. */
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<UserEntity?>

    /** Observa um usuário pelo `email` (case-insensitive). */
    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    fun observeByEmail(email: String): Flow<UserEntity?>

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

    /** Conta quantos usuários compartilham o mesmo `email`. */
    @Query("SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(:email)")
    suspend fun countByEmail(email: String): Int

    // --- Writes ---

    /**
     * Insere um novo usuário. Lança `SQLiteConstraintException` em caso
     * de PK duplicada.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity)

    /**
     * Faz upsert (insert-or-update). Útil em migrações futuras onde um
     * registro possa já existir.
     */
    @Upsert
    suspend fun upsert(user: UserEntity)

    @Update
    suspend fun update(user: UserEntity)

    @Delete
    suspend fun delete(user: UserEntity)

    /** Remove o usuário com o `id` informado. Retorna linhas afetadas. */
    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: String): Int

    // --- Bulk ---

    /** Conta total de usuários cadastrados. */
    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    /** Remove todos os usuários. Usado em testes e migrações destrutivas. */
    @Query("DELETE FROM users")
    suspend fun deleteAll()
}
