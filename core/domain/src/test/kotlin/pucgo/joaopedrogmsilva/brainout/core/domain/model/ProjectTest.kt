// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException
import java.time.Instant

/**
 * Testes de invariantes e regras de [Project].
 *
 * Cobre:
 * - nome não vazio e dentro do limite de 120 caracteres;
 * - descrição limitada a 500 caracteres;
 * - conclusão/reativação via [markCompleted] e [markIncomplete].
 */
class ProjectTest {

    private val fixedInstant: Instant = Instant.parse("2026-09-18T12:00:00Z")

    @Test
    fun `create builds a project with default not completed state`() {
        val project = Project.create(
            name = "BrainOut",
            ownerId = "user-1",
            description = "Projeto Integrador",
            now = fixedInstant,
        )

        assertThat(project.id).isNotEmpty()
        assertThat(project.name).isEqualTo("BrainOut")
        assertThat(project.description).isEqualTo("Projeto Integrador")
        assertThat(project.ownerId).isEqualTo("user-1")
        assertThat(project.createdAt).isEqualTo(fixedInstant)
        assertThat(project.isCompleted).isFalse()
    }

    @Test
    fun `name with only spaces fails validation`() {
        val ex = kotlin.runCatching {
            Project.requireValidName("   ")
        }.exceptionOrNull()

        assertThat(ex).isInstanceOf(InvalidModelException::class.java)
        assertThat(ex).hasMessageThat().contains("Nome do projeto")
    }

    @Test
    fun `name over max length fails validation`() {
        val longName = "p".repeat(Project.MAX_NAME_LENGTH + 1)
        val ex = kotlin.runCatching { Project.requireValidName(longName) }.exceptionOrNull()

        assertThat(ex).isInstanceOf(InvalidModelException::class.java)
        assertThat(ex).hasMessageThat().contains("no máximo ${Project.MAX_NAME_LENGTH}")
    }

    @Test
    fun `description over max length fails validation`() {
        val tooLong = "d".repeat(Project.MAX_DESCRIPTION_LENGTH + 1)
        val ex = kotlin.runCatching { Project.requireValidDescription(tooLong) }.exceptionOrNull()

        assertThat(ex).isInstanceOf(InvalidModelException::class.java)
        assertThat(ex).hasMessageThat().contains("Descrição deve ter no máximo")
    }

    @Test
    fun `description accepts exactly max length`() {
        val boundary = "d".repeat(Project.MAX_DESCRIPTION_LENGTH)
        // Não lança.
        Project.requireValidDescription(boundary)
    }

    @Test
    fun `constructor rejects empty owner id`() {
        kotlin.runCatching {
            Project(
                id = "p1",
                name = "BrainOut",
                description = null,
                ownerId = "",
                createdAt = fixedInstant,
                isCompleted = false,
            )
        }.also { result ->
            assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun `constructor rejects blank name`() {
        kotlin.runCatching {
            Project(
                id = "p1",
                name = " ",
                description = null,
                ownerId = "user-1",
                createdAt = fixedInstant,
                isCompleted = false,
            )
        }.also { result ->
            assertThat(result.exceptionOrNull()).isInstanceOf(InvalidModelException::class.java)
        }
    }

    @Test
    fun `markCompleted sets flag and is idempotent`() {
        val project = Project.create(
            name = "BrainOut",
            ownerId = "user-1",
            now = fixedInstant,
        )

        val once = project.markCompleted()
        val twice = once.markCompleted()

        assertThat(once.isCompleted).isTrue()
        // Idempotente: o valor final é o mesmo, embora retorne nova
        // instância (modelo imutável).
        assertThat(twice.isCompleted).isTrue()
        assertThat(twice).isEqualTo(once)
    }

    @Test
    fun `markIncomplete unsets flag and is idempotent`() {
        val completed = Project.create(
            name = "BrainOut",
            ownerId = "user-1",
            now = fixedInstant,
        ).markCompleted()

        val reopened = completed.markIncomplete()
        val reopenedAgain = reopened.markIncomplete()

        assertThat(reopened.isCompleted).isFalse()
        assertThat(reopenedAgain.isCompleted).isFalse()
        assertThat(reopenedAgain).isEqualTo(reopened)
    }

    @Test
    fun `description can be null on construction`() {
        val project = Project(
            id = "p1",
            name = "BrainOut",
            description = null,
            ownerId = "user-1",
            createdAt = fixedInstant,
            isCompleted = false,
        )
        assertThat(project.description).isNull()
    }
}
