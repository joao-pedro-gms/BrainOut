// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.dao

/**
 * Linha agregada da consulta de contagem por prioridade
 * (`TaskDao.observeCountByPriority`, E2.7 do ROADMAP).
 *
 * O Room materializa esta classe a partir das colunas `priorityCode`
 * e `taskCount` da query `GROUP BY priority_code`. Prioridades sem
 * tarefas não aparecem no resultado — quem preenche os níveis 0..4
 * faltantes com zero é o consumidor do Flow.
 *
 * @property priorityCode código numérico da prioridade (0..4, ver
 *  `TaskPriority`).
 * @property taskCount quantidade de tarefas do owner com essa
 *  prioridade.
 */
data class PriorityCountRow(
    val priorityCode: Int,
    val taskCount: Int,
)

/**
 * Linha agregada da consulta de estatísticas de conclusão
 * (`TaskDao.observeCompletionStats`, E2.7 do ROADMAP).
 *
 * Como a query é um `SELECT` sem `GROUP BY`, o Room sempre materializa
 * exatamente uma linha (mesmo com `tasks` vazia — `COUNT(*)` retorna 0
 * e os `SUM` retornam `NULL`, convertido para `0` no default).
 *
 * @property totalCount total de tarefas do owner (ativas + concluídas).
 * @property doneCount total de tarefas com `status = 'DONE'`.
 * @property doneThisWeekCount tarefas concluídas com
 *  `completed_at >= weekStartMillis` — base da taxa semanal.
 */
data class CompletionStatsRow(
    val totalCount: Int = 0,
    val doneCount: Int = 0,
    val doneThisWeekCount: Int = 0,
)
