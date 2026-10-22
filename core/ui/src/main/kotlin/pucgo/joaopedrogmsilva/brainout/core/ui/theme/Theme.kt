// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

/**
 * Tema Compose raiz do BrainOut — **identidade Neo** (paleta A).
 *
 * Este é o ponto de entrada único do tema e não mudou de nome nem de
 * assinatura: `MainActivity`, os testes de UI das features e as previews
 * continuam chamando `BrainOutTheme { … }` e recebem a identidade
 * neobrutalista.
 *
 * O que entra na árvore:
 *  - `colorScheme` de [NeoColors] via `toMaterialColorScheme` (nenhum
 *    resíduo do roxo `#6750A4` do Material), `LocalNeoColors` publicado
 *    para os componentes Neo;
 *  - `typography` = [NeoMaterialTypography], que carrega as cinco fontes
 *    empacotadas em `res/font` (offline, primeira abertura);
 *  - `shapes` = [NeoShapes] (4/4/8/8/16 dp).
 *
 * A paleta do sistema (Material You) **não** é uma opção: ela substituiria
 * a identidade Neo e quebraria a determinismo que os tokens Neo garantem.
 *
 * Alternância de tema (E4.5): continua seguindo a configuração do sistema
 * via `isSystemInDarkTheme()`, e ainda aceita override explícito de
 * `darkTheme` (previews, testes Compose). Nenhum toggle manual é
 * obrigatório — quando existir, deve persistir a escolha em DataStore
 * com default "seguir sistema" e passar a flag para cá.
 */
@Composable
fun BrainOutTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    BrainOutNeoTheme(darkTheme = darkTheme, content = content)
}
