// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/**
 * Os cinco slots de `Shapes` do Material 3 preenchidos com os raios Neo
 * (`DESIGN.md` §5, NB-04).
 *
 * O mapeamento é o da tabela da especificação — «extraSmall 4; small 4;
 * medium 8; large 8; extraLarge 16» — e cada slot aponta para o token
 * pelo nome do uso, não para o número: `chip`/`field` são os dois 4 dp,
 * `button`/`card` os dois 8 dp e `modal` o 16 dp. Não há valor literal
 * aqui, para que a paridade com `tokens.json` (coberta por
 * `NeoTokenParityTest`) continue valendo por construção.
 *
 * O raio zero (`NeoRadii.none`, de barras e gráficos) não tem slot no
 * `Shapes`: quem desenha essas formas usa `NeoRadii.none` direto.
 */
val NeoShapes: Shapes =
    Shapes(
        extraSmall = RoundedCornerShape(NeoRadii.chip),
        small = RoundedCornerShape(NeoRadii.field),
        medium = RoundedCornerShape(NeoRadii.button),
        large = RoundedCornerShape(NeoRadii.card),
        extraLarge = RoundedCornerShape(NeoRadii.modal),
    )
