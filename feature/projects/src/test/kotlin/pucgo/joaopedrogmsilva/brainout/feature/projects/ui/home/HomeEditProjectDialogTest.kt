// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "pt-rBR")
class HomeEditProjectDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val tags =
        listOf(
            TagChip(id = "t1", name = "Urgente", color = "#FF0000"),
            TagChip(id = "t2", name = "Estagio", color = "#00FF00"),
        )
    private val project =
        Project.create(
            name = "Projeto original",
            description = "Descrição original",
            ownerId = "u1",
        )

    private fun showDialog(
        onUpdateProject: (Project, List<String>) -> Unit = { _, _ -> },
        errorMessage: String? = null,
    ) {
        composeRule.setContent {
            BrainOutTheme {
                Surface {
                    EditProjectDialog(
                        item = ProjectCardItem(project = project, tags = listOf(tags[0])),
                        availableTags = tags,
                        onDismiss = {},
                        onCreateTag = { _, _ -> },
                        onUpdateProject = onUpdateProject,
                        errorMessage = errorMessage,
                    )
                }
            }
        }
    }

    @Test
    fun `edicao preenche campos e tags atuais`() {
        showDialog()

        composeRule.onNodeWithText("Editar projeto").assertIsDisplayed()
        composeRule
            .onNodeWithTag(HomeTestTags.EDIT_PROJECT_NAME)
            .assertTextEquals("Nome", "Projeto original")
        composeRule
            .onNodeWithTag(HomeTestTags.EDIT_PROJECT_DESCRIPTION)
            .assertTextEquals("Descrição (opcional)", "Descrição original")
        composeRule.onNodeWithTag("${HomeTestTags.EDIT_PROJECT_TAG}_t1").assertIsSelected()
        composeRule.onNodeWithTag("${HomeTestTags.EDIT_PROJECT_TAG}_t2").assertIsNotSelected()
    }

    @Test
    fun `salvar envia campos aparados e tags alteradas sem trocar a identidade`() {
        var updated: Project? = null
        var submittedTagIds: List<String>? = null
        showDialog(onUpdateProject = { edited, tagIds ->
            updated = edited
            submittedTagIds = tagIds
        })

        composeRule.onNodeWithTag(HomeTestTags.EDIT_PROJECT_NAME).performTextReplacement("  Projeto editado  ")
        composeRule
            .onNodeWithTag(HomeTestTags.EDIT_PROJECT_DESCRIPTION)
            .performTextReplacement("  Nova descrição  ")
        composeRule.onNodeWithTag("${HomeTestTags.EDIT_PROJECT_TAG}_t1").performClick()
        composeRule.onNodeWithTag("${HomeTestTags.EDIT_PROJECT_TAG}_t2").performClick()
        composeRule.onNodeWithTag(HomeTestTags.EDIT_PROJECT_CONFIRM).performClick()

        assertThat(updated?.id).isEqualTo(project.id)
        assertThat(updated?.ownerId).isEqualTo(project.ownerId)
        assertThat(updated?.name).isEqualTo("Projeto editado")
        assertThat(updated?.description).isEqualTo("Nova descrição")
        assertThat(submittedTagIds).containsExactly("t2")
    }

    @Test
    fun `nome em branco mostra validacao e nao envia a edicao`() {
        var submitted = false
        showDialog(onUpdateProject = { _, _ -> submitted = true })

        composeRule.onNodeWithTag(HomeTestTags.EDIT_PROJECT_NAME).performTextReplacement("   ")
        composeRule.onNodeWithTag(HomeTestTags.EDIT_PROJECT_CONFIRM).performClick()

        composeRule.onNodeWithText("Informe um nome para o projeto.").assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.EDIT_PROJECT_DIALOG).assertIsDisplayed()
        assertThat(submitted).isFalse()
    }

    @Test
    fun `erro da atualizacao aparece no dialogo de edicao`() {
        showDialog(errorMessage = "Não foi possível concluir a operação")

        composeRule
            .onNodeWithTag(HomeTestTags.EDIT_PROJECT_ERROR)
            .assertTextEquals("Não foi possível concluir a operação")
        composeRule.onNodeWithTag(HomeTestTags.EDIT_PROJECT_DIALOG).assertIsDisplayed()
    }
}
