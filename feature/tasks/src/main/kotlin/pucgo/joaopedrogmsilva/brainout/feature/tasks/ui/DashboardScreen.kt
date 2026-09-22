// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Tela "Painel" (DashboardScreen) — visão consolidada (E2.7 do ROADMAP):
// - contagem de projetos por estado (ativos/concluídos);
// - gráfico de barras de tarefas por prioridade (0..4) desenhado em
//   Compose Canvas custom (sem dependência nova — decisão do escopo);
// - taxa de conclusão semanal (e global) em cartões de percentual.
//
// Todos os dados vêm de Flows reativos do Room via DashboardViewModel:
// quando o banco muda, os gráficos atualizam sem recarregar.
//
// Acessibilidade (padrão E4.4): áreas de toque ≥ 48dp, contraste AA
// via tokens do tema (core:ui/theme) e contentDescription nos
// elementos gráficos para TalkBack.

@file:Suppress("TooManyFunctions", "MagicNumber")

package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pucgo.joaopedrogmsilva.brainout.feature.tasks.R

/**
 * Tela "Painel" do BrainOut (E2.7).
 *
 * @param viewModel injetado pelo Hilt; substituível por fake em
 *  previews/testes.
 */
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DashboardContent(
        state = state,
        onRetry = viewModel::retry,
        onDismissError = viewModel::clearError,
        modifier = modifier,
    )
}

/** Identificadores para testes de UI Compose. */
object DashboardTestTags {
    const val LOADING: String = "dashboard_loading"
    const val EMPTY: String = "dashboard_empty"
    const val ERROR_BANNER: String = "dashboard_error_banner"
    const val ERROR_RETRY: String = "dashboard_error_retry"
    const val ERROR_DISMISS: String = "dashboard_error_dismiss"
    const val PROJECT_STATE_CARD: String = "dashboard_project_state_card"
    const val PROJECT_ACTIVE_COUNT: String = "dashboard_project_active_count"
    const val PROJECT_COMPLETED_COUNT: String = "dashboard_project_completed_count"
    const val PRIORITY_CHART: String = "dashboard_priority_chart"
    const val WEEKLY_RATE_CARD: String = "dashboard_weekly_rate_card"
    const val OVERALL_RATE_CARD: String = "dashboard_overall_rate_card"

    /** Test tag da barra de prioridade nível [code] (0..4). */
    fun priorityBar(code: Int): String = "dashboard_priority_bar_$code"
}

@Composable
internal fun DashboardContent(
    state: DashboardUiState,
    onRetry: () -> Unit = {},
    onDismissError: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(id = R.string.dashboard_screen_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // E2.8 — prioridade ao banner de erro, depois loader, depois
        // conteúdo. Mesma ordem de TasksScreen/HomeScreen.
        when {
            state.errorMessage != null -> DashboardErrorBanner(
                onRetry = onRetry,
                onDismiss = onDismissError,
            )
            state.isLoading -> DashboardLoading()
            else -> DashboardBody(state = state)
        }
    }
}

/**
 * Banner de erro com retry (E2.8). Mesma estrutura visual dos banners
 * de Home/Tasks.
 */
@Composable
private fun DashboardErrorBanner(
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .testTag(DashboardTestTags.ERROR_BANNER),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.dashboard_error_load_failed),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .testTag(DashboardTestTags.ERROR_RETRY)
                        .heightIn(min = 48.dp),
                ) {
                    Text(text = stringResource(id = R.string.dashboard_error_retry))
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .testTag(DashboardTestTags.ERROR_DISMISS)
                        .heightIn(min = 48.dp),
                ) {
                    Text(text = stringResource(id = R.string.dashboard_error_dismiss))
                }
            }
        }
    }
}

@Composable
private fun DashboardLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .testTag(DashboardTestTags.LOADING),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.height(48.dp))
            Text(
                text = stringResource(id = R.string.dashboard_loading_aria_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DashboardBody(state: DashboardUiState) {
    val hasAnyData = state.activeProjects + state.completedProjects > 0 || state.totalTasks > 0
    if (!hasAnyData) {
        DashboardEmptyState(modifier = Modifier.fillMaxWidth())
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProjectStateCard(
            activeProjects = state.activeProjects,
            completedProjects = state.completedProjects,
        )
        PriorityChart(
            counts = state.priorityCounts,
        )
        CompletionRateCards(
            weeklyPercent = state.weeklyCompletionPercent,
            overallPercent = state.overallCompletionPercent,
        )
    }
}

/**
 * Cartão de contagem de projetos por estado (ativos/concluídos).
 */
@Composable
private fun ProjectStateCard(
    activeProjects: Int,
    completedProjects: Int,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DashboardTestTags.PROJECT_STATE_CARD),
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(id = R.string.dashboard_projects_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StateCounter(
                    label = stringResource(id = R.string.dashboard_projects_active),
                    count = activeProjects,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(DashboardTestTags.PROJECT_ACTIVE_COUNT),
                )
                StateCounter(
                    label = stringResource(id = R.string.dashboard_projects_completed),
                    count = completedProjects,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(DashboardTestTags.PROJECT_COMPLETED_COUNT),
                )
            }
        }
    }
}

@Composable
private fun StateCounter(
    label: String,
    count: Int,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            // E4.4: área de toque/percepção mínima de 48dp.
            .heightIn(min = 72.dp),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = contentColor,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Gráfico de barras de tarefas por prioridade (E2.7), desenhado em
 * Compose Canvas puro — sem biblioteca de gráficos nova.
 *
 * Cinco barras (níveis 0..4, LOW → CRITICAL). A descrição para
 * TalkBack é construída a partir dos rótulos localizados + contagem,
 * e o valor numérico de cada barra fica legível abaixo dela.
 */
@Composable
private fun PriorityChart(
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
        val barWidth = slotWidth * 0.55f
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
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
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

/**
 * Cartões de taxa de conclusão: semanal (concluídas na semana /
 * total) e global.
 */
@Composable
private fun CompletionRateCards(
    weeklyPercent: Int,
    overallPercent: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RateCard(
            title = stringResource(id = R.string.dashboard_weekly_rate_title),
            percent = weeklyPercent,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            testTag = DashboardTestTags.WEEKLY_RATE_CARD,
            modifier = Modifier.weight(1f),
        )
        RateCard(
            title = stringResource(id = R.string.dashboard_overall_rate_title),
            percent = overallPercent,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            testTag = DashboardTestTags.OVERALL_RATE_CARD,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RateCard(
    title: String,
    percent: Int,
    containerColor: Color,
    contentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .testTag(testTag)
            // E4.4: mínimo 48dp.
            .heightIn(min = 96.dp),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(id = R.string.dashboard_rate_percent, percent),
                style = MaterialTheme.typography.headlineSmall,
                color = contentColor,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DashboardEmptyState(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .height(280.dp)
            .testTag(DashboardTestTags.EMPTY),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.dashboard_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.dashboard_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// --- Previews ---------------------------------------------------------

@Preview(showBackground = true)
@Composable
private fun DashboardScreenEmptyPreview() {
    androidx.compose.material3.MaterialTheme {
        DashboardContent(state = DashboardUiState(isLoading = false))
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPopulatedPreview() {
    androidx.compose.material3.MaterialTheme {
        DashboardContent(
            state = DashboardUiState(
                activeProjects = 3,
                completedProjects = 1,
                priorityCounts = listOf(2, 4, 3, 1, 0),
                totalTasks = 10,
                doneTasks = 4,
                weeklyCompletionPercent = 40,
                overallCompletionPercent = 40,
                isLoading = false,
            ),
        )
    }
}
