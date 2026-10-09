// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Tela "Painel" (DashboardScreen) — visão consolidada (E2.7 do ROADMAP):
// - contagem de projetos por estado (ativos/concluídos);
// - gráfico de barras de tarefas por prioridade (0..4) desenhado em
//   Compose Canvas custom (sem dependência nova — decisão do escopo);
// - taxa de conclusão semanal (e global) em cartões de percentual.
//
// Orquestrador puro: roteia estado (erro/loading/body) para os composables
// extraídos em `ui/DashboardErrorBanner.kt`, `ui/DashboardLoading.kt`,
// `ui/DashboardEmptyState.kt`, `ui/DashboardProjectStateCard.kt`,
// `ui/DashboardPriorityChart.kt` e `ui/DashboardCompletionRateCards.kt`.
//
// Todos os dados vêm de Flows reativos do Room via DashboardViewModel:
// quando o banco muda, os gráficos atualizam sem recarregar.
//
// Acessibilidade (padrão E4.4): áreas de toque ≥ 48dp, contraste AA
// via tokens do tema (core:ui/theme) e contentDescription nos
// elementos gráficos para TalkBack.

package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pucgo.joaopedrogmsilva.brainout.feature.tasks.R

/**
 * Tela "Painel" do BrainOut (E2.7).
 *
 * DEF-22 — passou a usar `Scaffold` + [TasksTopBar], como a [TasksScreen]:
 * antes não havia como voltar, e o título ficava sob a barra de status
 * porque `MainActivity` liga `enableEdgeToEdge()`.
 *
 * @param onBackClicked chamado pelo botão de voltar da [TasksTopBar].
 * @param viewModel injetado pelo Hilt; substituível por fake em
 *  previews/testes.
 */
@Composable
fun DashboardScreen(
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TasksTopBar(
                titleRes = R.string.dashboard_screen_title,
                onBackClicked = onBackClicked,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        DashboardContent(
            state = state,
            onRetry = viewModel::retry,
            onDismissError = viewModel::clearError,
            modifier = Modifier.padding(innerPadding),
        )
    }
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
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // DEF-22 — o título saiu daqui e foi para a `TasksTopBar`.
        // Repetir o mesmo texto no corpo faria o TalkBack anunciar
        // "Painel" duas vezes e a tela mostrar o título duas vezes.
        // É o que LoginScreen e SettingsScreen já faziam: o título
        // pertence à barra, o corpo traz o conteúdo.
        //
        // Divisor com o token cheio: `outline` puro dá 16.10:1 (claro)
        // e 15.95:1 (escuro); com `alpha = 0.3f` caía para 1.93:1 /
        // 2.59:1, abaixo do mínimo de 3:1 do WCAG §1.4.11.
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // E2.8 — prioridade ao banner de erro, depois loader, depois
        // conteúdo. Mesma ordem de TasksScreen/HomeScreen.
        when {
            state.errorMessage != null ->
                DashboardErrorBanner(
                    onRetry = onRetry,
                    onDismiss = onDismissError,
                )
            state.isLoading -> DashboardLoading()
            else -> DashboardBody(state = state)
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
            state =
                DashboardUiState(
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
