// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.security

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

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
 * - `verify` respeita as iterações gravadas no hash (regressão).
 */
class PasswordHasherImplTest {
    private lateinit var hasher: PasswordHasherImpl

    @Before
    fun setUp() {
        hasher = PasswordHasherImpl(pepper = FixedPepperProvider(TEST_PEPPER))
    }

    @Test
    fun `hash produces pbkdf2 formatted string`() {
        val hashed = hasher.hash("correct horse battery staple")

        val parts = hashed.split(FIELD_SEPARATOR)
        assertThat(parts).hasSize(4)
        assertThat(parts[0]).isEqualTo(ALGORITHM_NAME)
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

    @Test
    fun `hash sempre grava o custo default no hash persistido`() {
        val hashed = hasher.hash("senha-default")

        assertThat(custoDe(hashed)).isEqualTo(PBKDF2_DEFAULT_ITERATIONS)
    }

    @Test
    fun `verify aceita hash gravado com custo abaixo do default`() {
        val hashLegado = hashComCusto("senha-regressao", salDe(1), LEGACY_ITERATIONS)

        assertThat(custoDe(hashLegado)).isEqualTo(LEGACY_ITERATIONS)
        assertThat(hasher.verify("senha-regressao", hashLegado)).isTrue()
        assertThat(hasher.verify("senha-errada", hashLegado)).isFalse()
    }

    @Test
    fun `verify aceita hash gravado com custo acima do default`() {
        val hashFuturo = hashComCusto("senha-futura", salDe(2), HIGHER_ITERATIONS)

        assertThat(custoDe(hashFuturo)).isEqualTo(HIGHER_ITERATIONS)
        assertThat(hasher.verify("senha-futura", hashFuturo)).isTrue()
    }

    @Test
    fun `verify aceita custos distintos para a mesma senha e mesmo sal`() {
        val sal = salDe(3)
        val hashBarato = hashComCusto("mesma-senha", sal, LOW_ITERATIONS)
        val hashPadrao = hashComCusto("mesma-senha", sal, PBKDF2_DEFAULT_ITERATIONS)

        assertThat(hashBarato).isNotEqualTo(hashPadrao)
        assertThat(hasher.verify("mesma-senha", hashBarato)).isTrue()
        assertThat(hasher.verify("mesma-senha", hashPadrao)).isTrue()
    }

    @Test
    fun `verify rejeita custo fora da faixa suportada sem lançar`() {
        val salCodificado = encode(salDe(4))
        val derivadoCodificado = encode(ByteArray(DERIVED_KEY_BYTES) { it.toByte() })

        listOf("0", "-5", "999999999999").forEach { custo ->
            val malformado =
                listOf(
                    ALGORITHM_NAME,
                    custo,
                    salCodificado,
                    derivadoCodificado,
                ).joinToString(FIELD_SEPARATOR.toString())

            assertThat(hasher.verify("any", malformado)).isFalse()
        }
    }

    private fun custoDe(hashed: String): Int = hashed.split(FIELD_SEPARATOR)[1].toInt()

    private fun salDe(seed: Int): ByteArray = ByteArray(SALT_BYTES) { (it * seed).toByte() }

    private fun encode(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    /**
     * Monta um hash no formato de disco com custo arbitrário aplicando
     * o mesmo pepper do `setUp`. Assim o teste exercita o caminho
     * público `verify` sem precisar expor `formatHash`/`ParsedHash`,
     * que seguem privados por decisão de encapsulamento.
     */
    private fun hashComCusto(
        password: String,
        salt: ByteArray,
        iterations: Int,
    ): String {
        val pepperComoSenha = String(TEST_PEPPER, Charsets.ISO_8859_1)
        val combinado = (password + pepperComoSenha).toCharArray()
        val spec = PBEKeySpec(combinado, salt, iterations, DERIVED_KEY_BITS)
        val derivado =
            try {
                SecretKeyFactory.getInstance(PBKDF2_JCA_ALGORITHM).generateSecret(spec).encoded
            } finally {
                spec.clearPassword()
            }
        return listOf(
            ALGORITHM_NAME,
            iterations.toString(),
            encode(salt),
            encode(derivado),
        ).joinToString(FIELD_SEPARATOR.toString())
    }

    private companion object {
        val TEST_PEPPER: ByteArray = byteArrayOf(0x42, 0x13, 0x37, 0xAB.toByte())

        const val FIELD_SEPARATOR: Char = '$'
        const val ALGORITHM_NAME: String = "pbkdf2_sha256"
        const val PBKDF2_JCA_ALGORITHM: String = "PBKDF2WithHmacSHA256"
        const val DERIVED_KEY_BITS: Int = 256
        const val DERIVED_KEY_BYTES: Int = 32
        const val SALT_BYTES: Int = 16
        const val LOW_ITERATIONS: Int = 1_000
        const val LEGACY_ITERATIONS: Int = 10_000
        const val PBKDF2_DEFAULT_ITERATIONS: Int = 120_000
        const val HIGHER_ITERATIONS: Int = 250_000
    }
}
