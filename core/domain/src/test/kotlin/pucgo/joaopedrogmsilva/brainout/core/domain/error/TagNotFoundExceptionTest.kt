// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.error

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TagNotFoundExceptionTest {

    @Test
    fun `mensagem identifica tag ausente`() {
        val exception = TagNotFoundException("tag-1")
        assertThat(exception).isInstanceOf(RuntimeException::class.java)
        assertThat(exception.message).isEqualTo("Tag não encontrada: tag-1")
    }
}
