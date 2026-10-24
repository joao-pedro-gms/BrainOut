// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.security

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.PasswordHasher
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

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
 *   — quando o Keystore oferece hardware-backed key (TEE/StrongBox), o
 *   pepper nunca deixa o hardware; em emulador recai para chave
 *   derivada do AndroidKeyStore. Em testes, injetamos um pepper
 *   determinístico para isolar a lógica do KDF do subsistema de
 *   chaves.
 * - **Comparação em tempo constante** em [verify] via
 *   `MessageDigest.isEqual` (constant-time compare).
 * - **O KDF roda fora da thread que chamou.** 120 000 iterações de
 *   PBKDF2-HMAC-SHA256 levam centenas de milissegundos; sem o
 *   `withContext`, o login e o cadastro travariam a interface — o
 *   `AuthViewModel` chama o caso de uso de dentro do `viewModelScope`,
 *   que roda no dispatcher principal. (A regra de não embrulhar o DAO
 *   do Room em `withContext(Dispatchers.IO)` não se aplica aqui: aqui
 *   não há I/O de banco, e sim trabalho de CPU.)
 * - **O custo do KDF é lido do hash, não da constante.** [verify]
 *   deriva a chave candidate usando `iterations` gravado no hash
 *   persistido, e não [PBKDF2_ITERATIONS]. Sem isso, subir o custo
 *   (ex.: 120 000 → 600 000 conforme o OWASP) tornaria todo hash
 *   antigo verificável apenas contra o custo novo — quebrando
 *   silenciosamente todas as contas existentes. `hash()` continua
 *   gravando [PBKDF2_ITERATIONS] ao criar hashes novos.
 * - **Faixa de iterações validada na leitura.** `parseHash` rejeita
 *   custos fora de `[1, MAX_PBKDF2_ITERATIONS]`: um `.db` adulterado
 *   ou restaurado não consegue transformar o login em negação de
 *   serviço com `iterations` na casa dos bilhões.
 *
 * @property pepperProvider Fonte do pepper aplicado antes do PBKDF2.
 *   Injetada pelo Hilt via [PepperProvider.Factory].
 */
@Singleton
class PasswordHasherImpl(
    pepper: PepperProvider,
    private val kdfDispatcher: CoroutineDispatcher,
) : PasswordHasher {
    private val pepper: ByteArray = pepper.bytes()

    /**
     * Construtor de produção: o KDF roda em [Dispatchers.IO].
     *
     * O dispatcher é um parâmetro apenas para que o teste possa
     * observar em qual thread o KDF executou — a garantia é "não roda
     * na thread que chamou", e ela precisa de uma forma de ser
     * verificada.
     */
    @Inject
    constructor(pepper: PepperProvider) : this(pepper, Dispatchers.IO)

    @Suppress("MagicNumber") // As constantes do PBKDF2 são o próprio assunto.
    override suspend fun hash(rawPassword: String): String {
        require(rawPassword.isNotEmpty()) { "rawPassword não pode ser vazia" }

        val salt = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val derived = pbkdf2(rawPassword.toCharArray(), salt, PBKDF2_ITERATIONS)
        return formatHash(
            algorithm = ALGORITHM,
            iterations = PBKDF2_ITERATIONS,
            salt = salt,
            derived = derived,
        )
    }

    override suspend fun verify(
        rawPassword: String,
        storedHash: String,
    ): Boolean {
        val parsed = parseHashSafely(rawPassword, storedHash) ?: return false
        val candidate = pbkdf2(rawPassword.toCharArray(), parsed.salt, parsed.iterations)
        return constantTimeEquals(candidate, parsed.derived)
    }

    private fun parseHashSafely(
        rawPassword: String,
        storedHash: String,
    ): ParsedHash? {
        if (rawPassword.isEmpty() || storedHash.isEmpty()) return null
        val parsed = parseHash(storedHash) ?: return null
        return parsed.takeIf { it.algorithm == ALGORITHM }
    }

    // --- internals ---

    private suspend fun pbkdf2(
        password: CharArray,
        salt: ByteArray,
        iterations: Int,
    ): ByteArray {
        require(iterations in MIN_PBKDF2_ITERATIONS..MAX_PBKDF2_ITERATIONS) {
            "iterations fora da faixa suportada: $iterations"
        }
        val combined = password + pepperAsPasswordChars()
        val spec =
            PBEKeySpec(
                combined,
                salt,
                iterations,
                DERIVED_KEY_LENGTH_BITS,
            )
        return withContext(kdfDispatcher) {
            try {
                val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
                factory.generateSecret(spec).encoded
            } finally {
                spec.clearPassword()
            }
        }
    }

    private fun pepperAsPasswordChars(): CharArray = String(pepper, Charsets.ISO_8859_1).toCharArray()

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
    ): String =
        buildString {
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
        val iterations =
            parts[1]
                .toIntOrNull()
                ?.takeIf { it in MIN_PBKDF2_ITERATIONS..MAX_PBKDF2_ITERATIONS }
                ?: return null
        val salt = parts[2].decodeBase64UrlOrNull() ?: return null
        val derived = parts[3].decodeBase64UrlOrNull() ?: return null
        return ParsedHash(parts[0], iterations, salt, derived)
    }

    private fun String.decodeBase64UrlOrNull(): ByteArray? =
        runCatching {
            Base64.getUrlDecoder().decode(this)
        }.getOrNull()

    private fun constantTimeEquals(
        a: ByteArray,
        b: ByteArray,
    ): Boolean {
        if (a.size != b.size) return false
        return MessageDigest.isEqual(a, b)
    }

    companion object {
        internal const val ALGORITHM: String = "pbkdf2_sha256"
        internal const val PBKDF2_ALGORITHM: String = "PBKDF2WithHmacSHA256"

        // Constantes nomeadas — detekt MagicNumber permite constante.
        private const val PBKDF2_ITERATIONS: Int = 120_000
        private const val MIN_PBKDF2_ITERATIONS: Int = 1

        // Teto de custo na leitura: um `.db` adulterado ou restaurado
        // com `iterations` absurdos vira negação de serviço no login
        // (PBKDF2 bilionário por tentativa). 10x o custo atual é folga
        // para qualquer plano futuro de upgrade de custo.
        private const val MAX_PBKDF2_ITERATIONS: Int = 2_000_000

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
