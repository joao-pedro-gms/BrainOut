// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Testes da superfície de gerenciamento de tags (`TagManageDialog`).
//
// O contrato novo é o caminho sem digitação: as tags existentes já
// aparecem como chips selecionáveis (testTag `"${prefix}_tag_<id>"`,
// o mesmo dos testes de criar/editar projeto) e criar tag nova exige
// apenas nome + toque numa amostra — o campo de hex livre foi removido
// e o último teste prova que nenhum campo editável de cor existe na
// árvore.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NEO_TAG_COLORS
import pucgo.joaopedrogmsilva.brainout.core.ui.components.neoColorSwatchTag
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "pt-rBR")
class HomeTagManageDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val prefix = HomeTestTags.CREATE_PROJECT_FORM_PREFIX

    private val tags =
        listOf(
            TagChip(id = "t1", name = "Urgente", color = "#FF0000"),
            TagChip(id = "t2", name = "Estagio", color = "#00FF00"),
        )

    private fun showDialog(
        selectedTagIds: List<String> = emptyList(),
        onTagToggle: (String) -> Unit = {},
        onCreate: (String, String) -> Unit = { _, _ -> },
    ) {
        composeRule.setContent {
            BrainOutTheme {
                Surface {
                    TagManageDialog(
                        availableTags = tags,
                        selectedTagIds = selectedTagIds,
                        onTagToggle = onTagToggle,
                        onDismiss = {},
                        onCreate = onCreate,
                        testTagPrefix = prefix,
                    )
                }
            }
        }
    }

    @Test
    fun `tags existentes aparecem selecionaveis sem digitar`() {
        showDialog()

        val first = composeRule.onNodeWithTag("${HomeTestTags.CREATE_PROJECT_TAG}_t1")
        val second = composeRule.onNodeWithTag("${HomeTestTags.CREATE_PROJECT_TAG}_t2")
        first.assertIsDisplayed().assertIsNotSelected()
        second.assertIsDisplayed().assertIsNotSelected()
    }

    @Test
    fun `tocar na tag alterna a selecao`() {
        composeRule.setContent {
            BrainOutTheme {
                Surface {
                    var selected by remember { mutableStateOf(listOf<String>()) }
                    TagManageDialog(
                        availableTags = tags,
                        selectedTagIds = selected,
                        onTagToggle = { id ->
                            selected =
                                if (selected.contains(id)) {
                                    selected - id
                                } else {
                                    selected + id
                                }
                        },
                        onDismiss = {},
                        onCreate = { _, _ -> },
                        testTagPrefix = prefix,
                    )
                }
            }
        }

        val chip = composeRule.onNodeWithTag("${HomeTestTags.CREATE_PROJECT_TAG}_t1")
        chip.performClick()
        chip.assertIsSelected()
        composeRule.onNodeWithTag("${HomeTestTags.CREATE_PROJECT_TAG}_t2").assertIsNotSelected()
        chip.performClick()
        chip.assertIsNotSelected()
    }

    @Test
    fun `nova tag envia nome e hex exato da amostra escolhida`() {
        var createdName: String? = null
        var createdColor: String? = null
        showDialog(onCreate = { name, color ->
            createdName = name
            createdColor = color
        })
        val target = NEO_TAG_COLORS[3]

        composeRule
            .onNodeWithTag(neoColorSwatchTag(target))
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag(neoColorSwatchTag(target)).assertIsSelected()
        composeRule.onNodeWithTag("${prefix}_tag_manage_name").performTextInput("Sprint")
        composeRule.onNodeWithTag("${prefix}_tag_manage_confirm").performClick()

        assertThat(createdName).isEqualTo("Sprint")
        assertThat(createdColor).isEqualTo(target)
    }

    @Test
    fun `nao existe campo de hex livre na arvore`() {
        showDialog()

        // O único campo de texto editável é o nome da nova tag; a cor
        // sai das amostras, então nenhum OutlinedTextField de hex pode
        // existir na árvore de semântica.
        composeRule.onAllNodes(hasSetTextAction()).assertCountEquals(1)
    }
}
