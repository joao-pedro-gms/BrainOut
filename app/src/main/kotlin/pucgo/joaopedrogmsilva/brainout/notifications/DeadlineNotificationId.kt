// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.notifications

/**
 * Id da notificação de lembrete de uma tarefa (marco E3.6).
 *
 * Publicação, cancelamento e `PendingIntent` precisam do **mesmo** id:
 * se divergirem, a notificação publicada não é a que o "Dispensar"
 * cancela, e o toque no corpo não abre a tarefa certa.
 */
object DeadlineNotificationId {
    /**
     * Id numérico da notificação associada a [taskId].
     */
    fun of(taskId: String): Int = taskId.hashCode()
}
