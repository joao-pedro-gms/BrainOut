// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// NB-05: fontes estáticas locais — existência, instância real e licença.

package pucgo.joaopedrogmsilva.brainout.core.ui.assets

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

/**
 * Contrato de asset de fonte (NB-05, item 1):
 *
 *  1. Os cinco pesos do contrato `DESIGN.md` §4 existem como `.ttf` locais
 *     em `core/ui/src/main/res/font` — nenhuma fonte remota, nenhum download
 *     na primeira abertura;
 *  2. cada arquivo é uma **instância estática real** (sem tabela `fvar`,
 *     `OS/2.usWeightClass` correto, nome completo coerente) — é o que
 *     sustenta API 24/25, onde suporte a fonte variável não existe;
 *  3. a cobertura de glifos cobre pt-BR + EN sem cair no fallback;
 *  4. as licenças SIL OFL 1.1 acompanham os binários;
 *  5. todo não-ASCII usado em `strings.xml` do app existe nas duas
 *     famílias, menos os dois documentados em `ASSETS.md` §1.4.
 *
 * Os arquivos são lidos do disco, não do classpath: o que importa é o que
 * o AGP vai empacotar no APK.
 */
class NeoFontAssetsTest {

    private val fontDir: File = coreUiRes().resolve("font")

    @Test
    fun `o diretorio de fontes tem exatamente os cinco pesos do contrato`() {
        val files = fontDir.listFiles { file -> file.extension == "ttf" }.orEmpty()

        assertThat(files.map { it.nameWithoutExtension }.sorted())
            .isEqualTo(expectedFontAssets.map { it.resource }.sorted())
    }

    @Test
    fun `cada fonte e uma instancia estatica com nome e peso reais`() {
        expectedFontAssets.forEach { asset ->
            val file = fontDir.resolve("${asset.resource}.ttf")

            assertThat(file.isFile).isTrue()
            assertThat(file.length()).isGreaterThan(0L)
            assertThat(fullNameOf(file)).isEqualTo(asset.expectedFullName)
            assertThat(os2WeightOf(file)).isEqualTo(asset.expectedWeight)
        }
    }

    @Test
    fun `nenhuma fonte variavel foi embutida`() {
        expectedFontAssets.forEach { asset ->
            val file = fontDir.resolve("${asset.resource}.ttf")

            assertThat(isVariableFont(file)).isFalse()
        }
    }

    @Test
    fun `as fontes cobrem pt-BR e EN sem fallback`() {
        expectedFontAssets.forEach { asset ->
            val file = fontDir.resolve("${asset.resource}.ttf")

            assertThat(missingGlyphsOf(file, REQUIRED_TEXT)).isEmpty()
        }
    }

    @Test
    fun `todos os nao-ascii das strings do app existem nas fontes salvo as excecoes`() {
        val text = appStringNonAsciiCharacters()

        check(text.isNotEmpty()) { "nenhum caractere nao-ascii encontrado nas strings" }

        expectedFontAssets.forEach { asset ->
            val file = fontDir.resolve("${asset.resource}.ttf")

            assertThat(missingGlyphsOf(file, text)).isEqualTo(DOCUMENTED_FALLBACK_GLYPHS)
        }
    }

    @Test
    fun `as licencas OFL estao rastreaveis ao lado das fontes`() {
        val licenses = repoRoot().resolve("core/ui/licenses")
        val archivo = licenses.resolve("OFL-1.1-Archivo.txt")
        val publicSans = licenses.resolve("OFL-1.1-PublicSans.txt")

        assertThat(archivo.isFile).isTrue()
        assertThat(publicSans.isFile).isTrue()
        assertThat(archivo.readText()).contains(
            "Copyright 2020 The Archivo Project Authors",
        )
        assertThat(archivo.readText()).contains("SIL OPEN FONT LICENSE")
        assertThat(publicSans.readText()).contains(
            "Copyright 2015 The Public Sans Project Authors",
        )
        assertThat(publicSans.readText()).contains("SIL OPEN FONT LICENSE")
    }
}

/**
 * Os dois glifos que caem no fallback do sistema por decisão documentada
 * em `docs/design/ASSETS.md` §1.4: `→` existe só no Archivo upstream e
 * `⚠` não existe em nenhuma das duas famílias. Qualquer outro caractere
 * não-ASCII de `strings.xml` sem glifo real reprova o teste.
 */
private const val DOCUMENTED_FALLBACK_GLYPHS: String = "→⚠"
