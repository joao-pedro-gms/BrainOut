// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Fixtures compartilhadas dos testes de asset do NB-05 (fontes, ícones, arte).

package pucgo.joaopedrogmsilva.brainout.core.ui.assets

import java.awt.Font
import java.io.File

/**
 * Cobertura mínima exigida das fontes: pt-BR, EN e a pontuação que a UI
 * realmente usa. Corresponde à validação pedida em `DESIGN.md` §4
 * («Ação», «Conclusion», «João», «ç», números, e-mail longo e texto EN).
 * Símbolos matemáticos (≥, ≤) não entram: não aparecem em nenhuma string
 * do app e o subset é deliberado.
 */
internal const val REQUIRED_TEXT: String =
    "Ação Conclusão João ç ã õ á é í ó ú à â ê Ü " +
        "0123456789 +55 (11) 99999-0000 joao.silva+puc@exemplo.edu.br " +
        "The quick brown fox jumps over the lazy dog — “quoted”, ‘single’… • 100% €9,99"

/**
 * Recurso de fonte embutido: nome do arquivo (sem extensão), nome completo
 * que a tabela `name` precisa declarar e o peso que `OS/2.usWeightClass`
 * precisa carregar. Os três juntos são o que garante instância estática
 * real — não um rótulo por cima de um arquivo variável.
 */
internal data class FontAsset(
    val resource: String,
    val expectedFullName: String,
    val expectedWeight: Int,
)

/** Os cinco pesos do contrato tipográfico de `DESIGN.md` §4. */
internal val expectedFontAssets: List<FontAsset> =
    listOf(
        FontAsset("archivo_semi_bold", "Archivo SemiBold", 600),
        FontAsset("archivo_bold", "Archivo Bold", 700),
        FontAsset("archivo_extra_bold", "Archivo ExtraBold", 800),
        FontAsset("public_sans_regular", "Public Sans Regular", 400),
        FontAsset("public_sans_semi_bold", "Public Sans SemiBold", 600),
    )

/**
 * Manifesto de ícones: recurso Android -> espelha em RTL.
 *
 * O espelhamento é o do código atual (`Icons.AutoMirrored.*`); símbolos de
 * estado não são espelhados, conforme `DESIGN.md` §5.
 */
internal val expectedIconAssets: Map<String, Boolean> =
    linkedMapOf(
        "neo_ic_add" to false,
        "neo_ic_arrow_back" to true,
        "neo_ic_assignment" to true,
        "neo_ic_bar_chart" to false,
        "neo_ic_chevron_right" to false,
        "neo_ic_close" to false,
        "neo_ic_cloud_off" to false,
        "neo_ic_cloud_sync" to false,
        "neo_ic_delete" to false,
        "neo_ic_folder" to false,
        "neo_ic_logout" to true,
        "neo_ic_more_vert" to false,
        "neo_ic_settings" to false,
        "neo_ic_sort" to true,
        "neo_ic_visibility" to false,
        "neo_ic_visibility_off" to false,
    )

/** Arte geométrica original (splash + três empty states), Q28. */
internal val expectedArtAssets: List<String> =
    listOf(
        "neo_art_splash_poster",
        "neo_art_empty_projects",
        "neo_art_empty_tasks",
        "neo_art_empty_dashboard",
    )

/** Cores da paleta A admitidas na arte (DESIGN.md §3). */
internal val allowedArtColors: Set<String> =
    setOf(
        "#FF181818", // ink
        "#FFF9D94A", // yellow
        "#FFB5A1F5", // violet
        "#FFA5D8FF", // blue
        "#FFFFFFFF", // white
    )

/** Teto de asset do Q28: fontes + drawables de `:core:ui` em 1 MB. */
internal const val ASSET_BUDGET_BYTES: Int = 1_048_576

/** Cores transparentes de VectorDrawable significam «sem preenchimento». */
internal const val TRANSPARENT_FILL: String = "#00000000"

/**
 * Resolve `core/ui/src/main/res` subindo a árvore a partir do diretório de
 * trabalho do teste (que pode ser a raiz do repositório ou o módulo).
 *
 * Falha alto quando não encontra: pular silenciosamente deixaria os testes
 * de asset verificando nada.
 */
internal fun coreUiRes(): File = repoRoot().resolve("core/ui/src/main/res")

/** Raiz do repositório encontrada a partir do diretório de trabalho. */
internal fun repoRoot(): File {
    var dir: File? = File("").absoluteFile
    while (dir != null) {
        if (dir.resolve("core/ui/src/main/res").isDirectory) return dir
        dir = dir.parentFile
    }
    error(
        "core/ui/src/main/res não encontrado a partir de ${File("").absolutePath}",
    )
}

/**
 * Todos os caracteres não-ASCII usados em `strings.xml` do repositório
 * (qualquer módulo, `values/` e variantes), lidos do disco como o AGP os
 * lê. Falha alto quando não encontra arquivo nenhum: pular em silêncio
 * deixaria a cobertura de glifos do app sem nada para verificar.
 */
internal fun appStringNonAsciiCharacters(): String {
    val root = repoRoot()
    val files =
        root
            .walkTopDown()
            .onEnter { dir -> dir.name != ".git" }
            .filter { file ->
                file.isFile &&
                    file.name == "strings.xml" &&
                    file.path.contains(
                        "${File.separator}src${File.separator}main" +
                            "${File.separator}res${File.separator}values",
                    )
            }.toList()

    check(files.isNotEmpty()) { "nenhum strings.xml sob $root" }

    val chars = mutableSetOf<Char>()
    files.forEach { file ->
        file.readText().forEach { char -> if (char.code > 127) chars.add(char) }
    }

    return chars.sortedBy { it.code }.joinToString("")
}

/** Lê `OS/2.usWeightClass` de um TTF: o peso real do arquivo, não o rótulo. */
internal fun os2WeightOf(file: File): Int {
    val bytes = file.readBytes()
    val offset = tableOffset(bytes, "OS/2")
    return readUint16(bytes, offset + 4)
}

/** `true` se o arquivo carrega a tabela `fvar` (isto é, é variável). */
internal fun isVariableFont(file: File): Boolean =
    runCatching {
        tableOffset(file.readBytes(), "fvar")
    }.isSuccess

/** Nome completo (`name` ID4) do arquivo, lido pela AWT. */
internal fun fullNameOf(file: File): String =
    Font.createFont(Font.TRUETYPE_FONT, file).getFontName(java.util.Locale.ROOT)

/** Caracteres de [text] que a fonte não consegue desenhar. */
internal fun missingGlyphsOf(
    file: File,
    text: String,
): String {
    val font = Font.createFont(Font.TRUETYPE_FONT, file)
    return buildString {
        text.forEach { char ->
            if (!font.canDisplay(char)) append(char)
        }
    }
}

private fun tableOffset(
    bytes: ByteArray,
    tag: String,
): Int {
    val tableCount = readUint16(bytes, 4)
    var cursor = 12
    repeat(tableCount) {
        val current = String(bytes, cursor, 4, Charsets.US_ASCII)
        if (current == tag) return readUint32(bytes, cursor + 8)
        cursor += 16
    }
    error("tabela '$tag' ausente no arquivo")
}

private fun readUint16(
    bytes: ByteArray,
    offset: Int,
): Int =
    ((bytes[offset].toInt() and 0xFF) shl 8) or
        (bytes[offset + 1].toInt() and 0xFF)

private fun readUint32(
    bytes: ByteArray,
    offset: Int,
): Int =
    ((bytes[offset].toInt() and 0xFF) shl 24) or
        ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
        ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
        (bytes[offset + 3].toInt() and 0xFF)
