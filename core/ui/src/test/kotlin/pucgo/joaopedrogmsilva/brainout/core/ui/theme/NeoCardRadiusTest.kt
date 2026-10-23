// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// A guarda de conformidade do raio dos cards nas features.

package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.ui.assets.repoRoot
import java.io.File

/**
 * Todo card das features usa o raio do card da identidade.
 *
 * A identidade fixa 8 dp para cards — `DESIGN.md` §raios ("8
 * botões/cards/menus"), `docs/design/tokens.json` (`materialShapes.large`)
 * e o token [NeoRadii.card], que o `NeoTokenParityTest` já prende contra
 * o JSON.
 *
 * O que o teste de paridade **não** vê é o uso. O app chegou a ter três
 * raios para o mesmo conceito: 12 dp (literal, o raio `large` do Material
 * legado que sobreviveu à ativação Neo) em sete superfícies, 16 dp (via
 * `NeoRadii.modal`) em duas, e o token de 8 dp sem uso em lugar nenhum.
 *
 * Este teste fecha essa porta: nenhuma superfície das features define o
 * próprio formato com um raio que não seja [NeoRadii.card]. Sem ele,
 * voltar a 12 dp é uma digitação — e o olho não pega, porque 12 dp contra
 * 8 dp é meia dúzia de milímetros de borda.
 *
 * A regra olha só a posição `shape =` — o formato de uma superfície. Os
 * `clip(RoundedCornerShape(...))` de linha não são formato de superfície,
 * e a especificação não define raio de linha: eles ficam de fora de
 * propósito, não por esquecimento.
 */
class NeoCardRadiusTest {
    @Test
    fun `nenhuma superficie das features define um raio fora do token do card`() {
        val offenders = mutableListOf<String>()

        featureSources().forEach { file ->
            val text = file.readText()
            CARD_SHAPE.findAll(text).forEach { match ->
                val radius = match.groupValues[1].trim()
                if (radius != "NeoRadii.card") {
                    val path = file.relativeTo(repoRoot()).path.replace('\\', '/')
                    offenders += "$path: shape = RoundedCornerShape($radius)"
                }
            }
        }

        assertThat(offenders).isEmpty()
    }

    /** Os fontes de produção das features — nada de teste, nada de build. */
    private fun featureSources(): List<File> =
        repoRoot()
            .resolve("feature")
            .walkTopDown()
            .onEnter { dir -> dir.name != "build" }
            .filter { file ->
                file.isFile &&
                    file.extension == "kt" &&
                    file.path.contains("${File.separator}src${File.separator}main${File.separator}")
            }.toList()

    private companion object {
        /** O formato de uma superfície: `shape = RoundedCornerShape(<raio>)`. */
        val CARD_SHAPE = Regex("""shape\s*=\s*RoundedCornerShape\(([^)]*)\)""")
    }
}
