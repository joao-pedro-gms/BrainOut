// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.entity

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.model.User
import pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole

/**
 * Testes unitários do mapeamento [User] ↔ [UserEntity].
 *
 * Garantem que o round-trip preserva todos os campos e que o nome do
 * `UserRole` é usado como discriminador textual — o suficiente para
 * sobreviver a upgrades do app.
 */
class UserEntityTest {

    @Test
    fun `fromDomain then toDomain is identity for an owner`() {
        val original = User(
            id = "id-1",
            name = "João Pedro",
            email = "joao@example.com",
            passwordHash = "pbkdf2_sha256\$120000\$AAAA\$BBBB",
            role = UserRole.OWNER,
            createdAt = Instant.parse("2026-01-15T12:00:00Z"),
        )

        val entity = UserEntity.fromDomain(original)
        val back = entity.toDomain()

        assertThat(back).isEqualTo(original)
        assertThat(entity.role).isEqualTo("OWNER")
    }

    @Test
    fun `fromDomain then toDomain is identity for a member`() {
        val original = User(
            id = "id-2",
            name = "Maria",
            email = "maria@example.com",
            passwordHash = "x",
            role = UserRole.MEMBER,
            createdAt = Instant.parse("2026-02-01T08:30:00Z"),
        )

        val back = UserEntity.fromDomain(original).toDomain()

        assertThat(back).isEqualTo(original)
        assertThat(back.role).isEqualTo(UserRole.MEMBER)
    }
}
