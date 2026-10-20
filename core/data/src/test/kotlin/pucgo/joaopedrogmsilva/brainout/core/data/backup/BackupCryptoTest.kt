// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.backup

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.error.BackupIntegrityException
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Testes da cifragem do backup (issue #214 — LOCAL-03).
 *
 * A parte sensível é a implementação **manual** de PBKDF2 sobre
 * `Mac`/`HmacSHA256` (necessária porque
 * `SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")` só existe a
 * partir do API 26 e o `minSdk` do projeto é 24). Estes testes
 * fixam a equivalência dela com a implementação da JDK, para que a
 * hipótese de um erro sutil no KDF não passe.
 */
class BackupCryptoTest {
    @Test
    fun `pbkdf2 manual reproduz o vetor conhecido de hmac sha256`() {
        // RFC 7914 / vectors widely used: PBKDF2-HMAC-SHA256
        // ("passwd", "salt", 1, dkLen = 32).
        val derived =
            BackupCrypto.pbkdf2(
                password = "passwd".toByteArray(Charsets.UTF_8),
                salt = "salt".toByteArray(Charsets.UTF_8),
                iterations = 1,
                keyLengthBytes = 32,
            )

        val expected =
            "55ac046e56e3089fec1691c22544b605f94185216dde0465e68b9d57c20dacbc"
        assertThat(toHex(derived)).isEqualTo(expected)
    }

    @Test
    fun `pbkdf2 manual equivale a implementacao da jdk`() {
        val password = "senha-de-teste-çá"
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iterations = 1000

        val manual = BackupCrypto.pbkdf2(password.toByteArray(Charsets.UTF_8), salt, iterations, 32)
        val jdk =
            SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(PBEKeySpec(password.toCharArray(), salt, iterations, 256))
                .encoded

        assertThat(manual).isEqualTo(jdk)
    }

    @Test
    fun `cifra e decifra o mesmo conteudo`() {
        val plaintext = "conteúdo do backup".toByteArray(Charsets.UTF_8)

        val encrypted = BackupCrypto.encrypt(plaintext, PASSPHRASE)
        val decrypted = BackupCrypto.decrypt(encrypted, PASSPHRASE)

        assertThat(decrypted).isEqualTo(plaintext)
    }

    @Test
    fun `senha errada falha com erro de integridade`() {
        val encrypted = BackupCrypto.encrypt("conteúdo".toByteArray(Charsets.UTF_8), PASSPHRASE)

        val error =
            runCatching {
                BackupCrypto.decrypt(encrypted, "outra-senha")
            }.exceptionOrNull()

        assertThat(error).isInstanceOf(BackupIntegrityException::class.java)
    }

    @Test
    fun `conteudo adulterado falha com erro de integridade`() {
        val encrypted = BackupCrypto.encrypt("conteúdo".toByteArray(Charsets.UTF_8), PASSPHRASE)
        val tampered = encrypted.copy(ciphertext = encrypted.ciphertext.copyOf().also { it[0] = 0 })

        val error =
            runCatching {
                BackupCrypto.decrypt(tampered, PASSPHRASE)
            }.exceptionOrNull()

        assertThat(error).isInstanceOf(BackupIntegrityException::class.java)
    }

    @Test
    fun `salt e iv mudam a cada cifragem`() {
        val plaintext = "conteúdo".toByteArray(Charsets.UTF_8)

        val first = BackupCrypto.encrypt(plaintext, PASSPHRASE)
        val second = BackupCrypto.encrypt(plaintext, PASSPHRASE)

        assertThat(first.salt).isNotEqualTo(second.salt)
        assertThat(first.iv).isNotEqualTo(second.iv)
        assertThat(first.ciphertext).isNotEqualTo(second.ciphertext)
    }

    @Test
    fun `base64 invalido devolve null em vez de lancar`() {
        assertThat(BackupCrypto.decodeBase64OrNull("@@@nao-e-base64@@@")).isNull()
    }

    private fun toHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    private companion object {
        const val PASSPHRASE: String = "senha-segura"
    }
}
