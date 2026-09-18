// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.security

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

/**
 * Testes do [PasswordHasherImpl] usando um [PepperProvider]
 * determinístico em vez de [MasterKey] — Robolectric em modo unit
 * não inicializa o AndroidKeyStore, então dependemos de uma fonte
 * fixa de pepper para isolar a lógica do KDF.
 *
 * Cobre:
 * - `hash` produz saída determinística com os campos esperados.
 * - Cada chamada gera sal novo (não-determinístico).
 * - `verify` aceita senha correta, rejeita senha errada.
 * - `verify` rejeita hashes malformados sem lançar.
 * - `hash` rejeita senha vazia via `IllegalArgumentException`.
 */
class PasswordHasherImplTest {

    private lateinit var hasher: PasswordHasherImpl

    @Before
    fun setUp() {
        hasher = PasswordHasherImpl(
            pepper = FixedPepperProvider(byteArrayOf(0x42, 0x13, 0x37, 0xAB.toByte())),
        )
    }

    @Test
    fun `hash produces pbkdf2 formatted string`() {
        val hashed = hasher.hash("correct horse battery staple")

        val parts = hashed.split("$")
        assertThat(parts).hasSize(4)
        assertThat(parts[0]).isEqualTo("pbkdf2_sha256")
        assertThat(parts[1].toInt()).isGreaterThan(0)
        assertThat(parts[2]).isNotEmpty() // salt base64
        assertThat(parts[3]).isNotEmpty() // hash base64
    }

    @Test
    fun `two hashes of the same password differ because of random salt`() {
        val first = hasher.hash("same-password")
        val second = hasher.hash("same-password")

        assertThat(first).isNotEqualTo(second)
    }

    @Test
    fun `verify accepts the original password`() {
        val hashed = hasher.hash("super-secret-123")

        assertThat(hasher.verify("super-secret-123", hashed)).isTrue()
    }

    @Test
    fun `verify rejects an incorrect password`() {
        val hashed = hasher.hash("super-secret-123")

        assertThat(hasher.verify("wrong", hashed)).isFalse()
        assertThat(hasher.verify("", hashed)).isFalse()
        assertThat(hasher.verify("super-secret-124", hashed)).isFalse()
    }

    @Test
    fun `verify rejects malformed stored hash without throwing`() {
        assertThat(hasher.verify("any", "")).isFalse()
        assertThat(hasher.verify("any", "single-segment")).isFalse()
        assertThat(hasher.verify("any", "pbkdf2_sha256\$notanumber\$AAAA\$BBBB")).isFalse()
        assertThat(hasher.verify("any", "wrong_alg\$100\$AAAA\$BBBB")).isFalse()
        assertThat(hasher.verify("any", "pbkdf2_sha256\$100\$!!notbase64\$\$\$")).isFalse()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `hash rejects empty password`() {
        hasher.hash("")
    }

    @Test
    fun `hash produces base64-url safe output`() {
        val hashed = hasher.hash("a-test-password")
        val saltAndHash = hashed.substringAfter("$").substringAfter("$")
        val hashField = hashed.substringAfterLast("$")
        // Sem padding e alfabeto URL-safe — sem `+` nem `/` nem `=`.
        assertThat(saltAndHash).doesNotContain("=")
        assertThat(saltAndHash).doesNotContain("+")
        assertThat(saltAndHash).doesNotContain("/")
        assertThat(hashField).doesNotContain("=")
        assertThat(hashField).doesNotContain("+")
        assertThat(hashField).doesNotContain("/")
    }
}
