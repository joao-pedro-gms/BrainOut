// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

/**
 * Smoke test de internacionalização (E4.6).
 *
 * Garante que `R.string.app_name` resolve para texto não vazio tanto na
 * localidade padrão (pt-BR, `values/`) quanto na tradução em inglês
 * (`values-en/`), via Robolectric. O qualifier padrão `pt` vem de
 * `app/src/test/resources/robolectric.properties`; a localidade `en` é
 * exercitada com um `ConfigurationContext` com `Locale("en")`.
 */
@RunWith(RobolectricTestRunner::class)
class I18nStringsSmokeTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Contexto com a localidade passada, preservando os demais qualifiers. */
    private fun contextFor(locale: Locale): Context =
        context.createConfigurationContext(Configuration().apply { setLocale(locale) })

    @Test
    fun `app_name resolves non-empty in default locale pt`() {
        val name = context.getString(R.string.app_name)
        assertThat(name).isNotEmpty()
    }

    @Test
    fun `app_name resolves non-empty in en`() {
        val name = contextFor(Locale("en")).getString(R.string.app_name)
        assertThat(name).isNotEmpty()
        assertThat(name).isEqualTo("BrainOut")
    }

    @Test
    fun `tagline is localized in both pt and en`() {
        val pt = contextFor(Locale("pt")).getString(R.string.splash_tagline)
        val en = contextFor(Locale("en")).getString(R.string.splash_tagline)
        // A chave existe nos dois conjuntos (lint MissingTranslation garante
        // a paridade); traduções efetivas devem diferir entre as línguas,
        // provando que o lookup de `values-en` não está quebrado.
        assertThat(pt).isNotEmpty()
        assertThat(en).isNotEmpty()
        assertThat(en).isNotEqualTo(pt)
    }
}
