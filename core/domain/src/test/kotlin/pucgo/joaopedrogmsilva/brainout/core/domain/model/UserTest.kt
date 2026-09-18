// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException
import java.time.Instant

/**
 * Testes de invariantes e fábrica de [User].
 *
 * Cobre o especificado em E1.4: nome não vazio e com até 120
 * caracteres, e-mail via regex, hash de senha obrigatório.
 */
class UserTest {

    private val fixedInstant: Instant = Instant.parse("2026-09-18T12:00:00Z")

    @Test
    fun `create builds a valid user with trimmed email and trimmed name`() {
        val user = User.create(
            name = "  João Pedro  ",
            email = "  joao@example.com  ",
            passwordHash = "hashed",
            role = UserRole.OWNER,
            now = fixedInstant,
        )

        assertThat(user.id).isNotEmpty()
        assertThat(user.name).isEqualTo("  João Pedro  ")
        assertThat(user.email).isEqualTo("joao@example.com")
        assertThat(user.passwordHash).isEqualTo("hashed")
        assertThat(user.role).isEqualTo(UserRole.OWNER)
        assertThat(user.createdAt).isEqualTo(fixedInstant)
    }

    @Test
    fun `name with empty content fails validation`() {
        val ex = assertThrows<InvalidModelException> {
            User.requireValidName("   ")
        }
        assertThat(ex).hasMessageThat().contains("Nome não pode ser vazio")
    }

    @Test
    fun `name longer than max length fails validation`() {
        val longName = "a".repeat(User.MAX_NAME_LENGTH + 1)
        val ex = assertThrows<InvalidModelException> {
            User.requireValidName(longName)
        }
        assertThat(ex).hasMessageThat().contains("no máximo ${User.MAX_NAME_LENGTH}")
    }

    @Test
    fun `name at exactly max length is accepted`() {
        val boundaryName = "a".repeat(User.MAX_NAME_LENGTH)
        // Não lança.
        User.requireValidName(boundaryName)
    }

    @Test
    fun `email without at sign fails validation`() {
        val ex = assertThrows<InvalidModelException> {
            User.requireValidEmail("not-an-email")
        }
        assertThat(ex).hasMessageThat().contains("E-mail inválido")
    }

    @Test
    fun `email without domain dot fails validation`() {
        val ex = assertThrows<InvalidModelException> {
            User.requireValidEmail("user@domain")
        }
        assertThat(ex).hasMessageThat().contains("E-mail inválido")
    }

    @Test
    fun `well formed email passes validation`() {
        // Não lança.
        User.requireValidEmail("joao.pedro@pucgoias.edu.br")
    }

    @Test
    fun `constructor rejects empty passwordHash`() {
        assertThrows<IllegalArgumentException> {
            User(
                id = "user-1",
                name = "João",
                email = "joao@example.com",
                passwordHash = "",
                role = UserRole.MEMBER,
                createdAt = fixedInstant,
            )
        }
    }

    @Test
    fun `constructor rejects invalid email`() {
        assertThrows<InvalidModelException> {
            User(
                id = "user-1",
                name = "João",
                email = "invalid",
                passwordHash = "h",
                role = UserRole.MEMBER,
                createdAt = fixedInstant,
            )
        }
    }

    @Test
    fun `constructor rejects empty name`() {
        assertThrows<InvalidModelException> {
            User(
                id = "user-1",
                name = "   ",
                email = "joao@example.com",
                passwordHash = "h",
                role = UserRole.MEMBER,
                createdAt = fixedInstant,
            )
        }
    }

    @Test
    fun `copy preserves immutability contract`() {
        val original = User.create(
            name = "João",
            email = "joao@example.com",
            passwordHash = "h",
            role = UserRole.MEMBER,
            now = fixedInstant,
        )
        val renamed = original.copy(name = "Pedro")

        assertThat(renamed).isNotSameInstanceAs(original)
        assertThat(original.name).isEqualTo("João")
        assertThat(renamed.name).isEqualTo("Pedro")
        assertThat(renamed.id).isEqualTo(original.id)
    }

    /**
     * Pequeno helper para capturar exceções esperadas em testes JUnit 4
     * sem expor o parâmetro reificado de kotlin.test.
     */
    private inline fun <reified T : Throwable> assertThrows(block: () -> Unit): T {
        try {
            block()
        } catch (t: Throwable) {
            if (t is T) return t
            throw AssertionError("Esperava ${T::class.qualifiedName}, recebeu ${t::class.qualifiedName}", t)
        }
        throw AssertionError("Esperava ${T::class.qualifiedName}, mas nenhuma exceção foi lançada")
    }
}
