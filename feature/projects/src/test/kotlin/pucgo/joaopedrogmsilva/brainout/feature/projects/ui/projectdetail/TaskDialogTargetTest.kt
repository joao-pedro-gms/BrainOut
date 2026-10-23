// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task

/**
 * Testes do estado de edição de tarefa (DEF-52).
 *
 * O `Task` não tem `Saver`, então o estado salvo guarda o **id** da
 * tarefa — é o que permite o diálogo de edição sobreviver a uma
 * recreação da Activity (giro do aparelho).
 *
 * Os dois diálogos (prioridade e renomear) têm estados **independentes**:
 * cada um tem o seu próprio `rememberSaveable`, e é o escopo posicional do
 * runtime do Compose que garante que eles não compartilhem o slot.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "pt-rBR")
class TaskDialogTargetTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `o id da tarefa em edicao sobrevive a uma recreacao`() {
        val restorationTester = StateRestorationTester(compose)
        val observed = mutableStateOf<String?>(null)

        restorationTester.setContent {
            val editedId = rememberRenameTaskId()
            observed.value = editedId.value
            editedId.value = "task-1"
        }

        // Simula o que o Android faz ao girar o aparelho: salva o
        // estado, destrói a composição e a reconstrói a partir do que
        // foi salvo.
        restorationTester.emulateSavedInstanceStateRestore()

        compose.runOnIdle {
            assertThat(observed.value).isEqualTo("task-1")
        }
    }

    @Test
    fun `dois dialogos de edicao nao compartilham o mesmo slot`() {
        val restorationTester = StateRestorationTester(compose)
        val observed = mutableStateOf<Pair<String?, String?>>(null to null)

        restorationTester.setContent {
            val priorityId = rememberPriorityDialogTaskId()
            val renameId = rememberRenameTaskId()
            observed.value = priorityId.value to renameId.value
            priorityId.value = "task-1"
            renameId.value = "task-2"
        }

        restorationTester.emulateSavedInstanceStateRestore()

        compose.runOnIdle {
            assertThat(observed.value).isEqualTo("task-1" to "task-2")
        }
    }

    @Test
    fun `a tarefa em edicao e resolvida pelo id guardado`() {
        val task = Task.create(projectId = "p1", title = "Tarefa")
        val other = Task.create(projectId = "p1", title = "Outra")

        assertThat(taskById(listOf(task, other), task.id)).isEqualTo(task)
        assertThat(taskById(listOf(task, other), other.id)).isEqualTo(other)
    }

    @Test
    fun `tarefa que saiu da lista nao abre o dialogo`() {
        val task = Task.create(projectId = "p1", title = "Tarefa")

        // A tarefa foi excluída (ou filtrada) enquanto o diálogo
        // estava aberto: o id continua salvo, mas não há o que editar.
        assertThat(taskById(emptyList(), task.id)).isNull()
        assertThat(taskById(listOf(task), null)).isNull()
    }
}
