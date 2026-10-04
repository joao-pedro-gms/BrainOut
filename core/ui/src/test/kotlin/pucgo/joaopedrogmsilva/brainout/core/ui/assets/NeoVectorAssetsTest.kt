// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// NB-05: ícones Material Symbols e arte geométrica como vetores locais.

package pucgo.joaopedrogmsilva.brainout.core.ui.assets

import com.google.common.truth.Truth.assertThat
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * Contrato de asset vetorial (NB-05, itens 3 e 4):
 *
 *  1. o manifesto de drawables é fechado — os 16 ícones usados pelo app e as
 *     4 artes originais, nada mais, nada menos (adicionar um asset exige
 *     atualizar o manifesto, que é a especificação);
 *  2. todo ícone é Material Symbols Outlined local em viewport 960 (o
 *     viewBox `0 -960 960 960` do SVG é deslocado por um `group`), sem
 *     cor remota e com espelhamento idêntico ao `AutoMirrored` do código;
 *  3. a arte é geométrica, com no máximo três cores da paleta A, traço
 *     uniforme e um único offset de sombra (Q28: ≤ 1 MB comprimido).
 *
 * O XML é lido do disco porque é exatamente o que o AGP empacota.
 */
class NeoVectorAssetsTest {

    private val drawableDir: File = coreUiRes().resolve("drawable")

    @Test
    fun `o manifesto de drawables tem os 16 icones e as 4 artes`() {
        val names = drawableDir.listFiles { file -> file.extension == "xml" }
            .orEmpty()
            .map { it.nameWithoutExtension }
            .sorted()

        assertThat(names).isEqualTo(
            (expectedIconAssets.keys + expectedArtAssets).sorted(),
        )
    }

    @Test
    fun `todo icone e um VectorDrawable 24dp com viewport 960 e path literal`() {
        expectedIconAssets.keys.forEach { name ->
            val vector = parseVector(drawableDir.resolve("$name.xml"))

            assertThat(vector.rootTag()).isEqualTo("vector")
            assertThat(vector.attr("width")).isEqualTo("24dp")
            assertThat(vector.attr("height")).isEqualTo("24dp")
            assertThat(vector.attr("viewportWidth")).isEqualTo(ICON_VIEWPORT)
            assertThat(vector.attr("viewportHeight")).isEqualTo(ICON_VIEWPORT)
            assertThat(vector.groups()).isNotEmpty()
            assertThat(vector.groupAttr(0, "translateY")).isEqualTo(ICON_VIEWPORT)

            val paths = vector.paths()
            assertThat(paths).isNotEmpty()
            paths.forEach { path ->
                assertThat(path.attr("pathData")).isNotEmpty()
                assertThat(path.attr("fillColor")).matches(HEX_COLOR)
            }
        }
    }

    @Test
    fun `autoMirrored aparece exatamente onde o codigo usa AutoMirrored`() {
        expectedIconAssets.forEach { (name, mirrored) ->
            val vector = parseVector(drawableDir.resolve("$name.xml"))

            assertThat(vector.hasAttr("autoMirrored")).isEqualTo(mirrored)
        }
    }

    @Test
    fun `a arte e geometrica com tres cores da paleta e traco uniforme`() {
        expectedArtAssets.forEach { name ->
            val vector = parseVector(drawableDir.resolve("$name.xml"))

            assertThat(vector.rootTag()).isEqualTo("vector")
            assertThat(vector.attr("width")).isEqualTo("120dp")
            assertThat(vector.attr("height")).isEqualTo("120dp")
            assertThat(vector.attr("viewportWidth")).isEqualTo(ART_VIEWPORT)
            assertThat(vector.attr("viewportHeight")).isEqualTo(ART_VIEWPORT)
            assertThat(vector.paths().size).isAtLeast(MIN_ART_PATHS)

            val fills = vector.paths()
                .map { it.attr("fillColor") }
                .filterNot { it == TRANSPARENT_FILL }
                .toSet()
            assertThat(allowedArtColors).containsAtLeastElementsIn(fills)
            assertThat(fills.size).isAtMost(MAX_ART_COLORS)

            val strokes = vector.paths()
                .map { it.attr("strokeColor") }
                .filter { it.isNotEmpty() }
                .toSet()
            assertThat(allowedArtColors).containsAtLeastElementsIn(strokes)

            val widths = vector.paths()
                .map { it.attr("strokeWidth") }
                .filter { it.isNotEmpty() }
                .toSet()
            assertThat(widths).hasSize(1)
        }
    }

    @Test
    fun `a soma de fontes e drawables cabe no teto de 1 MB`() {
        val fontFiles = coreUiRes().resolve("font").listFiles().orEmpty().toList()
        val drawableFiles = drawableDir.listFiles().orEmpty().toList()
        val assets = fontFiles + drawableFiles
        val total = assets.sumOf { it.length() }

        assertThat(assets).isNotEmpty()
        assertThat(total).isAtMost(ASSET_BUDGET_BYTES.toLong())
    }

    @Test
    fun `todo xml de asset carrega o cabecalho de autoria`() {
        val assets = drawableDir.listFiles { file -> file.extension == "xml" }
            .orEmpty()

        assertThat(assets).isNotEmpty()
        assets.forEach { file ->
            assertThat(file.readText()).startsWith(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "$AUTHOR_HEADER\n",
            )
        }
    }

    private fun parseVector(file: File): Document {
        assertThat(file.isFile).isTrue()
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        return factory.newDocumentBuilder().parse(file)
    }
}

private const val ICON_VIEWPORT = "960"
private const val ART_VIEWPORT = "120"
private const val MIN_ART_PATHS = 4
private const val MAX_ART_COLORS = 3
private const val HEX_COLOR = "#[0-9A-F]{8}"
private const val AUTHOR_HEADER =
    "<!-- João Pedro G M Silva - PUC Goiás ADS - 20251012000740 -->"

private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

private fun Document.rootTag(): String = documentElement.tagName

/** Atributo no elemento raiz `<vector>`. */
private fun Document.attr(name: String): String = documentElement.attr(name)

/** Presença de atributo no elemento raiz `<vector>`. */
private fun Document.hasAttr(name: String): Boolean = documentElement.hasAttr(name)

private fun Element.attr(name: String): String = getAttributeNS(ANDROID_NS, name)

private fun Element.hasAttr(name: String): Boolean = hasAttributeNS(ANDROID_NS, name)

private fun Document.paths(): List<Element> =
    getElementsByTagName("path").let { list ->
        (0 until list.length).map { list.item(it) as Element }
    }

private fun Document.groups(): List<Element> =
    getElementsByTagName("group").let { list ->
        (0 until list.length).map { list.item(it) as Element }
    }

private fun Document.groupAttr(index: Int, name: String): String =
    groups()[index].attr(name)
