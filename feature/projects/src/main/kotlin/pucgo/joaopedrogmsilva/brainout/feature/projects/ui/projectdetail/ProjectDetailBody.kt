// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Coluna do corpo da ProjectDetail (E2.2/E2.8): cabeçalho (id,
// descrição), banner offline, divisória, título da seção de tarefas e
// a área da lista com as branches loading/empty/error/list (LazyColumn).
// Rotas os callbacks do ViewModel recebidos do orquestrador.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

@Suppress("LongParameterList") // 12 params: roteia estado + callbacks do orquestrador.
@Composable
internal fun ProjectDetailBody(
    projectId: String,
    tasks: List<Task>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    contentPadding: PaddingValues,
    onChangeStatus: (taskId: String, target: TaskStatus) -> Unit,
    onDeleteTask: (Task) -> Unit,
    @Suppress("unused") onRenameTask: (Task, String) -> Unit,
    onChangePriority: (Task, TaskPriority) -> Unit,
    syncState: ProjectDetailSyncState,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(id = R.string.project_detail_id_label, projectId),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(id = R.string.project_detail_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ProjectDetailOfflineBanner(
            syncState = syncState,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        Text(
            text = stringResource(id = R.string.project_detail_tasks_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        if (errorMessage != null && ProjectDetailViewModel.isLoadErrorMessage(errorMessage)) {
            ProjectDetailErrorBanner(
                message = errorMessage,
                onRetry = onRetry,
                onDismiss = onDismissError,
            )
        } else if (tasks.isEmpty() && isLoading) {
            ProjectDetailLoadingState()
        } else if (tasks.isEmpty()) {
            EmptyTasksCard()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .testTag(ProjectDetailTestTags.TASK_LIST),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                items(
                    items = tasks,
                    key = { it.id },
                ) { task ->
                    TaskRow(
                        task = task,
                        onChangeStatus = onChangeStatus,
                        onDeleteTask = onDeleteTask,
                        onChangePriority = onChangePriority,
                    )
                }
            }
        }
    }
}
