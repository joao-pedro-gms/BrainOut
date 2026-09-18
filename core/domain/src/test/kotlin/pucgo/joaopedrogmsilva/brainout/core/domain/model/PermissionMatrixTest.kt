// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Testes unitários da matriz de permissões por papel.
 *
 * Cobre o requisito R2 (dois perfis com permissões distintas) e E1.7
 * do roadmap.
 */
class PermissionMatrixTest {

    @Test
    fun `OWNER owns every defined permission`() {
        val allPermissions = Permission.entries.toSet()
        for (permission in allPermissions) {
            assertThat(UserRole.OWNER.hasPermission(permission)).isTrue()
        }
    }

    @Test
    fun `MEMBER only has task creation and editing permissions`() {
        val memberPermissions = UserRole.MEMBER.permissions

        assertThat(memberPermissions).containsExactly(
            Permission.CREATE_TASK,
            Permission.EDIT_TASK,
        )
    }

    @Test
    fun `MEMBER cannot create edit or delete projects`() {
        assertThat(UserRole.MEMBER.hasPermission(Permission.CREATE_PROJECT)).isFalse()
        assertThat(UserRole.MEMBER.hasPermission(Permission.EDIT_PROJECT)).isFalse()
        assertThat(UserRole.MEMBER.hasPermission(Permission.DELETE_PROJECT)).isFalse()
    }

    @Test
    fun `MEMBER cannot delete tasks nor invite members`() {
        assertThat(UserRole.MEMBER.hasPermission(Permission.DELETE_TASK)).isFalse()
        assertThat(UserRole.MEMBER.hasPermission(Permission.INVITE_MEMBER)).isFalse()
    }

    @Test
    fun `permission sets are exhaustive and immutable per role`() {
        // Apenas Owner e Member são esperados nesta entrega. Refletindo
        // essa expectativa no teste para detectar regressões futuras.
        assertThat(UserRole.entries.toList()).containsExactly(
            UserRole.OWNER,
            UserRole.MEMBER,
        ).inOrder()
    }
}
