// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.notifications

import javax.inject.Inject
import javax.inject.Singleton
import pucgo.joaopedrogmsilva.brainout.core.data.session.SessionStore

/**
 * Guarda a flag de "já pedimos a permissão POST_NOTIFICATIONS" em
 * [SessionStore] (DataStore), para não insistir após uma negativa
 * (marco E3.6).
 *
 * Reutiliza o mesmo arquivo de preferências da sessão (`auth_prefs`),
 * mas com chave própria — o DataStore é compartilhado para evitar um
 * segundo arquivo de preferências por um único booleano.
 */
@Singleton
class NotificationPermissionStore @Inject constructor(
    private val sessionStore: SessionStore,
) {

    /** `true` se a permissão já foi pedida alguma vez. */
    suspend fun wasAsked(): Boolean = sessionStore.wasNotificationPermissionAsked()

    /** Registra que a permissão foi pedida (independente do resultado). */
    suspend fun markAsked() {
        sessionStore.markNotificationPermissionAsked()
    }
}
