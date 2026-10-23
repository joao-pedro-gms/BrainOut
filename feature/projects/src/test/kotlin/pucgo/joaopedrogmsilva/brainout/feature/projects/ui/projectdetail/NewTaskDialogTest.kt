// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.DeadlineInfo
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "pt-rBR")
class NewTaskDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `prioridade tem seletor nomeado e falha preserva formulario`() {
        var selected: TaskPriority? = null
        compose.setContent {
            BrainOutTheme {
                NewTaskDialog(
                    onDismiss = {},
                    onConfirm = { _, priority, _ -> selected = priority },
                    evaluateDeadline = { DeadlineInfo(true, null) },
                    errorMessage = "Falha local",
                )
            }
        }
        compose.onNodeWithTag(ProjectDetailTestTags.NEW_TASK_TITLE_FIELD).performTextInput("Preservar título")
        compose.onNodeWithTag("project_detail_new_task_priority_field").assertHasClickAction().performClick()
        compose.onNodeWithText("Crítica").performClick()
        compose.onNodeWithTag(ProjectDetailTestTags.NEW_TASK_SAVE).performClick()
        compose.runOnIdle { assertThat(selected).isEqualTo(TaskPriority.CRITICAL) }
        compose.onNodeWithText("Preservar título").assertExists()
        compose.onNodeWithText("Falha local").assertExists()
    }
}
