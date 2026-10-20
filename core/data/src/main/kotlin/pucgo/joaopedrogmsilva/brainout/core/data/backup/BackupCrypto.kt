// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.backup

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cifragem do arquivo de backup (issue #214 — LOCAL-03).
 *
 * ## Por que o conteúdo é cifrado
 *
 * O backup local é um arquivo que o usuário escolhe onde gravar
 * (Storage Access Framework) — normalmente na pasta "Downloads" ou
 * num provedor de nuvem. Sem cifra, nomes de projetos, títulos de
 * tarefas e e-mails ficariam legíveis por qualquer app com acesso ao
 * arquivo. O contrato do formato (em `docs/BACKUP.md`) define, então,
 * que **o conteúdo é cifrado** e que **nenhum material do Android
 * Keystore** (o pepper do [pucgo.joaopedrogmsilva.brainout.core.data.security.PepperProvider])
 * ou hash de senha viaja no arquivo.
 *
 * ## Algoritmos
 *
 * - **Derivação de chave:** PBKDF2-HMAC-SHA256 com 120 000 iterações
 *   e sal aleatório de 16 bytes — o mesmo custo de KDF já usado pelo
 *   [pucgo.joaopedrogmsilva.brainout.core.data.security.PasswordHasherImpl].
 * - **Cifra:** AES-256-GCM com IV aleatório de 12 bytes. O GCM traz
 *   **autenticação embutida**: a tag de verificação detecta qualquer
 *   adulteração do conteúdo, o que dispensa um checksum separado.
 *
 * ## Por que o PBKDF2 é implementado à mão
 *
 * `SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")` só existe a
 * partir do **API 26**; o `minSdk` do projeto é 24, então chamá-lo
 * diretamente quebraria o backup em API 24/25. A implementação abaixo
 * usa apenas `javax.crypto.Mac` ("HmacSHA256", disponível desde o
 * API 1) e segue a especificação RFC 8018 (PKCS #5 v2.1). A
 * equivalência com a implementação da JDK é assegurada por teste de
 * vetor conhecido (`BackupCryptoTest`).
 *
 * O material secreto é manipulado como `ByteArray`/`CharArray` e
 * zerado depois do uso — nunca fica em `String` além do argumento
 * recebido da camada de apresentação.
 */
internal object BackupCrypto {
    /** Identificador do KDF gravado no envelope do arquivo. */
    const val KDF_ALGORITHM: String = "pbkdf2_hmac_sha256"

    /** Custo do KDF, em iterações. Igual ao do hasher de senhas do app. */
    const val KDF_ITERATIONS: Int = 120_000

    /** Identificador da cifra gravado no envelope do arquivo. */
    const val CIPHER_ALGORITHM: String = "aes_256_gcm"

    private const val HMAC_ALGORITHM: String = "HmacSHA256"
    private const val CIPHER_TRANSFORMATION: String = "AES/GCM/NoPadding"
    private const val SALT_LENGTH_BYTES: Int = 16
    private const val IV_LENGTH_BYTES: Int = 12
    private const val KEY_LENGTH_BITS: Int = 256
    private const val GCM_TAG_LENGTH_BITS: Int = 128
    private const val BLOCK_INDEX_BYTES: Int = 4
    private const val BITS_PER_BYTE: Int = 8

    private val secureRandom: SecureRandom = SecureRandom()

    /**
     * Cifra [plaintext] com uma chave derivada de [passphrase].
     *
     * @return o sal, o IV e o texto cifrado (com a tag do GCM).
     */
    fun encrypt(
        plaintext: ByteArray,
        passphrase: String,
    ): EncryptedPayload {
        val salt = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val secret = passphrase.toCharArray()
        val key = deriveKey(secret, salt, KDF_ITERATIONS)
        try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            val ciphertext = cipher.doFinal(plaintext)
            return EncryptedPayload(salt = salt, iv = iv, ciphertext = ciphertext)
        } finally {
            secret.fill('\u0000')
        }
    }

    /**
     * Decifra [payload] com uma chave derivada de [passphrase].
     *
     * @throws pucgo.joaopedrogmsilva.brainout.core.domain.error.BackupIntegrityException
     *  quando a senha está errada ou o conteúdo foi adulterado — o
     *  AES-GCM não distingue os dois casos, e a mensagem é a mesma
     *  de propósito.
     */
    fun decrypt(
        payload: EncryptedPayload,
        passphrase: String,
    ): ByteArray {
        val secret = passphrase.toCharArray()
        val key = deriveKey(secret, payload.salt, KDF_ITERATIONS)
        try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, payload.iv))
            return cipher.doFinal(payload.ciphertext)
        } catch (error: AEADBadTagException) {
            throw pucgo.joaopedrogmsilva.brainout.core.domain.error
                .BackupIntegrityException()
        } catch (error: javax.crypto.BadPaddingException) {
            throw pucgo.joaopedrogmsilva.brainout.core.domain.error
                .BackupIntegrityException()
        } finally {
            secret.fill('\u0000')
        }
    }

    /**
     * Deriva a chave de [KEY_LENGTH_BITS] bits a partir da senha.
     *
     * Exposta em `internal` para que o teste de vetor conhecido possa
     * conferir a implementação de PBKDF2 contra a da JDK.
     */
    internal fun deriveKey(
        passphrase: CharArray,
        salt: ByteArray,
        iterations: Int,
    ): SecretKeySpec {
        val passwordBytes = String(passphrase).toByteArray(Charsets.UTF_8)
        try {
            val derived = pbkdf2(passwordBytes, salt, iterations, KEY_LENGTH_BITS / Byte.SIZE_BITS)
            // `SecretKeySpec` copia o array recebido — zera o buffer de
            // origem para não deixar a chave em memória depois do uso.
            return SecretKeySpec(derived, "AES").also { derived.fill(0) }
        } finally {
            passwordBytes.fill(0)
        }
    }

    /**
     * PBKDF2 (RFC 8018) sobre HMAC-SHA256.
     *
     * @param password senha em bytes UTF-8.
     * @param salt sal.
     * @param iterations iterações do KDF.
     * @param keyLengthBytes tamanho da chave derivada, em bytes.
     */
    internal fun pbkdf2(
        password: ByteArray,
        salt: ByteArray,
        iterations: Int,
        keyLengthBytes: Int,
    ): ByteArray {
        require(iterations > 0) { "iterations deve ser positivo" }
        require(keyLengthBytes > 0) { "keyLengthBytes deve ser positivo" }
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        val key = SecretKeySpec(password, HMAC_ALGORITHM)
        mac.init(key)
        val hashLength = mac.macLength
        val blocks = (keyLengthBytes + hashLength - 1) / hashLength
        val output = ByteArray(blocks * hashLength)
        for (block in 1..blocks) {
            mac.init(key)
            mac.update(salt)
            mac.update(blockIndexBytes(block))
            var u = mac.doFinal()
            val accumulator = u.copyOf()
            repeat(iterations - 1) {
                mac.init(key)
                u = mac.doFinal(u)
                for (index in accumulator.indices) {
                    accumulator[index] = (accumulator[index].toInt() xor u[index].toInt()).toByte()
                }
            }
            val offset = (block - 1) * hashLength
            System.arraycopy(accumulator, 0, output, offset, hashLength)
        }
        return output.copyOf(keyLengthBytes)
    }

    /** Bloco em big-endian de 4 bytes, como exige o RFC 8018. */
    private fun blockIndexBytes(block: Int): ByteArray =
        ByteArray(BLOCK_INDEX_BYTES).also { bytes ->
            bytes.indices.forEach { index ->
                bytes[bytes.lastIndex - index] = (block shr (index * BITS_PER_BYTE)).toByte()
            }
        }

    /** Codifica bytes em Base64 sem quebras de linha (para o envelope). */
    fun encodeBase64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    /** Decodifica Base64; devolve `null` quando o conteúdo não é Base64 válido. */
    fun decodeBase64OrNull(value: String): ByteArray? = runCatching { Base64.decode(value, Base64.NO_WRAP) }.getOrNull()
}

/**
 * Bloco cifrado de um backup.
 *
 * @property salt sal do KDF.
 * @property iv vetor de inicialização do GCM.
 * @property ciphertext texto cifrado **com** a tag de autenticação do GCM.
 */
internal data class EncryptedPayload(
    val salt: ByteArray,
    val iv: ByteArray,
    val ciphertext: ByteArray,
)
