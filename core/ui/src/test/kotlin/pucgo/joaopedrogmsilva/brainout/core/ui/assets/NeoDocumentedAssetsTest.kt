// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// NB-05: os números de bytes e sha256 publicados em `docs/design/ASSETS.md`.

package pucgo.joaopedrogmsilva.brainout.core.ui.assets

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * `docs/design/ASSETS.md` é a entrega de proveniência do NB-05: publica
 * contagens, bytes e sha256 de tudo que o `:core:ui` empacota. Nenhum
 * desses números era assinado por teste — foi assim que a correção dos
 * `neo_art_*.xml` (+1 byte em cada) deixou seis células da tabela erradas
 * até a revisão conferir à mão com `wc -c` e `du -cb`.
 *
 * Este teste assina o documento: lê o markdown do disco e compara com os
 * arquivos reais, um número de cada vez.
 *
 *  1. inventário da §1 — bytes e contagens das três categorias, o
 *     subtotal de arte e o total;
 *  2. §1.3 — bytes e os cinco sha256 dos `.ttf`;
 *  3. §3 — os quatro tamanhos da arte.
 *
 * O documento continua fonte de verdade editorial: o teste acusa
 * divergência e nunca reescreve a tabela. Quando acusa, o caminho é
 * decidir qual dos dois está errado — o disco (asset trocado sem
 * documentar) ou o documento (tabela desatualizada) — antes de mexer em
 * qualquer um; «ajustar» o teste para o disco seria apagar o sinal.
 *
 * O parsing é texto puro sobre markdown, sem dependência nova no
 * classpath.
 */
class NeoDocumentedAssetsTest {
    private val document: File = repoRoot().resolve("docs/design/ASSETS.md")
    private val fontDir: File = coreUiRes().resolve("font")
    private val drawableDir: File = coreUiRes().resolve("drawable")

    private val tables: List<MarkdownTable> = markdownTables(document)

    private val fontFiles: List<File> = filesWith(fontDir, "", "ttf")
    private val iconFiles: List<File> = filesWith(drawableDir, "neo_ic_", "xml")
    private val artFiles: List<File> = filesWith(drawableDir, "neo_art_", "xml")

    @Test
    fun `a tabela do inventario da secao 1 bate com bytes e contagens do disco`() {
        val table = tables.table(INVENTORY) { header -> header.firstOrNull() == "Conjunto" }
        val bytes = table.column("Bytes")
        val count = table.column("Arquivos")

        val scopes = inventory()
        scopes.forEach { scope -> assertScope(table, scope, bytes, count) }

        // O total precisa fechar com a soma das três categorias no disco,
        // que é o que o `du -cb` do §5 imprime.
        val everything = scopes.flatMap { it.files }
        assertScope(table, AssetScope("Total", "Total", everything, RES_SOURCE), bytes, count)
    }

    @Test
    fun `os cinco tamanhos da tabela de fontes empacotadas batem com os ttf do disco`() {
        val table = fontsTable()
        val bytes = table.column("Bytes")

        table.assertNoUnknownRows(FONTS, fontFiles.map { it.nameWithoutExtension })
        fontFiles.forEach { file ->
            val cells = table.rowContaining(FONTS, file.nameWithoutExtension)

            assertDocumented(
                where = FONTS,
                label = file.nameWithoutExtension,
                column = "Bytes",
                cell = cells[bytes],
                disk = file.length(),
                source = FONT_SOURCE,
            )
        }
    }

    @Test
    fun `os cinco sha256 da tabela de fontes empacotadas batem com os ttf do disco`() {
        val table = fontsTable()
        val sha = table.column("sha256")

        fontFiles.forEach { file ->
            val cells = table.rowContaining(FONTS, file.nameWithoutExtension)
            val published = cells[sha].trim().removeSurrounding("`").lowercase()

            assertWithMessage(
                "ASSETS.md %s, linha «%s», sha256 malformado na tabela: «%s»",
                FONTS,
                file.nameWithoutExtension,
                cells[sha],
            ).that(published).matches(SHA256_HEX.pattern)

            val onDisk = sha256Of(file)
            assertWithMessage(
                "ASSETS.md %s, linha «%s», sha256: documento = %s, disco = %s (%s)",
                FONTS,
                file.nameWithoutExtension,
                published,
                onDisk,
                FONT_SOURCE,
            ).that(onDisk).isEqualTo(published)
        }
    }

