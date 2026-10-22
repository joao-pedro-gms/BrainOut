// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.notifications

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import pucgo.joaopedrogmsilva.brainout.core.data.session.SessionStore

/**
 * Testes da decisão de pedir a permissão `POST_NOTIFICATIONS` (E3.6).
 *
 * O pedido acontece **uma** vez: já concedida não pede, e negativa
 * anterior não faz o app insistir.
 */
class NotificationPermissionRequestTest {
    private val sessionStore: SessionStore = mockk()
    private val permissionStore: NotificationPermissionStore = NotificationPermissionStore(sessionStore)

    @Test
    fun `pede quando a permissao nunca foi concedida nem pedida`() =
        runTest {
            coEvery { sessionStore.wasNotificationPermissionAsked() } returns false

            val shouldAsk = shouldRequestNotificationPermission(permissionStore, isPermissionGranted = false)

            assertThat(shouldAsk).isTrue()
        }

    @Test
    fun `nao pede quando a permissao ja foi concedida`() =
        runTest {
            coEvery { sessionStore.wasNotificationPermissionAsked() } returns false

            val shouldAsk = shouldRequestNotificationPermission(permissionStore, isPermissionGranted = true)

            assertThat(shouldAsk).isFalse()
            coVerify(exactly = 0) { sessionStore.wasNotificationPermissionAsked() }
        }

    @Test
    fun `nao insiste quando a permissao ja foi negada antes`() =
        runTest {
            coEvery { sessionStore.wasNotificationPermissionAsked() } returns true

            val shouldAsk = shouldRequestNotificationPermission(permissionStore, isPermissionGranted = false)

            assertThat(shouldAsk).isFalse()
        }
}
