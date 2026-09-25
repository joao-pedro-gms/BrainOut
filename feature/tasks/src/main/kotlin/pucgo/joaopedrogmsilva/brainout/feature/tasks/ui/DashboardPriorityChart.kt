// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Gráfico de barras de tarefas por prioridade (E2.7) do Dashboard —
// desenho puro em Compose Canvas (sem dependência nova). Inclui o
// cartão, o canvas, a legenda numérica e o rótulo curto por nível.

package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.feature.tasks.R

private const val BAR_TO_SLOT_RATIO: Float = 0.55f
private const val BAR_CORNER_RADIUS_PX: Float = 8f

@Composable
internal fun PriorityChart(
    counts: List<Int>,
) {
    val labels = listOf(
        stringResource(id = R.string.task_priority_low),
        stringResource(id = R.string.task_priority_medium),
        stringResource(id = R.string.task_priority_high),
        stringResource(id = R.string.task_priority_urgent),
        stringResource(id = R.string.task_priority_critical),
    )
    val barColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.error,
    )
    val maxCount = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    val chartDescription = counts
        .mapIndexed { index, count -> "${labels[index]}: $count" }
        .joinToString(separator = ", ")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DashboardTestTags.PRIORITY_CHART),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(id = R.string.dashboard_priority_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            PriorityBarsCanvas(
                counts = counts,
                barColors = barColors,
                maxCount = maxCount,
                chartDescription = chartDescription,
            )
            PriorityBarsLegend(counts = counts)
        }
    }
}

/**
 * Canvas das barras do gráfico de prioridades. Desenho puro Compose
 * (sem biblioteca de gráficos): cada nível ocupa um slot igual e a
 * altura da barra é proporcional a `count / maxCount`.
 */
@Composable
private fun PriorityBarsCanvas(
    counts: List<Int>,
    barColors: List<Color>,
    maxCount: Int,
    chartDescription: String,
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .semantics { contentDescription = chartDescription },
    ) {
        val slotWidth = size.width / counts.size
        val barWidth = slotWidth * BAR_TO_SLOT_RATIO
        counts.forEachIndexed { index, count ->
            val barHeight = size.height * (count.toFloat() / maxCount)
            val topLeft = Offset(
                x = slotWidth * index + (slotWidth - barWidth) / 2f,
                y = size.height - barHeight,
            )
            drawRoundRect(
                color = barColors[index],
                topLeft = topLeft,
                size = Size(width = barWidth, height = barHeight),
                cornerRadius = CornerRadius(BAR_CORNER_RADIUS_PX, BAR_CORNER_RADIUS_PX),
            )
        }
    }
}

/** Legenda numérica sob as barras do gráfico (contagem por nível). */
@Composable
private fun PriorityBarsLegend(
    counts: List<Int>,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        counts.forEachIndexed { index, count ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.testTag(DashboardTestTags.priorityBar(index)),
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = priorityShortLabel(index),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Rótulo curto (1 letra) por nível de prioridade — mantém as colunas
 * do gráfico compactas em pt e en.
 */
@Composable
private fun priorityShortLabel(index: Int): String = when (index) {
    0 -> stringResource(id = R.string.dashboard_priority_short_low)
    1 -> stringResource(id = R.string.dashboard_priority_short_medium)
    2 -> stringResource(id = R.string.dashboard_priority_short_high)
    3 -> stringResource(id = R.string.dashboard_priority_short_urgent)
    else -> stringResource(id = R.string.dashboard_priority_short_critical)
}
