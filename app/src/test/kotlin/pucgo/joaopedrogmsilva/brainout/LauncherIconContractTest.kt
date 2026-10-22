// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

/**
 * Densidades dos PNGs legados e o pixel de cada uma (API 24/25).
 */
private val legacyDensities: Map<String, Int> =
    mapOf(
        "mdpi" to 48,
        "hdpi" to 72,
        "xhdpi" to 96,
        "xxhdpi" to 144,
        "xxxhdpi" to 192,
    )

/** Paleta A do `DESIGN.md` §3, em notação Android. */
private val paletteA: Set<String> = setOf("#FF181818", "#FFF9D94A", "#FFB5A1F5")

/** As mesmas três cores, como pixel opaco, para conferir os PNGs legados. */
private val palettePixels: List<Int> =
    listOf(0xFFB5A1F5.toInt(), 0xFFF9D94A.toInt(), 0xFF181818.toInt())

/**
 * Contrato do ícone do launcher do BrainOut.
 *
 * O ícone é a marca do produto (`brand/brainout-icon.svg`): ficha yellow com
 * contorno ink, marca de conclusão e barra violeta sobre sombra rígida.
 * Este teste trava o que o resto do app já dá como certo — que o ícone do
 * launcher é a marca (e não um placeholder), que o adaptive icon declara as
 * três camadas, que os PNGs legados existem em todas as densidades e que a
 * fonte da marca está no repositório, versionada junto com o código.
 *
 * Sem `Robolectric`: é leitura de arquivo e de PNG, não de recurso Android.
 */
class LauncherIconContractTest {
    private val root: File = repoRoot()
    private val appRes: File = root.resolve("app/src/main/res")

    @Test
    fun `o adaptive icon declara as tres camadas da marca`() {
        listOf("ic_launcher.xml", "ic_launcher_round.xml").forEach { name ->
            val xml = appRes.resolve("mipmap-anydpi-v26/$name").readText()

            assertThat(xml).contains("""android:drawable="@drawable/ic_launcher_background"""")
            assertThat(xml).contains("""android:drawable="@drawable/ic_launcher_foreground"""")
            assertThat(xml).contains("""android:drawable="@drawable/ic_launcher_monochrome"""")
        }
    }

    @Test
    fun `as camadas do adaptive icon sao vetores na grade de 108 dp`() {
        listOf(
            "ic_launcher_background",
            "ic_launcher_foreground",
            "ic_launcher_monochrome",
        ).forEach { name ->
            val xml = appRes.resolve("drawable/$name.xml").readText()

            assertThat(xml).contains("""android:width="108dp"""")
            assertThat(xml).contains("""android:height="108dp"""")
            assertThat(xml).contains("""android:viewportWidth="108"""")
            assertThat(xml).contains("""android:viewportHeight="108"""")
        }
    }

    @Test
    fun `as cores das camadas do icone sao da paleta A`() {
        val xml = appRes.resolve("drawable/ic_launcher_foreground.xml").readText()
        val colors =
            Regex("""android:(?:fill|stroke)Color="(#[0-9A-F]{8})"""")
                .findAll(xml)
                .map { it.groupValues[1] }
                // `#00000000` é «sem preenchimento», não uma cor da paleta.
                .filterNot { it == "#00000000" }
                .toSet()

        assertThat(colors).isEqualTo(paletteA)
    }

    @Test
    fun `os icones legados existem em todas as densidades com o tamanho certo`() {
        legacyDensities.forEach { (density, size) ->
            listOf("ic_launcher.png", "ic_launcher_round.png").forEach { name ->
                val file = appRes.resolve("mipmap-$density/$name")

                assertThat(file.isFile).isTrue()
                val image = ImageIO.read(file)
                assertThat(image.width).isEqualTo(size)
                assertThat(image.height).isEqualTo(size)
            }
        }
    }

    @Test
    fun `os icones legados carregam a marca e nao sao um bloco chapado`() {
        listOf("ic_launcher.png", "ic_launcher_round.png").forEach { name ->
            val file = appRes.resolve("mipmap-xhdpi/$name")
            val image = ImageIO.read(file)
            val pixels = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
            val opaque = pixels.filter { it ushr 24 == 0xFF }.toSet()

            assertThat(opaque.size).isAtLeast(3)
            palettePixels.forEach { color ->
                assertThat(opaque).contains(color)
            }
        }
    }

    @Test
    fun `o manifesto aponta para o mipmap do icone`() {
        val manifest = root.resolve("app/src/main/AndroidManifest.xml").readText()

        assertThat(manifest).contains("""android:icon="@mipmap/ic_launcher"""")
        assertThat(manifest).contains("""android:roundIcon="@mipmap/ic_launcher_round"""")
    }

    @Test
    fun `a fonte da marca esta no repositorio e usa a paleta A`() {
        val svg = root.resolve("brand/brainout-icon.svg")
        val script = root.resolve("brand/generate_icons.py")

        assertThat(svg.isFile).isTrue()
        assertThat(script.isFile).isTrue()

        val content = svg.readText()
        listOf("#181818", "#F9D94A", "#B5A1F5").forEach { color ->
            assertThat(content).contains(color)
        }
    }

    /**
     * Raiz do repositório: o primeiro diretório acima do diretório de
     * trabalho que contém a fonte da marca. Falha alto quando não encontra
     * — um teste de asset que não verifica nada não pode passar em silêncio.
     */
    private fun repoRoot(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            if (dir.resolve("brand/brainout-icon.svg").isFile) return dir
            dir = dir.parentFile
        }
        error("brand/brainout-icon.svg não encontrado a partir de ${File("").absolutePath}")
    }
}
