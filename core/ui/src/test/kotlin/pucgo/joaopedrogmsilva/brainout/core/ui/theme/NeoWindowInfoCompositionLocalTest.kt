// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// AC-0.3 — `LocalNeoWindowInfo` tem default seguro.
//
// Teste separado de `NeoWindowInfoTest` porque este precisa **montar uma
// composição de verdade** para ler a CompositionLocal sem nenhum
// `provides`. Montar composição no Compose runtime chama `android.os.Trace`,
// que não existe em JVM puro — daí o Robolectric, e não
// `returnDefaultValues = true`: esse switch silencia "esqueci de rodar
// sob Robolectric" em todo o módulo, inclusive nos testes que não têm nada
// a ver com Android.

package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

// `sdk = [34]` pela convenção do repositório (mesma razão dos outros testes de
// :core:ui: sem o pin o Robolectric assume o targetSdk 37 e falha na
// inicialização com `targetSdkVersion=37 > maxSdkVersion=35`).
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NeoWindowInfoCompositionLocalTest {
    @Test
    fun `sem provider a CompositionLocal devolve a janela mais apertada`() {
        // O contrato do AC-0.3: um @Preview ou um teste que não monte o shell
        // lê a CompositionLocal e não estoura. A leitura aqui passa pelo
        // caminho de produção — nenhum `provides` instalado.
        val read = currentInfo()

        assertThat(read.width).isEqualTo(NeoWidth.COMPACT)
        assertThat(read.height).isEqualTo(NeoHeight.COMPACT)
        assertThat(read.isWide).isFalse()
        assertThat(read.margin).isEqualTo(NeoLayout.compactMargin)
        assertThat(read.forceSingleColumn).isTrue()
    }

    @Test
    fun `a leitura sem provider devolve a mesma instancia do default`() {
        val read = currentInfo()

        assertThat(read).isSameInstanceAs(DefaultNeoWindowInfo)
    }

    @Test
    fun `com provider a CompositionLocal devolve a janela fornecida`() {
        // A outra metade do contrato: o default não é um valor travado. Shell
        // que mede a janela e fornece o resultado tem que ser respeitado.
        val wide =
            NeoWindowInfo.fromSize(
                NeoLayout.expandedMinWidth,
                NeoLayout.compactHeightMaxExclusive,
            )
        var read: NeoWindowInfo? = null
        val composition = Composition(UnitApplier(), Recomposer(EmptyCoroutineContext))

        composition.setContent {
            CompositionLocalProvider(LocalNeoWindowInfo provides wide) {
                read = LocalNeoWindowInfo.current
            }
        }

        assertThat(read).isSameInstanceAs(wide)
        assertThat(read?.width).isEqualTo(NeoWidth.EXPANDED)
        assertThat(read?.margin).isEqualTo(NeoLayout.expandedMargin)
    }

    private fun currentInfo(): NeoWindowInfo {
        var read: NeoWindowInfo? = null
        val composition = Composition(UnitApplier(), Recomposer(EmptyCoroutineContext))

        composition.setContent { read = LocalNeoWindowInfo.current }

        return checkNotNull(read) { "a composição não executou o conteúdo" }
    }

    /** Applier sem nós: só interessa a execução do conteúdo da composição. */
    private class UnitApplier : AbstractApplier<Unit>(Unit) {
        override fun insertBottomUp(
            index: Int,
            instance: Unit,
        ) = Unit

        override fun insertTopDown(
            index: Int,
            instance: Unit,
        ) = Unit

        override fun move(
            from: Int,
            to: Int,
            count: Int,
        ) = Unit

        override fun remove(
            index: Int,
            count: Int,
        ) = Unit

        override fun onClear() = Unit
    }
}
