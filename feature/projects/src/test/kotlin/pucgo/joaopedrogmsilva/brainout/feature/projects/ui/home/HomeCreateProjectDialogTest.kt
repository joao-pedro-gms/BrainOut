// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Testes do diálogo de criação de projeto (`:feature:projects`).
//
// A seleção de tags é o ponto que importa: as funções internas
// recebem `List<String>` + callback (o tipo de snapshot do Compose não
// vaza para a assinatura), e a leitura que registra a dependência do
// snapshot acontece no dono do estado. Se essa leitura sair de lá, o
// chip marcado não recompõe — e o teste abaixo é o que prova que
// continua funcionando.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "pt-rBR")
class HomeCreateProjectDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val tags =
        listOf(
            TagChip(id = "t1", name = "Urgente", color = "#FF0000"),
            TagChip(id = "t2", name = "Estagio", color = "#00FF00"),
        )

    private fun showDialog(onCreateProject: (String, String?, List<String>) -> Unit) {
        composeRule.setContent {
            BrainOutTheme {
                Surface {
                    CreateProjectDialog(
                        availableTags = tags,
                        onDismiss = {},
                        onCreateTag = { _, _ -> },
                        onCreateProject = onCreateProject,
                    )
                }
            }
        }
    }

    @Test
    fun `marcar uma tag leva o id na criacao do projeto`() {
        var tagIds: List<String>? = null
        showDialog { _, _, ids -> tagIds = ids }

        composeRule.onNodeWithTag(HomeTestTags.CREATE_PROJECT_NAME).performTextInput("Projeto")
        composeRule.onNodeWithTag("${HomeTestTags.CREATE_PROJECT_TAG}_t1").performClick()
        composeRule.onNodeWithTag(HomeTestTags.CREATE_PROJECT_CONFIRM).performClick()

        assertThat(tagIds).containsExactly("t1")
    }

    @Test
    fun `o chip marcado aparece selecionado`() {
        showDialog { _, _, _ -> }

        composeRule.onNodeWithTag("${HomeTestTags.CREATE_PROJECT_TAG}_t1").performClick()

        composeRule.onNodeWithTag("${HomeTestTags.CREATE_PROJECT_TAG}_t1").assertIsSelected()
        composeRule.onNodeWithTag("${HomeTestTags.CREATE_PROJECT_TAG}_t2").assertIsNotSelected()
    }

    @Test
    fun `desmarcar a tag tira o id da criacao`() {
        var tagIds: List<String>? = null
        showDialog { _, _, ids -> tagIds = ids }

        composeRule.onNodeWithTag(HomeTestTags.CREATE_PROJECT_NAME).performTextInput("Projeto")
        composeRule.onNodeWithTag("${HomeTestTags.CREATE_PROJECT_TAG}_t1").performClick()
        composeRule.onNodeWithTag("${HomeTestTags.CREATE_PROJECT_TAG}_t1").performClick()
        composeRule.onNodeWithTag(HomeTestTags.CREATE_PROJECT_CONFIRM).performClick()

        assertThat(tagIds).isEmpty()
    }
}
