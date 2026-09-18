// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole

/**
 * Linha da tabela `users` no Room.
 *
 * Mantém o mapeamento 1:1 com [User] do domínio. O id é uma `String`
 * (UUID gerado em [User.create]) e usamos como chave primária.
 *
 * O `password_hash` é armazenado como `String` no formato produzido por
 * [pucgo.joaopedrogmsilva.brainout.core.data.security.PasswordHasherImpl]
 * (`alg$iter$salt$hash` Base64). Persistir apenas o hash é o suficiente:
 * sal e iterações são regeneráveis a partir do próprio valor.
 *
 * @property id UUID do usuário (chave primária).
 * @property name Nome completo (1..120 caracteres).
 * @property email Endereço de e-mail (normalizado em [User.create]).
 * @property passwordHash Hash+salt+iter no formato opaco do hasher.
 * @property role Nome do [UserRole] (enum estável — `OWNER` ou `MEMBER`).
 * @property createdAt Instante de criação em epoch millis.
 */
@Entity(
    tableName = "users",
    indices = [
        Index(
            value = ["email"],
            name = "idx_users_email",
            unique = true,
        ),
    ],
)
data class UserEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "email")
    val email: String,
    @ColumnInfo(name = "password_hash")
    val passwordHash: String,
    @ColumnInfo(name = "role")
    val role: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
) {

    /** Converte a linha para o modelo imutável de domínio [User]. */
    fun toDomain(): User = User(
        id = id,
        name = name,
        email = email,
        passwordHash = passwordHash,
        role = UserRole.valueOf(role),
        createdAt = createdAt,
    )

    companion object {

        /** Constrói a entidade a partir de um [User] de domínio. */
        fun fromDomain(user: User): UserEntity = UserEntity(
            id = user.id,
            name = user.name,
            email = user.email,
            passwordHash = user.passwordHash,
            role = user.role.name,
            createdAt = user.createdAt,
        )
    }
}
