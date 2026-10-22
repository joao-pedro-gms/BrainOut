// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.notifications

/**
 * Chave compartilhada do fluxo de lembrete de prazo (marco E3.6).
 *
 * O id da tarefa viaja do agendamento até o worker, o receiver e a
 * notificação por vias diferentes — `Data` de entrada do WorkManager e
 * extra do `Intent`. Quando cada uma declarava a sua própria constante,
 * trocar o formato exigia acertar dois lugares e uma divergência
 * silenciosa quebrava o lembrete.
 */
object DeadlineKeys {
    /** Chave do id da tarefa (`Data` do WorkManager e extra do `Intent`). */
    const val TASK_ID: String = "task_id"
}
