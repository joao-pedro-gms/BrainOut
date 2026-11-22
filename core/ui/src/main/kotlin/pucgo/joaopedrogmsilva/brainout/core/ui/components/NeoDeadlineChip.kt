// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Chip de prazo (E2.7) — exibe a data de vencimento de uma tarefa com
// semáforo de proximidade: atrasada (vermelho), vence hoje (âmbar),
// vence esta semana (neutro), depois disso (neutro). Sem prazo, o
// chamador simplesmente não compõe o chip.
//
// A classificação é apresentação pura: `deadlineProximity` compara
// epoch-millis com `java.util.Calendar` (API 1, sem desugaring — este
// módulo não declara `coreLibraryDesugaring`) e o padrão da data vem
// do `strings.xml` (localeável, como em `DeadlineField`).

package pucgo.joaopedrogmsilva.brainout.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.ui.R
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.LocalNeoColors
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoBorders
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSpacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Proximidade do prazo em relação a "agora", para o semáforo do chip. */
enum class NeoDeadlineProximity {
    OVERDUE,
    TODAY,
    THIS_WEEK,
    LATER,
}

/**
 * Classifica a proximidade de [dueDateMillis] em relação a [nowMillis],
 * ambos em epoch-millis no fuso local.
 *
 * A comparação é por **dia civil local** (start-of-day), não por hora: a
 * tarefa vence "hoje" se o dia dela for o dia de hoje, independente da
 * hora. O limite da semana usa `Calendar.add`, que é DST-corrente — a
 * janela de 7 dias nunca encolhe ou cresce por mudança de fuso.
 */
internal fun deadlineProximity(
    dueDateMillis: Long,
    nowMillis: Long,
): NeoDeadlineProximity {
    fun startOfDay(millis: Long): Calendar =
        Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    val nowDay = startOfDay(nowMillis)
    val dueDay = startOfDay(dueDateMillis)
    val isToday =
        dueDay.get(Calendar.YEAR) == nowDay.get(Calendar.YEAR) &&
            dueDay.get(Calendar.DAY_OF_YEAR) == nowDay.get(Calendar.DAY_OF_YEAR)
    val weekLimit =
        nowDay.clone() as Calendar

    weekLimit.add(Calendar.DAY_OF_YEAR, 7)

    return when {
        dueDay.before(nowDay) -> NeoDeadlineProximity.OVERDUE
        isToday -> NeoDeadlineProximity.TODAY
        dueDay.after(weekLimit) -> NeoDeadlineProximity.LATER
        else -> NeoDeadlineProximity.THIS_WEEK
    }
}

/**
 * Chip informativo do prazo de uma tarefa: ícone + data formatada no
 * padrão local (`neo_deadline_date_format`), com semáforo de proximidade
 * no fundo. Não é interativo (mesmo contrato de [NeoInfoChip], DEF-22):
 * sem `onClick`, e o nó inteiro é lido como rótulo pelo TalkBack.
 *
 * A cor é reforço, não a única pista: a descrição falada carrega o
 * estado ("Prazo: 12/03, vence hoje"), satisfazendo o WCAG 1.4.1.
 *
 * @param dueDateMillis prazo em epoch-millis; nulo não é aceito — quem
 *  não tem prazo simplesmente não compõe o chip.
 * @param nowMillis relógio injetável (testes/previews); o default é
 *  `System.currentTimeMillis()`.
 */
@Composable
fun NeoDeadlineChip(
    dueDateMillis: Long,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val proximity = deadlineProximity(dueDateMillis, nowMillis)
    val pattern = stringResource(R.string.neo_deadline_date_format)
    val dateText =
        remember(dueDateMillis, pattern) {
            SimpleDateFormat(pattern, Locale.getDefault()).format(Date(dueDateMillis))
        }
    val containerColor =
        when (proximity) {
            NeoDeadlineProximity.OVERDUE -> MaterialTheme.colorScheme.errorContainer
            NeoDeadlineProximity.TODAY -> LocalNeoColors.current.warningContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    val contentColor =
        when (proximity) {
            NeoDeadlineProximity.OVERDUE -> MaterialTheme.colorScheme.onErrorContainer
            NeoDeadlineProximity.TODAY -> LocalNeoColors.current.warningText
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    val description =
        when (proximity) {
            NeoDeadlineProximity.OVERDUE ->
                stringResource(R.string.neo_deadline_description_overdue, dateText)
            NeoDeadlineProximity.TODAY ->
                stringResource(R.string.neo_deadline_description_today, dateText)
            NeoDeadlineProximity.THIS_WEEK ->
                stringResource(R.string.neo_deadline_description_this_week, dateText)
            NeoDeadlineProximity.LATER -> stringResource(R.string.neo_deadline_description, dateText)
        }

    Surface(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(NeoBorders.subtle, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NeoSpacing.xxs),
            modifier = Modifier.padding(horizontal = NeoSpacing.xs, vertical = NeoSpacing.xxs),
        ) {
            Icon(
                painter = painterResource(R.drawable.neo_ic_assignment),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = dateText,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NeoDeadlineChipPreview() {
    // "Agora" fixo para o preview mostrar os quatro estados de uma vez.
    val now = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 22, 10, 0) }.timeInMillis
    BrainOutTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(NeoSpacing.xs)) {
            NeoDeadlineChip(dueDateMillis = now - 86_400_000L, nowMillis = now)
            NeoDeadlineChip(dueDateMillis = now, nowMillis = now)
            NeoDeadlineChip(dueDateMillis = now + 3 * 86_400_000L, nowMillis = now)
            NeoDeadlineChip(dueDateMillis = now + 30 * 86_400_000L, nowMillis = now)
        }
    }
}
