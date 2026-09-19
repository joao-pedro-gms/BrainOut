// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class TaskPriorityTest {

    @Test
    fun `fromCodeOrThrow converte codigos validos e rejeita fora do intervalo`() {
        TaskPriority.entries.forEach { priority ->
            assertThat(TaskPriority.fromCodeOrThrow(priority.priorityCode)).isEqualTo(priority)
        }
        listOf(Int.MIN_VALUE, TaskPriority.VALID_RANGE.first - 1, TaskPriority.VALID_RANGE.last + 1, Int.MAX_VALUE)
            .forEach { code ->
                assertThrows(IllegalArgumentException::class.java) { TaskPriority.fromCodeOrThrow(code) }
            }
    }
}
