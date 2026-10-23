// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Gráfico de barras de tarefas por prioridade (E2.7) do Dashboard —
// desenho puro em Compose Canvas (sem dependência nova). Inclui o
// cartão, o canvas, a legenda numérica e o rótulo curto por nível.

package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoTaskPriorityLabel
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoMotion
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoRadii
import pucgo.joaopedrogmsilva.brainout.feature.tasks.R

private const val BAR_TO_SLOT_RATIO: Float = 0.55f

@Composable
internal fun PriorityChart(counts: List<Int>) {
    val labels = TaskPriority.entries.map { NeoTaskPriorityLabel(it) }
    val barColors =
        listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.error,
        )
    val maxCount = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    // `zip` em vez de índice: a lista de rótulos tem um item por
    // nível de prioridade e a de contagens pode ter outro tamanho —
    // indexar as duas em paralelo estourava `IndexOutOfBounds`.
    val chartDescription =
        counts
            .zip(labels) { count, label -> "$label: $count" }
            .joinToString(separator = ", ")

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(DashboardTestTags.PRIORITY_CHART),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier =
                Modifier
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
    val proportions =
        counts.map { count ->
            animateFloatAsState(
                count.toFloat() / maxCount,
                tween(NeoMotion.confirmation),
                label = "confirmedPriorityCount",
            )
        }
    // O raio vinha em pixels fixos (`8f`), o que o deixava
    // proporcionalmente menor conforme a densidade da tela. O token
    // `NeoRadii.chip` (4dp) escala com a tela e vale exatamente os
    // mesmos 8px na densidade xhdpi.
    val cornerRadiusPx = with(LocalDensity.current) { NeoRadii.chip.toPx() }
    val fallbackBarColor = MaterialTheme.colorScheme.surfaceVariant
    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .semantics { contentDescription = chartDescription },
    ) {
        // `coerceAtLeast(1)`: com a lista vazia a divisão daria
        // infinito — inofensivo hoje (o laço não roda), mas é uma
        // armadilha esperando a próxima lista menor.
        val slotCount = counts.size.coerceAtLeast(1)
        val slotWidth = size.width / slotCount
        val barWidth = slotWidth * BAR_TO_SLOT_RATIO
        proportions.forEachIndexed { index, proportion ->
            val barHeight = size.height * proportion.value
            val topLeft =
                Offset(
                    x = slotWidth * index + (slotWidth - barWidth) / 2f,
                    y = size.height - barHeight,
                )
            drawRoundRect(
                color = barColors.getOrElse(index) { fallbackBarColor },
                topLeft = topLeft,
                size = Size(width = barWidth, height = barHeight),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
            )
        }
    }
}

/** Legenda numérica sob as barras do gráfico (contagem por nível). */
@Composable
private fun PriorityBarsLegend(counts: List<Int>) {
    val labels = TaskPriority.entries.map { NeoTaskPriorityLabel(it) }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    text = labels.getOrElse(index) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
