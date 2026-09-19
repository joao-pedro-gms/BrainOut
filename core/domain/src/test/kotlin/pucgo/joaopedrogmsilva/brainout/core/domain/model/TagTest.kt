// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException
import java.time.Instant
import java.util.UUID

class TagTest {

    @Test
    fun `rename retorna nova instancia com nome atualizado`() {
        val original = Tag.create(ownerId = "u", name = "Estudo", color = "#0000FF")
        val renamed = original.rename(" Estudos ")

        assertThat(renamed.name).isEqualTo("Estudos")
        assertThat(renamed.id).isEqualTo(original.id)
        assertThat(renamed.ownerId).isEqualTo(original.ownerId)
        assertThat(renamed.color).isEqualTo(original.color)
        assertThat(renamed.createdAt).isEqualTo(original.createdAt)
        assertThat(renamed).isNotSameInstanceAs(original)
        assertThat(original.name).isEqualTo("Estudo")
        listOf(" ", "a".repeat(31)).forEach { name ->
            assertThrows(InvalidModelException::class.java) { original.rename(name) }
        }
    }

    @Test
    fun `cor fora do formato RRGGBB falha`() {
        listOf("FF0000", "#FF00", "#FF00000", "rgb(0,0,0)").forEach { invalid ->
            assertThrows("cor='$invalid' deveria falhar", InvalidModelException::class.java) {
                Tag.create(ownerId = "user-1", name = "ok", color = invalid)
            }
        }
    }

    @Test
    fun `name maior que 30 caracteres falha`() {
        assertThrows(InvalidModelException::class.java) {
            Tag.create(ownerId = "user-1", name = "a".repeat(31), color = "#000000")
        }
    }

    @Test
    fun `name vazio falha`() {
        listOf("", "   ", "\t\n").forEach { name ->
            assertThrows(InvalidModelException::class.java) {
                Tag.create(ownerId = "user-1", name = name, color = "#000000")
            }
        }
    }

    @Test
    fun `create gera id ownerId name e cor`() {
        val now = Instant.parse("2026-09-18T12:00:00Z")
        val tag = Tag.create(ownerId = "user-1", name = "Urgente", color = "#FF0000", now = now)

        assertThat(UUID.fromString(tag.id).version()).isEqualTo(4)
        assertThat(tag.ownerId).isEqualTo("user-1")
        assertThat(tag.name).isEqualTo("Urgente")
        assertThat(tag.color).isEqualTo("#FF0000")
        assertThat(tag.createdAt).isEqualTo(now)
    }
}
