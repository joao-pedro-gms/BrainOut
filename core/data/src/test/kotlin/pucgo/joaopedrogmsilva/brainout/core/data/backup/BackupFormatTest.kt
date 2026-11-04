// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.backup

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes do **formato** do envelope do arquivo de backup (issue #214 —
 * LOCAL-03).
 *
 * O envelope é o contrato de fora: é ele que diz ao app que o arquivo
 * é um backup, e em que versão do formato ele foi escrito. Um campo
 * renomeado aqui faz o app **recusar** o arquivo — o backup do
 * usuário deixa de ser restaurável, com a mensagem de "não é um backup
 * do app" ou "versão desconhecida".
 *
 * O contrato está descrito em `docs/BACKUP.md` §2.1; estes testes o
 * fixam, para que mudá-lo seja uma decisão consciente (subir o
 * `formatVersion`) e não um efeito colateral.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class BackupFormatTest {
    @Test
    fun `o envelope tem exatamente as chaves do formato`() {
        val json = envelopeJson()
        val kdf = json.getJSONObject("kdf")
        val cipher = json.getJSONObject("cipher")

        assertThat(json.keys().asSequence().toSet())
            .isEqualTo(setOf("format", "formatVersion", "createdAtEpochMillis", "kdf", "cipher", "payload"))
        assertThat(kdf.keys().asSequence().toSet())
            .isEqualTo(setOf("algorithm", "iterations", "salt"))
        assertThat(cipher.keys().asSequence().toSet())
            .isEqualTo(setOf("algorithm", "iv"))
    }

    @Test
    fun `o envelope se identifica como backup do app na versao suportada`() {
        val json = envelopeJson()

        assertThat(json.getString("format")).isEqualTo("brainout-backup")
        assertThat(json.getInt("formatVersion")).isEqualTo(2)
    }

    /** O envelope serializado, como o app o grava em disco. */
    private fun envelopeJson(): JSONObject {
        val payload = BackupCrypto.encrypt("conteúdo".toByteArray(), SENHA)
        val bytes = BackupFormat.encodeEnvelope(1_760_000_000_000L, payload)
        return JSONObject(String(bytes, Charsets.UTF_8))
    }

    private companion object {
        const val SENHA: String = "senha-de-teste"
    }
}
