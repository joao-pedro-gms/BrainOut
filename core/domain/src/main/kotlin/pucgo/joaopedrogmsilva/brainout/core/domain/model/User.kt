// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.model

import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException
import java.time.Instant
import java.util.UUID

/** Conta pessoal local. O nome de usuário distingue maiúsculas e minúsculas. */
data class User(
    val id: String,
    val username: String,
    val passwordHash: String,
    val createdAt: Instant,
) {
    init {
        require(id.isNotBlank()) { "id não pode ser vazio" }
        requireValidUsername(username)
        require(username == username.trim()) { "Nome de usuário deve estar aparado" }
        require(passwordHash.isNotBlank()) { "passwordHash não pode ser vazio" }
    }

    companion object {
        const val MIN_USERNAME_LENGTH: Int = 3
        const val MAX_USERNAME_LENGTH: Int = 30
        val USERNAME_REGEX: Regex = Regex("[A-Za-z0-9._]{3,30}")

        fun create(
            username: String,
            passwordHash: String,
            now: Instant = Instant.now(),
        ): User = User(UUID.randomUUID().toString(), username.trim(), passwordHash, now)

        fun requireValidUsername(raw: String) {
            if (!USERNAME_REGEX.matches(raw.trim())) {
                throw InvalidModelException(
                    "Nome de usuário deve ter 3–30 letras ASCII, números, pontos ou sublinhados",
                )
            }
        }
    }
}
