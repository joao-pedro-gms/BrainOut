// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Rampa semântica de cor da prioridade de tarefa (códigos 0..4 do
// domínio). Um par de papéis Neo por nível, resolvido por
// [LocalNeoColors]: o mesmo par alimenta o indicador de prioridade
// (chip), a legenda do gráfico e as barras do Dashboard, então a cor
// nunca diverge entre as superfícies que mostram o mesmo nível.

package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Rampa de cor semântica por código de prioridade.
 *
 * | Código | Nível    | Papel Neo                              | Leitura    |
 * |--------|----------|----------------------------------------|------------|
 * | 0      | Baixa    | `surfaceAlt` / `textMuted`             | neutro     |
 * | 1      | Média    | `infoContainer` / `infoText`           | azul       |
 * | 2      | Alta     | `warningContainer` / `warningText`     | âmbar      |
 * | 3      | Urgente  | `secondaryContainer` / `secondaryText` | violeta    |
 * | 4      | Crítica  | `errorContainer` / `errorText`         | vermelho   |
 *
 * A escala sobe de neutralidade (0) para perigo (4) em temperatura;
 * Baixa e Média ficam distinguíveis (antes ambas usavam
 * `surfaceVariant`), e Alta deixa de usar o verde de `tertiaryContainer`
 * — verde carrega leitura de "sucesso" e competia com o estado
 * concluída. Todo par contêiner/texto já foi desenhado para texto
 * (`DESIGN.md` §6), então o contraste do chip é o contraste validado
 * do próprio papel.
 *
 * Cor é **reforço**, nunca a única via de distinção: o ícone
 * (`neo_ic_priority_*`) e o rótulo localizado acompanham o indicador
 * em todo ponto de uso (WCAG 1.4.1).
 */
private fun NeoColors.priorityContainer(code: Int): Color =
    when (code) {
        1 -> infoContainer
        2 -> warningContainer
        3 -> secondaryContainer
        4 -> errorContainer
        else -> surfaceAlt
    }

private fun NeoColors.priorityContent(code: Int): Color =
    when (code) {
        1 -> infoText
        2 -> warningText
        3 -> secondaryText
        4 -> errorText
        else -> textMuted
    }

/**
 * Papel de preenchimento do nível de prioridade [priorityCode] (0..4).
 * Uso: chips e barras — quem precisa da cor do texto usa
 * [neoPriorityContent] com o mesmo código.
 */
@Composable
fun neoPriorityContainer(priorityCode: Int): Color {
    require(priorityCode in 0..4) { "Prioridade fora da faixa 0..4: $priorityCode" }
    return LocalNeoColors.current.priorityContainer(priorityCode)
}

/**
 * Papel de texto (conteúdo) do nível de prioridade [priorityCode] (0..4).
 * Par de [neoPriorityContainer] — os dois resolvem do mesmo
 * [NeoColors], então contêiner e texto nunca vêm de temas diferentes.
 */
@Composable
fun neoPriorityContent(priorityCode: Int): Color {
    require(priorityCode in 0..4) { "Prioridade fora da faixa 0..4: $priorityCode" }
    return LocalNeoColors.current.priorityContent(priorityCode)
}
