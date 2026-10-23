// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Testes instrumentados do [PepperProvider].
 *
 * O pepper é guardado em `EncryptedSharedPreferences`, cuja chave
 * mestra vive no AndroidKeyStore — o Robolectric não implementa esse
 * provedor, então a suíte só roda em dispositivo ou emulador (ela
 * compila em `:core:data:assembleDebugAndroidTest`).
 *
 * O que precisa estar provado:
 *
 * - o pepper tem 16 bytes — 128 bits de entropia, o suficiente para
 *   neutralizar rainbow table mesmo sobre um dump do banco;
 * - ele é **estável**: o mesmo pepper entre chamadas e entre
 *   instâncias do provider. Sem isso, as senhas derivadas antes de um
 *   reinício do processo deixariam de verificar;
 * - apagar o armazenamento gera um pepper **novo** — o valor vem de
 *   `SecureRandom`, não de nada público. É a regressão do B1: a
 *   versão anterior derivava o pepper de `masterKey.toString()`, que
 *   é idêntico em todos os dispositivos, e o dump do banco bastava
 *   para verificar as senhas offline.
 *
 * Os nomes dos testes são em `snake_case`, sem espaços: a suíte é
 * transformada em DEX, e o DEX anterior à versão 040 (o `minSdk` do
 * projeto é 24) recusa nome de método com espaço — o erro só aparece
 * em `assembleDebugAndroidTest`, não no teste em si.
 */
@RunWith(AndroidJUnit4::class)
class PepperProviderInstrumentedTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun pepper_tem_16_bytes() {
        val pepper = PepperProvider.Default(context).bytes()

        assertThat(pepper.toList()).hasSize(16)
    }

    @Test
    fun pepper_e_estavel_entre_chamadas_e_instancias() {
        val primeiro = PepperProvider.Default(context).bytes()
        val segundo = PepperProvider.Default(context).bytes()

        assertThat(segundo.toList()).isEqualTo(primeiro.toList())
    }

    @Test
    fun apagar_o_armazenamento_gera_um_pepper_novo() {
        val original = PepperProvider.Default(context).bytes()

        context.deleteSharedPreferences(PepperProvider.Default.PEPPER_PREFS_NAME)
        val novo = PepperProvider.Default(context).bytes()

        assertThat(novo.toList()).hasSize(16)
        assertThat(novo.toList()).isNotEqualTo(original.toList())
    }
}