    @Test
    fun `os quatro tamanhos da secao 3 batem com a arte do disco`() {
        val table =
            tables.table(ART) { header ->
                header.firstOrNull() == "Recurso" && header.lastOrNull() == "Bytes"
            }
        val bytes = table.column("Bytes")

        table.assertNoUnknownRows(ART, artFiles.map { it.nameWithoutExtension })
        artFiles.forEach { file ->
            val cells = table.rowContaining(ART, file.nameWithoutExtension)

            assertDocumented(
                where = ART,
                label = file.nameWithoutExtension,
                column = "Bytes",
                cell = cells[bytes],
                disk = file.length(),
                source = DRAWABLE_SOURCE,
            )
        }
    }

    /** As três linhas de categoria do inventário, na chave que as identifica. */
    private fun inventory(): List<AssetScope> =
        listOf(
            AssetScope("`res/font/`", "Fontes", fontFiles, FONT_SOURCE),
            AssetScope("`res/drawable/neo_ic_*.xml`", "Ícones", iconFiles, DRAWABLE_SOURCE),
            AssetScope("`res/drawable/neo_art_*.xml`", "Arte", artFiles, DRAWABLE_SOURCE),
        )

    private fun assertScope(
        table: MarkdownTable,
        scope: AssetScope,
        bytes: Int,
        count: Int,
    ) {
        val cells = table.rowContaining(INVENTORY, scope.key)
        val onDisk = scope.files.sumOf { it.length() }

        assertDocumented(
            where = INVENTORY,
            label = scope.name,
            column = "Arquivos",
            cell = cells[count],
            disk = scope.files.size.toLong(),
            source = scope.source,
        )
        assertDocumented(
            where = INVENTORY,
            label = scope.name,
            column = "Bytes",
            cell = cells[bytes],
            disk = onDisk,
            source = scope.source,
        )
    }

    /** Tabela da §1.3: identificada por `Recurso` na frente e `sha256` no fim. */
    private fun fontsTable(): MarkdownTable =
        tables.table(FONTS) { header ->
            header.firstOrNull() == "Recurso" && header.lastOrNull() == "sha256"
        }

    /**
     * Compara um número publicado com o do disco. A falha mostra o texto
     * exato da célula, o valor lido do arquivo e onde ele está — é o caminho
     * curto de `ASSETS.md` até `wc -c`.
     */
    private fun assertDocumented(
        where: String,
        label: String,
        column: String,
        cell: String,
        disk: Long,
        source: String,
    ) {
        val digits =
            NUMBER_IN_CELL
                .find(squeezeSpaces(cell))
                ?.value
                .orEmpty()
                .filterNot { it.isWhitespace() }
        val published =
            checkNotNull(digits.toIntOrNull()) {
                "ASSETS.md %s, linha «%s», coluna %s: não achei número em «%s»".format(
                    where,
                    label,
                    column,
                    cell.trim(),
                )
            }

        assertWithMessage(
            "ASSETS.md %s, linha «%s», coluna %s: documento = %s, disco = %s (%s)",
            where,
            label,
            column,
            cell.trim(),
            formatGrouped(disk),
            source,
        ).that(disk).isEqualTo(published.toLong())
    }
}

/** Uma linha do inventário: a chave que a localiza, o rótulo e o que ela cobre. */
private data class AssetScope(
    val key: String,
    val name: String,
    val files: List<File>,
    val source: String,
)

/** Tabela markdown: cabeçalho e linhas de dados (separador já removido). */
private data class MarkdownTable(
    val header: List<String>,
    val rows: List<List<String>>,
)

private const val INVENTORY = "§1 (inventário)"
private const val FONTS = "§1.3 (fontes empacotadas)"
private const val ART = "§3 (arte)"

private const val FONT_SOURCE = "core/ui/src/main/res/font"
private const val DRAWABLE_SOURCE = "core/ui/src/main/res/drawable"
private const val RES_SOURCE = "core/ui/src/main/res"

/** Número de uma célula: dígitos com separador de milhar, opcionalmente em negrito. */
private val NUMBER_IN_CELL = Regex("""\d[\d\s]*""")

