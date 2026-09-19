// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.model

import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException
import java.time.Instant
import java.util.UUID

/**
 * Etiqueta pessoal, associada a projetos, com identificador não vazio nem
 * composto apenas por espaços em branco. [create] gera esse identificador
 * com [UUID.randomUUID]; construção direta e [copy] não exigem formato UUID.
 *
 * O nome armazenado tem 1..30 caracteres, sem espaços nas pontas, e a cor
 * segue o formato #RRGGBB. [create] e [rename] normalizam o nome; construção
 * direta e [copy] rejeitam valores que violem essas invariantes.
 */
data class Tag(
    val id: String,
    val ownerId: String,
    val name: String,
    val color: String,
    val createdAt: Instant,
) {
    init {
        require(id.isNotBlank()) { "id não pode ser vazio" }
        require(ownerId.isNotBlank()) { "ownerId não pode ser vazio" }
        requireValidName(name)
        requireValidColor(color)
    }

    fun rename(newName: String): Tag = copy(name = newName.trim())

    companion object {
        const val MAX_NAME_LENGTH: Int = 30
        private val HEX_COLOR_REGEX = Regex("^#[0-9A-Fa-f]{6}$")

        fun requireValidColor(raw: String) {
            if (!HEX_COLOR_REGEX.matches(raw)) {
                throw InvalidModelException("Cor da tag deve estar no formato #RRGGBB (ex.: #FF0000)")
            }
        }

        fun requireValidName(raw: String) {
            if (raw.isBlank() || raw != raw.trim()) {
                throw InvalidModelException("Nome da tag não pode ser vazio nem ter espaços nas pontas")
            }
            if (raw.length > MAX_NAME_LENGTH) {
                throw InvalidModelException("Nome da tag deve ter no máximo $MAX_NAME_LENGTH caracteres")
            }
        }

        fun create(
            ownerId: String,
            name: String,
            color: String,
            now: Instant = Instant.now(),
        ): Tag = Tag(
            id = UUID.randomUUID().toString(),
            ownerId = ownerId,
            name = name.trim(),
            color = color,
            createdAt = now,
        )
    }
}
