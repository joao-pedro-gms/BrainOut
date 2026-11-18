// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSpacing
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoWindowProvider
import java.time.Instant
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w1280dp-h800dp")
class HomeResponsiveLayoutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun renderProjects() {
        val projects =
            (1..4).map { index ->
                ProjectCardItem(
                    project =
                        Project.create(
                            name = "Projeto $index",
                            ownerId = "u1",
                            now = Instant.parse("2026-11-18T00:00:00Z"),
                        ),
                    tags = emptyList(),
                )
            }
        composeRule.setContent {
            NeoWindowProvider {
                BrainOutTheme {
                    HomeProjectsContent(
                        contentPadding = PaddingValues(NeoSpacing.none),
                        state =
                            HomeProjectsState(
                                projects = projects,
                                isLoading = false,
                                currentFilter = HomeProjectFilter.Active,
                                errorMessage = null,
                                availableTags = emptyList(),
                                searchQuery = "",
                                selectedTagId = null,
                                sortOrder = SortOrder.CreatedDesc,
                            ),
                        actions =
                            HomeProjectsActions(
                                onOpenProject = {},
                                onSelectFilter = {},
                                onRetry = {},
                                onDismissError = {},
                                onSearchQueryChange = {},
                                onTagFilterChange = {},
                                onSortOrderChange = {},
                            ),
                    )
                }
            }
        }
    }

    private fun firstRowCount(): Int {
        val cards =
            composeRule
                .onAllNodesWithTag(HomeTestTags.PROJECT_CARD)
                .fetchSemanticsNodes()
                .map { it.boundsInRoot }
        assertThat(cards).hasSize(4)
        val firstRowTop = cards.minOf { it.top }
        return cards.count { abs(it.top - firstRowTop) < 1f }
    }

    @Test
    fun `tablet exibe varios projetos na mesma linha`() {
        renderProjects()

        assertThat(firstRowCount()).isGreaterThan(1)
    }

    @Test
    @Config(sdk = [34], qualifiers = "w800dp-h800dp")
    fun `largura media limita cada linha a duas colunas adaptativas`() {
        renderProjects()

        assertThat(firstRowCount()).isEqualTo(2)
    }
}
