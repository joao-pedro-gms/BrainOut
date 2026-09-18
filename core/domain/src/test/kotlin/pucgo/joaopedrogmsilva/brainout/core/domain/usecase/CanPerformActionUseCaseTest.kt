// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Permission
import pucgo.joaopedrogmsilva.brainout.core.domain.model.UserRole

/**
 * Matriz Owner x Member x Permission coberta via [CanPerformActionUseCase].
 *
 * Garante que o use case respeita exatamente a matriz declarada em
 * [UserRole.permissions].
 */
class CanPerformActionUseCaseTest {

    private val useCase = CanPerformActionUseCase()

    @Test
    fun `owner can perform every action`() {
        for (permission in Permission.entries) {
            assertThat(useCase(UserRole.OWNER, permission))
                .isTrue()
        }
    }

    @Test
    fun `member can create and edit tasks but nothing else`() {
        assertThat(useCase(UserRole.MEMBER, Permission.CREATE_TASK)).isTrue()
        assertThat(useCase(UserRole.MEMBER, Permission.EDIT_TASK)).isTrue()
        assertThat(useCase(UserRole.MEMBER, Permission.DELETE_TASK)).isFalse()
        assertThat(useCase(UserRole.MEMBER, Permission.CREATE_PROJECT)).isFalse()
        assertThat(useCase(UserRole.MEMBER, Permission.EDIT_PROJECT)).isFalse()
        assertThat(useCase(UserRole.MEMBER, Permission.DELETE_PROJECT)).isFalse()
        assertThat(useCase(UserRole.MEMBER, Permission.INVITE_MEMBER)).isFalse()
    }

    @Test
    fun `matrix is consistent with UserRole`() {
        for (role in UserRole.entries) {
            for (permission in Permission.entries) {
                assertThat(useCase(role, permission))
                    .isEqualTo(role.hasPermission(permission))
            }
        }
    }
}
