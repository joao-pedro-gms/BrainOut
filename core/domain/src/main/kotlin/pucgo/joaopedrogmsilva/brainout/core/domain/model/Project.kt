// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.model

import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException
import java.time.Instant
import java.util.UUID

/**
 * Projeto agrupador de [Task]s no BrainOut.
 *
 * Entidade imutável. Toda alteração deve ser feita por [copy] retornando
 * nova instância. As invariantes de [name], [description] e conclusão
 * são aplicadas em [init] e nos métodos de domínio.
 *
 * @property id Identificador único do projeto.
 * @property name Nome (1..120 caracteres, não vazio).
 * @property description Descrição opcional (até 500 caracteres).
 * @property ownerId [User.id] do proprietário (papel [UserRole.OWNER]).
 * @property createdAt Instante de criação em UTC.
 * @property isCompleted Flag de conclusão do projeto.
 */
data class Project(
    val id: String,
    val name: String,
    val description: String?,
    val ownerId: String,
    val createdAt: Instant,
    val isCompleted: Boolean,
) {
    init {
        require(id.isNotBlank()) { "id não pode ser vazio" }
        requireValidName(name)
        description?.let { requireValidDescription(it) }
        require(ownerId.isNotBlank()) { "ownerId não pode ser vazio" }
    }

    companion object {
        const val MAX_NAME_LENGTH: Int = 120
        const val MAX_DESCRIPTION_LENGTH: Int = 500

        fun create(
            name: String,
            ownerId: String,
            description: String? = null,
            now: Instant = Instant.now(),
        ): Project = Project(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            ownerId = ownerId,
            createdAt = now,
            isCompleted = false,
        )

        fun requireValidName(raw: String) {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) {
                throw InvalidModelException("Nome do projeto não pode ser vazio")
            }
            if (trimmed.length > MAX_NAME_LENGTH) {
                throw InvalidModelException(
                    "Nome do projeto deve ter no máximo $MAX_NAME_LENGTH caracteres",
                )
            }
        }

        fun requireValidDescription(raw: String) {
            if (raw.length > MAX_DESCRIPTION_LENGTH) {
                throw InvalidModelException(
                    "Descrição deve ter no máximo $MAX_DESCRIPTION_LENGTH caracteres",
                )
            }
        }
    }

    /**
     * Retorna nova instância marcada como concluída. Idempotente.
     */
    fun markCompleted(): Project = copy(isCompleted = true)

    /**
     * Retorna nova instância marcada como não concluída. Idempotente.
     */
    fun markIncomplete(): Project = copy(isCompleted = false)
}
