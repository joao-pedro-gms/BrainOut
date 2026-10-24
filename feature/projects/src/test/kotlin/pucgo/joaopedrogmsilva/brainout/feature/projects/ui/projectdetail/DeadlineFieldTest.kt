// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Holiday
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.DeadlineInfo
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "pt-rBR")
class DeadlineFieldTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `falha local nao impede trocar ou limpar prazo e nova data recupera aviso`() {
        val firstDate = Instant.parse("2026-11-19T12:00:00Z")
        val dueDate = mutableStateOf<Instant?>(firstDate)
        composeRule.setContent {
            BrainOutTheme {
                DeadlineField(
                    dueDate = dueDate.value,
                    onDateChange = { dueDate.value = it },
                    evaluateDeadline = {
                        if (it == firstDate) error("Falha no cálculo local")
                        DeadlineInfo(
                            isBusinessDay = false,
                            nextHoliday =
                                Holiday(
                                    LocalDate.of(2026, 11, 20),
                                    "Consciência Negra",
                                    Holiday.TYPE_NATIONAL,
                                ),
                        )
                    },
                )
            }
        }

        val failure = "Não foi possível calcular os feriados para esta data."
        composeRule.onNodeWithText(failure).assertIsDisplayed()
        composeRule.onNodeWithTag(ProjectDetailTestTags.NEW_TASK_DUE_DATE_FIELD).assertIsDisplayed()

        composeRule.runOnIdle { dueDate.value = Instant.parse("2026-11-20T12:00:00Z") }
        composeRule.onNodeWithText(failure).assertDoesNotExist()
        composeRule.onNodeWithTag(ProjectDetailTestTags.NEW_TASK_HOLIDAY_HINT).assertIsDisplayed()
        composeRule.onNodeWithText("Atenção: este prazo cai em dia não útil.").assertIsDisplayed()

        composeRule.onNodeWithTag("deadline_clear").performClick()
        composeRule.onNodeWithTag(ProjectDetailTestTags.NEW_TASK_HOLIDAY_HINT).assertDoesNotExist()
        composeRule.onNodeWithTag("deadline_clear").assertDoesNotExist()
        composeRule.onNodeWithTag(ProjectDetailTestTags.NEW_TASK_DUE_DATE_FIELD).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en")
    fun `falha local em ingles permite limpar o prazo`() {
        val dueDate = mutableStateOf<Instant?>(Instant.parse("2026-11-20T12:00:00Z"))
        composeRule.setContent {
            BrainOutTheme {
                DeadlineField(
                    dueDate = dueDate.value,
                    onDateChange = { dueDate.value = it },
                    evaluateDeadline = { error("Local calculation failed") },
                )
            }
        }

        val failure = "Could not calculate holidays for this date."
        composeRule.onNodeWithText(failure).assertIsDisplayed()
        composeRule.onNodeWithTag("deadline_clear").performClick()
        composeRule.onNodeWithText(failure).assertDoesNotExist()
        composeRule.onNodeWithTag(ProjectDetailTestTags.NEW_TASK_DUE_DATE_FIELD).assertIsDisplayed()
    }
}
