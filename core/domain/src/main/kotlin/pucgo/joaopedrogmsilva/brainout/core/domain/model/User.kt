// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.model

import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException
import java.time.Instant
import java.util.UUID

/**
 * Representa um usuário cadastrado no BrainOut.
 *
 * Entidade imutável: mutações retornam novas instâncias via [copy]. As
 * invariantes de [name] e [email] são aplicadas em [requireValidName],
 * [requireValidEmail] e [init].
 *
 * @property id Identificador único (UUID em formato String).
 * @property name Nome completo (1..120 caracteres após trim).
 * @property email Endereço de e-mail (regex simples, formato RFC suficiente para UI).
 * @property passwordHash Hash da senha (BCrypt/Argon2 — nunca em texto puro).
 * @property role Papel do usuário ([UserRole]).
 * @property createdAt Instante de criação em UTC.
 */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val passwordHash: String,
    val role: UserRole,
    val createdAt: Instant,
) {
    init {
        require(id.isNotBlank()) { "id não pode ser vazio" }
        requireValidName(name)
        requireValidEmail(email)
        require(passwordHash.isNotBlank()) { "passwordHash não pode ser vazio" }
    }

    companion object {
        const val MAX_NAME_LENGTH: Int = 120

        /** Regex simples para validação de e-mail no domínio. */
        val EMAIL_REGEX: Regex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

        /**
         * Factory que gera um novo [User] com [UUID] aleatório e
         * [createdAt] igual ao instante atual.
         *
         * [email] é normalizado com `trim` antes de ser persistido;
         * [name] é preservado como informado para não quebrar nomes com
         * espaços intencionais, mas a validação é feita sobre o valor
         * aparado.
         */
        fun create(
            name: String,
            email: String,
            passwordHash: String,
            role: UserRole,
            now: Instant = Instant.now(),
        ): User = User(
            id = UUID.randomUUID().toString(),
            name = name,
            email = email.trim(),
            passwordHash = passwordHash,
            role = role,
            createdAt = now,
        )

        /** Valida nome retornando [Unit] ou lançando [InvalidModelException]. */
        fun requireValidName(raw: String) {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) {
                throw InvalidModelException("Nome não pode ser vazio")
            }
            if (trimmed.length > MAX_NAME_LENGTH) {
                throw InvalidModelException(
                    "Nome deve ter no máximo $MAX_NAME_LENGTH caracteres",
                )
            }
        }

        /** Valida e-mail retornando [Unit] ou lançando [InvalidModelException]. */
        fun requireValidEmail(raw: String) {
            val trimmed = raw.trim()
            if (!trimmed.matches(EMAIL_REGEX)) {
                throw InvalidModelException("E-mail inválido: $raw")
            }
        }
    }
}
