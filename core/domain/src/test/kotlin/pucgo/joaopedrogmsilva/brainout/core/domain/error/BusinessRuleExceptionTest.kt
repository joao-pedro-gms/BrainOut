// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.error

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BusinessRuleExceptionTest {

    @Test
    fun `mensagem preserva regra violada`() {
        val exception = ProjectTaskLimitReachedException(projectId = "p-1", limit = 50)
        assertThat(exception.message).contains("p-1")
        assertThat(exception.message).contains("50")
        assertThat(exception).isInstanceOf(BusinessRuleException::class.java)
    }
}
