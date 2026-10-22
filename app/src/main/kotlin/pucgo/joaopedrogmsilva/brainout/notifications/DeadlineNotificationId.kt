// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.notifications

/**
 * Identidade da notificação de lembrete de uma tarefa (marco E3.6).
 *
 * Publicação, cancelamento e `PendingIntent` precisam da **mesma**
 * identidade: se divergirem, a notificação publicada não é a que o
 * "Dispensar" cancela, e o toque no corpo não abre a tarefa certa.
 *
 * No Android a identidade de uma notificação é o par **(tag, id)** —
 * o id numérico sozinho não basta. E o id numérico vem de
 * `hashCode()`, que tem 32 bits e **colide**: duas tarefas diferentes
 * podem receber o mesmo id. Com o id sozinho, a notificação de uma
 * substituía a da outra na bandeja e o botão "Concluir" podia agir
 * sobre a tarefa errada (DEF-54).
 *
 * Por isso a tag é o id da tarefa inteiro, que é único, e o id
 * numérico fica apenas como a metade numérica da identidade.
 */
object DeadlineNotificationId {
    /**
     * Id numérico da notificação associada a [taskId].
     *
     * Só completa a identidade — quem separa uma notificação da outra
     * é a tag ([tagOf]). O bit de sinal é zerado para o id nunca ser
     * negativo.
     */
    fun of(taskId: String): Int = taskId.hashCode() and Int.MAX_VALUE

    /**
     * Tag da notificação associada a [taskId] — o id da tarefa, que é
     * único por tarefa.
     */
    fun tagOf(taskId: String): String = taskId
}
