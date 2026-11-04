// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException

class UserTest {
    @Test
    fun `nomes validos preservam maiusculas e removem espacos das pontas`() {
        val user = User.create("  Joao_123.test  ", "hash")
        assertThat(user.username).isEqualTo("Joao_123.test")
        assertThat(user.id).isNotEmpty()
        assertThat(User.create("joao_123.test", "hash").username).isNotEqualTo(user.username)
    }

    @Test
    fun `nomes aceitam exatamente os limites e caracteres aprovados`() {
        listOf("abc", "A0_", "...", "a".repeat(30)).forEach { User.requireValidUsername(it) }
        listOf("", "ab", "a".repeat(31), "João", "Joao Pedro", "a-b", "a@b").forEach {
            assertThat(
                runCatching { User.create(it, "hash") }.exceptionOrNull(),
            ).isInstanceOf(InvalidModelException::class.java)
        }
    }

    @Test
    fun `id hash e nome persistido precisam respeitar invariantes`() {
        val user = User.create("Joao", "hash")
        assertThat(runCatching { user.copy(id = "") }.isFailure).isTrue()
        assertThat(runCatching { user.copy(passwordHash = "") }.isFailure).isTrue()
        assertThat(runCatching { user.copy(username = " Joao ") }.isFailure).isTrue()
    }
}
