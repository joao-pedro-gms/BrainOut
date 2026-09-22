// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.repository

/**
 * Contagem de tarefas em um nível de prioridade (E2.7).
 *
 * @property priorityCode código da prioridade (0..4, ver
 *  `TaskPriority.priorityCode`).
 * @property count quantidade de tarefas do owner naquele nível.
 */
data class TaskPriorityCount(
    val priorityCode: Int,
    val count: Int,
)

/**
 * Estatísticas agregadas de conclusão de tarefas de um owner (E2.7).
 *
 * @property totalCount total de tarefas (ativas + concluídas).
 * @property doneCount total de tarefas concluídas (`status = DONE`).
 * @property doneThisWeekCount tarefas concluídas na semana corrente
 *  (definida pelo caller via `weekStartMillis`).
 */
data class TaskCompletionStats(
    val totalCount: Int,
    val doneCount: Int,
    val doneThisWeekCount: Int,
) {
    /**
     * Taxa global de conclusão (0..100). Retorna 0 quando não há
     * tarefas, evitando divisão por zero.
     */
    val overallCompletionPercent: Int
        get() = if (totalCount == 0) 0 else doneCount * PERCENT_BASE / totalCount

    /**
     * Taxa de conclusão da semana corrente (0..100): concluídas na
     * semana / total. Retorna 0 quando não há tarefas.
     */
    val weeklyCompletionPercent: Int
        get() = if (totalCount == 0) 0 else doneThisWeekCount * PERCENT_BASE / totalCount

    private companion object {
        const val PERCENT_BASE: Int = 100
    }
}
