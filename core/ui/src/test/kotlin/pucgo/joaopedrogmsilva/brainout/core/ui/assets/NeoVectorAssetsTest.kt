// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// NB-05: ícones Material Symbols e arte geométrica como vetores locais.

package pucgo.joaopedrogmsilva.brainout.core.ui.assets

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.w3c.dom.Attr
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

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
 *     literal de 6 unidades, um único offset de sombra de (+8,+8) e nada
 *     que não seja vetor: sem texto rasterizado, sem imagem e sem rede
 *     (Q28: ≤ 1 MB comprimido).
 *
 * O XML é lido do disco porque é exatamente o que o AGP empacota.
 */
class NeoVectorAssetsTest {
    private val drawableDir: File = coreUiRes().resolve("drawable")

    @Test
    fun `o manifesto de drawables tem os 16 icones e as 4 artes`() {
        val names =
            drawableDir
                .listFiles { file -> file.extension == "xml" }
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

            val fills =
                vector
                    .paths()
                    .map { it.attr("fillColor") }
                    .filterNot { it == TRANSPARENT_FILL }
                    .toSet()
            assertThat(allowedArtColors).containsAtLeastElementsIn(fills)
            assertThat(fills.size).isAtMost(MAX_ART_COLORS)

            val strokes =
                vector
                    .paths()
                    .map { it.attr("strokeColor") }
                    .filter { it.isNotEmpty() }
                    .toSet()
            assertThat(allowedArtColors).containsAtLeastElementsIn(strokes)

            val widths =
                vector
                    .paths()
                    .map { it.attr("strokeWidth") }
                    .filter { it.isNotEmpty() }
                    .toSet()
            assertThat(widths).containsExactly(ART_STROKE_WIDTH)
        }
    }

    @Test
    fun `a sombra da arte e um unico deslocamento de 8 8 sobre o traco`() {
        expectedArtAssets.forEach { name ->
            val paths = parseVector(drawableDir.resolve("$name.xml")).paths()
            val figures = paths.filter { it.attr("strokeColor").isNotEmpty() }

            // Traço e tinta andam juntos: largura sem cor não desenha nada.
            paths.forEach { path ->
                assertThat(path.attr("strokeWidth").isNotEmpty())
                    .isEqualTo(path.attr("strokeColor").isNotEmpty())
            }
            assertThat(figures).isNotEmpty()

            // A figura é o grupo com traço; o bbox dela é a base do offset.
            val figure =
                figures
                    .map { pathBox(it.attr("pathData")) }
                    .reduce { a, b ->
                        Box(
                            minOf(a.left, b.left),
                            minOf(a.top, b.top),
                            maxOf(a.right, b.right),
                            maxOf(a.bottom, b.bottom),
                        )
                    }
            val shadowBox =
                Box(
                    figure.left + SHADOW_OFFSET,
                    figure.top + SHADOW_OFFSET,
                    figure.right + SHADOW_OFFSET,
                    figure.bottom + SHADOW_OFFSET,
                )
            val shadows = paths.filter { pathBox(it.attr("pathData")) == shadowBox }

            assertThat(shadows).hasSize(1)
            val shadow = shadows.single()
            assertThat(shadow.attr("strokeColor")).isEmpty()
            assertThat(shadow.attr("strokeWidth")).isEmpty()
            assertThat(shadow.attr("fillColor")).isEqualTo(ART_SHADOW_FILL)
        }
    }

    @Test
    fun `a arte nao tem texto rasterizado imagem ou rede`() {
        expectedArtAssets.forEach { name ->
            val file = drawableDir.resolve("$name.xml")
            assertVectorMarkupOnly(file, parseVector(file))
        }
    }

    @Test
    fun `a soma de fontes e drawables cabe no teto de 1 MB`() {
        val fontFiles =
            coreUiRes()
                .resolve("font")
                .listFiles()
                .orEmpty()
                .toList()
        val drawableFiles = drawableDir.listFiles().orEmpty().toList()
        val assets = fontFiles + drawableFiles
        val total = assets.sumOf { it.length() }

        assertThat(assets).isNotEmpty()
        assertThat(total).isAtMost(ASSET_BUDGET_BYTES.toLong())
    }

    @Test
    fun `todo xml de asset carrega o cabecalho de autoria`() {
        val assets =
            drawableDir
                .listFiles { file -> file.extension == "xml" }
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

    /**
     * Fecha as três partes da regra «sem texto rasterizado, sem imagem, sem
     * rede» (`ASSETS.md` §3): vocabulário de elementos e atributos restrito
     * ao vetor, nenhum texto fora de whitespace e nenhuma URL além do
     * namespace que o próprio XML exige.
     */
    private fun assertVectorMarkupOnly(
        file: File,
        document: Document,
    ) {
        val nodes = document.getElementsByTagName("*")
        val elements = (0 until nodes.length).map { nodes.item(it) as Element }

        assertThat(elements.map { it.tagName }.toSet() - ART_TAGS).isEmpty()
        elements
            .flatMap { element ->
                (0 until element.attributes.length).map {
                    element.attributes.item(it) as Attr
                }
            }.filterNot { it.name.startsWith(XMLNS_PREFIX) }
            .forEach { attribute ->
                assertThat(attribute.namespaceURI).isEqualTo(ANDROID_NS)
                assertThat(attribute.localName).isIn(ART_ATTRIBUTES)
            }

        // Vetor poligonal não tem texto: sobra só whitespace entre tags.
        assertThat(document.documentElement.textContent.trim()).isEmpty()

        // A única URL do arquivo é o namespace exigido pelo próprio XML.
        val urls = URL_REGEX.findAll(file.readText()).map { it.value }.toSet()
        assertThat(urls - setOf(ANDROID_NS)).isEmpty()
    }
}

