// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException

class TagInvariantTest {

    @Test
    fun `aceita limites do nome e cores hexadecimais`() {
        listOf("a", "a".repeat(Tag.MAX_NAME_LENGTH)).forEach { name ->
            listOf("#000000", "#FFFFFF", "#aAbBcC").forEach { color ->
                val tag = Tag.create(ownerId = "u", name = " $name ", color = color)
                Tag.requireValidName(tag.name)
                Tag.requireValidColor(tag.color)
                assertThat(tag.name).isEqualTo(name)
                assertThat(tag.color).isEqualTo(color)
            }
        }
    }

    @Test
    fun `copy e validadores rejeitam nome e cor invalidos`() {
        val tag = Tag.create(ownerId = "u", name = "Estudo", color = "#000000")
        listOf("", " ", "a".repeat(31)).forEach { name ->
            assertThrows(InvalidModelException::class.java) { tag.copy(name = name) }
            assertThrows(InvalidModelException::class.java) { Tag.requireValidName(name) }
        }
        listOf("#GG0000", "#000000\n", " #000000", "#000000 ").forEach { color ->
            assertThrows(InvalidModelException::class.java) { tag.copy(color = color) }
            assertThrows(InvalidModelException::class.java) { Tag.requireValidColor(color) }
        }
    }

    @Test
    fun `identificadores vazios nao podem ser construidos nem copiados`() {
        val tag = Tag.create(ownerId = "u", name = "Estudo", color = "#000000")
        listOf("", " \t").forEach { blank ->
            assertThrows(IllegalArgumentException::class.java) { tag.copy(id = blank) }
            assertThrows(IllegalArgumentException::class.java) { tag.copy(ownerId = blank) }
            assertThrows(IllegalArgumentException::class.java) {
                Tag.create(ownerId = blank, name = "Estudo", color = "#000000")
            }
        }
    }

    @Test
    fun `nome armazenado nunca tem espacos nas pontas`() {
        val tag = Tag.create(ownerId = "u", name = "  Estudo\t", color = "#aAbBcC")
        assertThat(tag.name).isEqualTo("Estudo")
        listOf(" Estudo", "Estudo ").forEach { name ->
            assertThrows(InvalidModelException::class.java) { tag.copy(name = name) }
            assertThrows(InvalidModelException::class.java) { Tag.requireValidName(name) }
        }
    }
}
