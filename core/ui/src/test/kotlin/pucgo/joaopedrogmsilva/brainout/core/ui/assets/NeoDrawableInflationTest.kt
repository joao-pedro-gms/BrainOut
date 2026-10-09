// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// NB-05: os vetores empacotados inflam com o parser real do Android.

package pucgo.joaopedrogmsilva.brainout.core.ui.assets

import android.content.Context
import android.graphics.drawable.VectorDrawable
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Inflação dos 20 drawables de asset com o `PathParser` do próprio Android.
 *
 * `NeoVectorAssetsTest` prova que o XML está bem formado; este prova que o
 * `pathData` é aceito pelo parser da plataforma e vira um `VectorDrawable`
 * — é aqui que um comando SVG fora do suportado, um viewBox deslocado ou
 * um grupo malformado quebrariam em vez de renderizar em branco.
 *
 * Robolectric carrega os recursos mesclados do módulo (`isIncludeAndroidResources`
 * já está ligado no `build.gradle.kts`), então o que se infla é exatamente o
 * que o AGP empacota.
 */
@RunWith(RobolectricTestRunner::class)
// `sdk = [34]` pela convenção do repositório (AGENTS.md): sem o pin, o
// Robolectric assume o targetSdk 37 e falha no maxSdkVersion do android-all.
@Config(sdk = [34])
class NeoDrawableInflationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `todo asset vetorial infla como VectorDrawable com tamanho real`() {
        (expectedIconAssets.keys + expectedArtAssets).forEach { name ->
            val id = drawableId(name)

            assertThat(id).isGreaterThan(0)
            val drawable = context.resources.getDrawable(id, null)
            assertThat(drawable).isInstanceOf(VectorDrawable::class.java)
            assertThat(drawable.intrinsicWidth).isGreaterThan(0)
            assertThat(drawable.intrinsicHeight).isGreaterThan(0)
        }
    }

    @Test
    fun `as cinco fontes existem como recurso empacotado`() {
        expectedFontAssets.forEach { asset ->
            val id =
                context.resources.getIdentifier(
                    asset.resource,
                    "font",
                    context.packageName,
                )

            assertThat(id).isGreaterThan(0)
        }
    }

    private fun drawableId(name: String): Int = context.resources.getIdentifier(name, "drawable", context.packageName)
}
