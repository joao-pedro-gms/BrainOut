// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.PasswordHasher

/**
 * Implementação de [PasswordHasher] baseada em PBKDF2-HMAC-SHA256.
 *
 * Formato do hash persistido (`passwordHash`):
 * ```
 * pbkdf2_sha256$<iterations>$<salt_base64>$<hash_base64>
 * ```
 *
 * Decisões de projeto:
 *
 * - **PBKDF2-HMAC-SHA256, 120 000 iterações, 32 bytes de saída.** É
 *   KDF disponível na JVM/Android desde o nível de API 26 sem libs
 *   extras. 120 000 iterações é o mínimo recomendado pelo OWASP
 *   Password Storage Cheat Sheet (2023) para PBKDF2-SHA256.
 * - **Sal aleatório de 16 bytes por senha** via [SecureRandom]. Impede
 *   ataques de rainbow table e força ataques em lote (cada hash deve
 *   ser quebrado individualmente).
 * - **Pepper** fornecido por [PepperProvider]. Em produção, a impl
 *   padrão deriva o pepper da [MasterKey] do androidx.security.crypto
 *   — quando o Keystore oferece hardware-backed key (TEE/StrongBox),
 *   o pepper nunca deixa o hardware; em emulador recai para chave
 *   derivada do AndroidKeyStore. Em testes, injetamos um pepper
 *   determinístico para isolar a lógica do KDF do subsistema de
 *   chaves.
 * - **Comparação em tempo constante** em [verify] via
 *   `MessageDigest.isEqual` (constant-time compare).
 *
 * @property pepperProvider Fonte do pepper aplicado antes do PBKDF2.
 *   Injetada pelo Hilt via [PepperProvider.Factory].
 */
@Singleton
class PasswordHasherImpl @Inject constructor(
    pepper: PepperProvider,
) : PasswordHasher {

    private val pepper: ByteArray = pepper.bytes()

    @Suppress("MagicNumber")
    override fun hash(rawPassword: String): String {
        require(rawPassword.isNotEmpty()) { "rawPassword não pode ser vazia" }

        val salt = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val derived = pbkdf2(rawPassword.toCharArray(), salt)
        return formatHash(
            algorithm = ALGORITHM,
            iterations = PBKDF2_ITERATIONS,
            salt = salt,
            derived = derived,
        )
    }

    override fun verify(rawPassword: String, storedHash: String): Boolean {
        val parsed = parseHashSafely(rawPassword, storedHash) ?: return false
        val candidate = pbkdf2(rawPassword.toCharArray(), parsed.salt)
        return constantTimeEquals(candidate, parsed.derived)
    }

    @Suppress("ReturnCount")
    private fun parseHashSafely(rawPassword: String, storedHash: String): ParsedHash? {
        if (rawPassword.isEmpty() || storedHash.isEmpty()) return null
        val parsed = parseHash(storedHash) ?: return null
        return parsed.takeIf { it.algorithm == ALGORITHM }
    }

    // --- internals ---

    private fun pbkdf2(password: CharArray, salt: ByteArray): ByteArray {
        val combined = password + pepperAsPasswordChars()
        val spec = PBEKeySpec(
            combined,
            salt,
            PBKDF2_ITERATIONS,
            DERIVED_KEY_LENGTH_BITS,
        )
        try {
            val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            return factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun pepperAsPasswordChars(): CharArray =
        String(pepper, Charsets.ISO_8859_1).toCharArray()

    private data class ParsedHash(
        val algorithm: String,
        val iterations: Int,
        val salt: ByteArray,
        val derived: ByteArray,
    )

    private fun formatHash(
        algorithm: String,
        iterations: Int,
        salt: ByteArray,
        derived: ByteArray,
    ): String = buildString {
        append(algorithm)
        append(HASH_FIELD_SEPARATOR)
        append(iterations)
        append(HASH_FIELD_SEPARATOR)
        append(BASE64_URL_ENCODER.encodeToString(salt))
        append(HASH_FIELD_SEPARATOR)
        append(BASE64_URL_ENCODER.encodeToString(derived))
    }

    @Suppress("ReturnCount")
    private fun parseHash(stored: String): ParsedHash? {
        val parts = stored.split(HASH_FIELD_SEPARATOR)
        if (parts.size != EXPECTED_HASH_FIELDS) return null
        val iterations = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
        val salt = parts[2].decodeBase64UrlOrNull() ?: return null
        val derived = parts[3].decodeBase64UrlOrNull() ?: return null
        return ParsedHash(parts[0], iterations, salt, derived)
    }

    private fun String.decodeBase64UrlOrNull(): ByteArray? =
        runCatching { Base64.getUrlDecoder().decode(this) }.getOrNull()

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        return MessageDigest.isEqual(a, b)
    }

    companion object {
        internal const val ALGORITHM: String = "pbkdf2_sha256"
        internal const val PBKDF2_ALGORITHM: String = "PBKDF2WithHmacSHA256"

        // Constantes nomeadas — detekt MagicNumber permite constante.
        private const val PBKDF2_ITERATIONS: Int = 120_000
        private const val SALT_LENGTH_BYTES: Int = 16
        private const val DERIVED_KEY_LENGTH_BITS: Int = 256
        private const val EXPECTED_HASH_FIELDS: Int = 4
        private const val HASH_FIELD_SEPARATOR: Char = '$'

        // Codificador Base64 URL-safe sem padding — `+` e `/` viram
        // `-` e `_`, e o `=` final é removido para caber em qualquer
        // campo textual sem aspas.
        private val BASE64_URL_ENCODER: Base64.Encoder =
            Base64.getUrlEncoder().withoutPadding()

        private val secureRandom: SecureRandom = SecureRandom()
    }
}
