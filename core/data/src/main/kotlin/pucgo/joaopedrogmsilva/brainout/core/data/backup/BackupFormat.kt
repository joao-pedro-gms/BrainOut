// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.backup

import org.json.JSONException
import org.json.JSONObject
import pucgo.joaopedrogmsilva.brainout.core.domain.error.BackupFormatException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.UnsupportedBackupVersionException

/**
 * Envelope do arquivo de backup (issue #214 — LOCAL-03).
 *
 * O arquivo é um JSON com os metadados em **texto claro** e o
 * conteúdo **cifrado** em Base64. A separação é deliberada: a
 * validação de formato e de versão acontece antes de qualquer
 * tentativa de decifrar, o que permite responder com precisão
 * "isto não é um backup do BrainOut" ou "este backup é de uma versão
 * futura" — e não com um genérico "senha incorreta".
 *
 * ```
 * {
 *   "format": "brainout-backup",
 *   "formatVersion": 1,
 *   "createdAtEpochMillis": 1760000000000,
 *   "kdf":    { "algorithm": "pbkdf2_hmac_sha256", "iterations": 120000, "salt": "<base64>" },
 *   "cipher": { "algorithm": "aes_256_gcm", "iv": "<base64>" },
 *   "payload": "<base64 do texto cifrado + tag do GCM>"
 * }
 * ```
 *
 * O envelope é a **fronteira de compatibilidade**: quando o formato
 * evoluir, `formatVersion` sobe e as versões antigas continuam
 * legíveis — o aplicativo recusa o que não sabe ler, em vez de
 * tentar adivinhar.
 */
internal object BackupFormat {
    /** Identificador do formato — precisa bater para o arquivo ser aceito. */
    const val FORMAT_ID: String = "brainout-backup"

    /** Versão do formato que esta implementação sabe ler. */
    const val SUPPORTED_FORMAT_VERSION: Int = 2

    /**
     * Limite de tamanho do arquivo aceito na importação: 20 MiB.
     *
     * O conteúdo de um app pessoal de projetos e tarefas é ordens de
     * grandeza menor que isso; o limite existe para que um arquivo
     * absurdo (ou uma unidade de rede montada como arquivo) não
     * consuma a memória do aparelho.
     */
    const val MAX_BACKUP_BYTES: Int = 20 * 1024 * 1024

    /**
     * Faixa aceitável para o custo do KDF declarado por um arquivo.
     *
     * O piso de 10.000 é deliberado: aceitar custo menor permitiria que um
     * envelope enfraquecesse a derivação da chave do próprio arquivo. O teto
     * existe para que um arquivo absurdo não prenda a interface por minutos
     * de PBKDF2 antes de qualquer validação.
     */
    const val MIN_KDF_ITERATIONS: Int = 10_000
    const val MAX_KDF_ITERATIONS: Int = 2_000_000

    private const val KEY_FORMAT: String = "format"
    private const val KEY_FORMAT_VERSION: String = "formatVersion"
    private const val KEY_CREATED_AT: String = "createdAtEpochMillis"
    private const val KEY_KDF: String = "kdf"
    private const val KEY_CIPHER: String = "cipher"
    private const val KEY_PAYLOAD: String = "payload"
    private const val KEY_ALGORITHM: String = "algorithm"
    private const val KEY_ITERATIONS: String = "iterations"
    private const val KEY_SALT: String = "salt"
    private const val KEY_IV: String = "iv"
    private const val SALT_LENGTH_BYTES: Int = 16
    private const val IV_LENGTH_BYTES: Int = 12
    private const val GCM_TAG_LENGTH_BYTES: Int = 16

    /** Monta o envelope completo do arquivo. */
    fun encodeEnvelope(
        createdAtEpochMillis: Long,
        payload: EncryptedPayload,
    ): ByteArray {
        val json =
            JSONObject()
                .put(KEY_FORMAT, FORMAT_ID)
                .put(KEY_FORMAT_VERSION, SUPPORTED_FORMAT_VERSION)
                .put(KEY_CREATED_AT, createdAtEpochMillis)
                .put(
                    KEY_KDF,
                    JSONObject()
                        .put(KEY_ALGORITHM, BackupCrypto.KDF_ALGORITHM)
                        .put(KEY_ITERATIONS, BackupCrypto.KDF_ITERATIONS)
                        .put(KEY_SALT, BackupCrypto.encodeBase64(payload.salt)),
                ).put(
                    KEY_CIPHER,
                    JSONObject()
                        .put(KEY_ALGORITHM, BackupCrypto.CIPHER_ALGORITHM)
                        .put(KEY_IV, BackupCrypto.encodeBase64(payload.iv)),
                ).put(KEY_PAYLOAD, BackupCrypto.encodeBase64(payload.ciphertext))
        return json.toString().toByteArray(Charsets.UTF_8)
    }