private const val ICON_VIEWPORT = "960"
private const val ART_VIEWPORT = "120"
private const val MIN_ART_PATHS = 4
private const val MAX_ART_COLORS = 3
private const val HEX_COLOR = "#[0-9A-F]{8}"
private const val AUTHOR_HEADER =
    "<!-- João Pedro G M Silva - PUC Goiás ADS - 20251012000740 -->"

/** Traço da arte: `ASSETS.md` §3 manda 6 unidades em todo arquivo. */
private const val ART_STROKE_WIDTH = "6"

/** Offset único de sombra do NB-04/NB-05: rígido, `+8,+8`, sem blur. */
private const val SHADOW_OFFSET = 8.0

/** A sombra é a silhueta em `ink` deslocada — mesma cor do traço. */
private const val ART_SHADOW_FILL = "#FF181818"

/** Únicos elementos admitidos num asset: nada de texto, imagem ou filtro. */
private val ART_TAGS: Set<String> = setOf("vector", "group", "path")

/** Únicos atributos android: admitidos na arte (sem `src`, `blur`…). */
private val ART_ATTRIBUTES: Set<String> =
    setOf(
        "width",
        "height",
        "viewportWidth",
        "viewportHeight",
        "fillColor",
        "pathData",
        "strokeColor",
        "strokeWidth",
    )

private val URL_REGEX = Regex("""https?://[^\s"'<>]+""")

/** Namespace das declarações `xmlns:*` — não é atributo `android:`. */
private const val XMLNS_PREFIX = "xmlns"

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

private fun Document.groupAttr(
    index: Int,
    name: String,
): String = groups()[index].attr(name)

/** Caixa envolvente de um `pathData` poligonal (`left, top, right, bottom`). */
private data class Box(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double,
)

private val PATH_TOKEN = Regex("""([MLHVZmlhvz])([^A-Za-z]*)""")
private val PATH_NUMBER = Regex("""-?\d+(?:\.\d+)?""")

/**
 * Bounding box do `pathData`. Só os comandos poligonais usados pela arte
 * (`M m L l H h V v Z z`); comando curvo (`Q`, `C`…) é erro alto, porque
 * geometria arredondada não é a assinatura do NB-05.
 */
private fun pathBox(pathData: String): Box {
    var x = 0.0
    var y = 0.0
    var startX = 0.0
    var startY = 0.0
    var left = Double.POSITIVE_INFINITY
    var top = Double.POSITIVE_INFINITY
    var right = Double.NEGATIVE_INFINITY
    var bottom = Double.NEGATIVE_INFINITY

    fun visit(
        nx: Double,
        ny: Double,
    ) {
        x = nx
        y = ny
        left = minOf(left, x)
        top = minOf(top, y)
        right = maxOf(right, x)
        bottom = maxOf(bottom, y)
    }

    PATH_TOKEN.findAll(pathData).forEach { match ->
        val command = match.groupValues[1]
        val relative = command.first().isLowerCase()
        val numbers =
            PATH_NUMBER
                .findAll(match.groupValues[2])
                .map { it.value.toDouble() }
                .toList()

        when (command.uppercase()) {
            "M" ->
                numbers.chunked(2).forEach { pair ->
                    check(pair.size == 2) { "par x,y esperado em M: $pathData" }
                    visit(
                        if (relative) x + pair[0] else pair[0],
                        if (relative) y + pair[1] else pair[1],
                    )
                    startX = x
                    startY = y
                }
            "L" ->
                numbers.chunked(2).forEach { pair ->
                    check(pair.size == 2) { "par x,y esperado em L: $pathData" }
                    visit(
                        if (relative) x + pair[0] else pair[0],
                        if (relative) y + pair[1] else pair[1],
                    )
                }
            "H" -> numbers.forEach { visit(if (relative) x + it else it, y) }
            "V" -> numbers.forEach { visit(x, if (relative) y + it else it) }
            "Z" -> visit(startX, startY)
            else -> error("comando não poligonal em pathData da arte: $command")
        }
    }

    check(left.isFinite()) { "pathData sem coordenadas: $pathData" }
    return Box(left, top, right, bottom)
}
