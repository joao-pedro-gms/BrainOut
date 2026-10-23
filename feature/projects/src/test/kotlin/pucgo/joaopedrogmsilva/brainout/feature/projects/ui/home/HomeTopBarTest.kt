// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Testes da TopAppBar da Home (E1.7).
//
// O ponto é o estado "sem sessão": enquanto o app não sabe quem é o
// usuário, a barra não pode inventar uma identidade — o esqueleto
// mostrava "João Pedro" para qualquer pessoa que abrisse o app
// (DEF-72).

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "pt-rBR")
class HomeTopBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `sem sessao a barra mostra a saudacao generica e nenhum badge de papel`() {
        composeRule.setContent {
            BrainOutTheme {
                Surface {
                    HomeTopBar(user = null)
                }
            }
        }

        composeRule.onNodeWithText("Olá!").assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.ROLE_BADGE).assertDoesNotExist()
    }

    @Test
    fun `com sessao a barra sauda pelo nome e mostra o badge de papel`() {
        composeRule.setContent {
            BrainOutTheme {
                Surface {
                    HomeTopBar(
                        user =
                            HomeUser(
                                displayName = "Maria Silva",
                                initials = "MS",
                                role = HomeUserRole.Owner,
                            ),
                    )
                }
            }
        }

        composeRule.onNodeWithText("Olá, Maria Silva").assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.ROLE_BADGE).assertIsDisplayed()
    }
}