    /**
     * Lê o envelope do arquivo.
     *
     * @throws BackupFormatException se o arquivo não for um backup do
     *  BrainOut ou estiver malformado.
     * @throws UnsupportedBackupVersionException se a versão do formato
     *  não for a suportada.
     */
    fun decodeEnvelope(bytes: ByteArray): BackupEnvelope {
        val root = parseRoot(bytes)
        val version = validateFormat(root)
        return readEnvelope(root, version)
    }

    /** Lê o JSON raiz do arquivo. */
    private fun parseRoot(bytes: ByteArray): JSONObject =
        try {
            JSONObject(String(bytes, Charsets.UTF_8))
        } catch (error: JSONException) {
            throw BackupFormatException("Arquivo de backup não contém JSON válido.")
        }

    /**
     * Confere o identificador e a versão do formato.
     *
     * A versão é validada **antes** de qualquer tentativa de decifrar:
     * assim o app consegue distinguir "isto não é um backup do
     * BrainOut" de "este backup é de uma versão futura", em vez de
     * reportar um genérico "senha incorreta".
     */
    private fun validateFormat(root: JSONObject): Int {
        val format = root.optString(KEY_FORMAT)
        if (format != FORMAT_ID) {
            throw BackupFormatException("O arquivo não é um backup do BrainOut.")
        }
        val version = root.optInt(KEY_FORMAT_VERSION, -1)
        if (version !in 1..SUPPORTED_FORMAT_VERSION) {
            throw UnsupportedBackupVersionException(version)
        }
        return version
    }

    /** Extrai sal, IV e texto cifrado do envelope validado. */
    private fun readEnvelope(
        root: JSONObject,
        version: Int,
    ): BackupEnvelope {
        val kdf = root.optJSONObject(KEY_KDF)
        val cipher = root.optJSONObject(KEY_CIPHER)
        if (kdf == null || cipher == null) {
            throw BackupFormatException("Envelope de backup incompleto.")
        }
        validateAlgorithms(kdf, cipher)
        val kdfIterations = kdf.optInt(KEY_ITERATIONS, BackupCrypto.KDF_ITERATIONS)
        validateKdfIterations(kdfIterations)
        val salt = decodeFixedLengthBase64OrFail(kdf.optString(KEY_SALT), "salt", SALT_LENGTH_BYTES)
        val iv = decodeFixedLengthBase64OrFail(cipher.optString(KEY_IV), "iv", IV_LENGTH_BYTES)
        val ciphertext = decodeBase64OrFail(root.optString(KEY_PAYLOAD), "payload")
        requireMinimumLength(ciphertext, GCM_TAG_LENGTH_BYTES, "texto cifrado")
        return BackupEnvelope(
            formatVersion = version,
            createdAtEpochMillis = root.optLong(KEY_CREATED_AT),
            kdfIterations = kdfIterations,
            salt = salt,
            iv = iv,
            ciphertext = ciphertext,
        )
    }

    private fun validateAlgorithms(
        kdf: JSONObject,
        cipher: JSONObject,
    ) {
        if (kdf.optString(KEY_ALGORITHM) != BackupCrypto.KDF_ALGORITHM) {
            throw BackupFormatException("Algoritmo KDF do backup não é suportado.")
        }
        if (cipher.optString(KEY_ALGORITHM) != BackupCrypto.CIPHER_ALGORITHM) {
            throw BackupFormatException("Algoritmo de cifra do backup não é suportado.")
        }
    }

    private fun validateKdfIterations(iterations: Int) {
        if (iterations !in MIN_KDF_ITERATIONS..MAX_KDF_ITERATIONS) {
            throw BackupFormatException("Custo do KDF do backup está fora da faixa aceita.")
        }
    }

    private fun decodeFixedLengthBase64OrFail(
        value: String,
        field: String,
        expectedLength: Int,
    ): ByteArray =
        decodeBase64OrFail(value, field).also { bytes ->
            if (bytes.size != expectedLength) {
                throw BackupFormatException("Tamanho do campo '$field' do backup inválido.")
            }
        }

    private fun requireMinimumLength(
        bytes: ByteArray,
        minimumLength: Int,
        field: String,
    ) {
        if (bytes.size < minimumLength) {
            throw BackupFormatException("Campo '$field' do backup está incompleto.")
        }
    }

    private fun decodeBase64OrFail(
        value: String,
        field: String,
    ): ByteArray =
        BackupCrypto.decodeBase64OrNull(value)
            ?: throw BackupFormatException("Campo '$field' do backup não é Base64 válido.")

    /**
     * Envelope lido do disco.
     *
     * @property formatVersion versão declarada no arquivo.
     * @property createdAtEpochMillis instante de geração do backup.
     * @property kdfIterations custo do KDF usado na cifragem.
     * @property salt sal do KDF.
     * @property iv vetor de inicialização do GCM.
     * @property ciphertext texto cifrado com a tag de autenticação.
     */
    internal data class BackupEnvelope(
        val formatVersion: Int,
        val createdAtEpochMillis: Long,
        val kdfIterations: Int,
        val salt: ByteArray,
        val iv: ByteArray,
        val ciphertext: ByteArray,
    )
}