private val SHA256_HEX = Regex("[0-9a-f]{64}")

/**
 * Tabela colada de outro lugar traz o separador de milhar como espaço não
 * separável; vira espaço comum para a leitura do número não depender disso.
 */
private fun squeezeSpaces(text: String): String =
    text
        .map { char -> if (Character.isSpaceChar(char) || char.isWhitespace()) ' ' else char }
        .joinToString("")

/**
 * Lê `docs/design/ASSETS.md` e devolve as tabelas markdown que ele contém.
 * Falha alto quando o documento não existe: um teste de asset que não
 * encontra o que deveria verificar não pode passar em silêncio.
 */
private fun markdownTables(file: File): List<MarkdownTable> {
    check(file.isFile) { "ASSETS.md não encontrado em ${file.path}" }

    val tables = mutableListOf<MarkdownTable>()
    var buffer = mutableListOf<String>()

    fun flush() {
        if (buffer.size >= TABLE_MIN_LINES) {
            val cells =
                buffer.map { line ->
                    line
                        .trim()
                        .removePrefix("|")
                        .removeSuffix("|")
                        .split("|")
                        .map { it.trim() }
                }
            val rows =
                cells.drop(1).filterNot { row ->
                    row.isNotEmpty() && row.all { it.matches(SEPARATOR_CELL) }
                }
            tables += MarkdownTable(cells.first(), rows)
        }
        buffer = mutableListOf()
    }

    file.readLines().forEach { line ->
        if (line.trimStart().startsWith("|")) {
            buffer += line
        } else {
            flush()
        }
    }
    flush()

    return tables
}

/** Localiza uma tabela pela assinatura do cabeçalho; exatamente uma. */
private fun List<MarkdownTable>.table(
    where: String,
    signature: (List<String>) -> Boolean,
): MarkdownTable {
    val found = filter { table -> signature(table.header) }

    check(found.size == 1) { "ASSETS.md: não achei (ou achei mais de uma) tabela da $where" }
    return found.single()
}

/** Índice da coluna pelo nome no cabeçalho — a ordem da tabela pode mudar. */
private fun MarkdownTable.column(name: String): Int {
    val index = header.indexOf(name)

    check(index >= 0) { "ASSETS.md: coluna «$name» ausente no cabeçalho $header" }
    return index
}

/** Linha da tabela cuja primeira célula contém [key] — exatamente uma. */
private fun MarkdownTable.rowContaining(
    where: String,
    key: String,
): List<String> {
    val found = rows.filter { row -> row.firstOrNull()?.contains(key) == true }

    check(found.size == 1) {
        "ASSETS.md: linha «$key» da tabela da $where não encontrada (ou duplicada)"
    }
    return found.single()
}

/**
 * Nenhuma linha documentando arquivo que o disco não tem: `ASSETS.md` não
 * pode prometer asset que ninguém empacota.
 */
private fun MarkdownTable.assertNoUnknownRows(
    where: String,
    onDisk: List<String>,
) {
    val documented = rows.map { row -> row.first().trim().removeSurrounding("`") }
    val unknown = documented - onDisk.toSet()

    assertWithMessage(
        "ASSETS.md %s: linhas sem arquivo no disco = %s; disco = %s",
        where,
        unknown,
        onDisk,
    ).that(unknown).isEmpty()
}

/** sha256 em hex minúsculo, como a tabela publica. */
private fun sha256Of(file: File): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest(file.readBytes())
        .joinToString("") { byte -> "%02x".format(byte) }

/** Arquivos de [dir] pelo prefixo e extensão, em ordem estável. */
private fun filesWith(
    dir: File,
    prefix: String,
    extension: String,
): List<File> =
    dir
        .listFiles { file -> file.name.startsWith(prefix) && file.extension == extension }
        .orEmpty()
        .sortedBy { file -> file.name }

/** `299463` -> `299 463`, o formato que o documento usa. */
private fun formatGrouped(value: Long): String =
    value
        .toString()
        .reversed()
        .chunked(GROUP_DIGITS)
        .joinToString(" ")
        .reversed()

private const val GROUP_DIGITS = 3
private const val TABLE_MIN_LINES = 2
private val SEPARATOR_CELL = Regex("-+:?")
