// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import pucgo.joaopedrogmsilva.brainout.core.domain.error.ProjectTaskLimitReachedException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TaskNotFoundException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TaskPriorityChangeForbiddenException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository
import java.time.Instant
import javax.inject.Inject

/**
 * Caso de uso responsável por alterar o [Task.status] de uma [Task]
 * **e** reconciliar o lembrete de prazo associado (marco E3.6).
 *
 * RN03 (E2.5) — conclusão cascata: ao receber `target = DONE` o caso
 * de uso delega para [TaskRepository.completeAndCascade], que aplica
 * a transição E marca o projeto como concluído na mesma transação
 * Room quando a tarefa concluída é a última ativa. Para reabrir
 * (DONE → DOING ou DOING → TODO) delega para [TaskRepository.reopenAndCascade]
 * que desfaz a marca de conclusão do projeto caso ele estivesse
 * concluído.
 *
 * Transições intermediárias (TODO → DOING, DOING → TODO) usam
 * [TaskRepository.changeStatus] sem cascata porque não alteram a
 * cardinalidade de tarefas ativas.
 *
 * RN01 (DEF-20) — reabrir **aumenta** a cardinalidade de tarefas
 * ativas, então o teto de [MAX_ACTIVE_TASKS_PER_PROJECT] é
 * revalidado antes de delegar a [TaskRepository.reopenAndCascade].
 * Concluir (`target == DONE`) diminui a cardinalidade e nunca
 * passa por esse guard. O raciocínio detalhado do limite está no
 * KDoc de [ensureReopenFitsActiveCap].
 *
 * A matriz de transições válidas é aplicada pelo domínio via
 * [Task.transitionTo]; o repositório apenas materializa a mudança.
 * Lança [pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidStateTransitionException]
 * quando a transição pedida não é permitida (ex.: DONE → TODO).
 *
 * Regra de agendamento do lembrete:
 * - Tarefa passa a **DONE** (ou permanece DONE) → lembrete cancelado.
 * - Tarefa volta a ficar ativa e possui [Task.dueDate] → lembrete
 *   (re)agendado para `dueDate - [DeadlineNotificationScheduler.REMINDER_LEAD]`.
 * - Prazo no passado ou sem prazo → sem agendamento (o scheduler
 *   descarta gatilhos no passado; aqui já não chamamos).
 */
class ChangeTaskStatusUseCase
    @Inject
    constructor(
        private val repository: TaskRepository,
        private val deadlineScheduler: DeadlineNotificationScheduler,
    ) {
        /**
         * @param taskId Identificador da tarefa.
         * @param target Estado desejado.
         * @return A [Task] atualizada.
         */
        suspend operator fun invoke(
            taskId: String,
            target: TaskStatus,
        ): Task {
            val updated =
                when (target) {
                    // RN03 (E2.5): entrada em DONE é o único caminho que pode
                    // alterar a contagem de tarefas ativas (decrementa) — passa
                    // pela cascata para também marcar o projeto se for a
                    // última ativa.
                    TaskStatus.DONE -> repository.completeAndCascade(taskId)
                    // Reabertura (saída de DONE) também muda a contagem e pode
                    // precisar desmarcar o projeto: mesma via.
                    else -> {
                        val current = repository.findById(taskId) ?: throw TaskNotFoundException(taskId)
                        if (current.status == TaskStatus.DONE) {
                            ensureReopenFitsActiveCap(current)
                            repository.reopenAndCascade(taskId, target)
                        } else {
                            repository.changeStatus(taskId, target)
                        }
                    }
                }
            reconcileReminder(updated)
            return updated
        }

        /**
         * Atualiza a tarefa inteira (usado pelos fluxos de edição) e
         * reconcilia o lembrete de acordo com o novo status/prazo.
         *
         * RN02 (E2.4): assim como [UpdateTaskUseCase], este overload
         * recarrega o registro **persistido** antes de escrever e rejeita
         * a operação com
         * [pucgo.joaopedrogmsilva.brainout.core.domain.error.TaskPriorityChangeForbiddenException]
         * quando ele estiver em [TaskStatus.DONE]. Sem essa checagem, um
         * chamador poderia burlar RN02 via `Task.copy` — o modelo não
         * valida RN02 no `init`, e `copy` não passa por
         * [Task.changePriority]. O erro é propagado para a camada de UI
         * sem qualquer suavização aqui.
         *
         * **Por que o overload foi mantido em vez de removido:** o
         * `grep` em `app`, `feature:*`, `core/data` e `core/domain`
         * (2026-10-08) mostra **zero** chamadores de produção do overload
         * `invoke(Task)` — todo caminho que muda o status passa por
         * [invoke] com `(taskId, target)` e a edição de campos passa pelo
         * `UpdateTaskUseCase`. Ainda assim ele é a porta pública de
         * atualização integral do caso de uso e, enquanto existir, precisa
         * cumprir a mesma invariante do RN02 que o [UpdateTaskUseCase];
         * remover seria uma decisão de API que cabe ao roadmap, não a um
         * patch de segurança. O guard abaixo torna o overload seguro por
         * construção, independentemente de quem o chamar.
         */
        suspend operator fun invoke(task: Task): Task {
            val persisted = repository.findById(task.id)
            if (persisted != null && persisted.status == TaskStatus.DONE) {
                throw TaskPriorityChangeForbiddenException(task.id)
            }
            val updated = repository.update(task)
            reconcileReminder(updated)
            return updated
        }

        /**
         * RN01 (DEF-20) — revalida o teto de
         * [MAX_ACTIVE_TASKS_PER_PROJECT] **antes** de reabrir.
         *
         * Até aqui a RN01 só era aplicada por [CreateTaskUseCase],
         * ou seja, era um guard de **criação**: um projeto com 50
         * ativas + 1 concluída reabria para 51 sem nenhum erro. O
         * guard mora no domínio (e não no repositório) porque
         * `reopenAndCascade` é uma operação de persistência e também
         * é alcançável por chamadores que não passam por esta regra.
         *
         * **Por que `>=` e não `>`:** a contagem vem de
         * [TaskRepository.countActiveByProject], que só conta
         * `status != DONE`, e a tarefa que está sendo reaberta está
         * justamente em DONE — logo ela **não** entra na contagem.
         * Reabrindo, o pós-estado é `active + 1`. Rejeitar quando
         * `active >= cap` é, por isso, exatamente a mesma
         * pré-condição (e o mesmo off-by-one) que [CreateTaskUseCase]
         * aplica: com `active == 50` a reabertura resultaria em 51;
         * com `active == 49` resulta em 50, que é o teto permitido.
         * Um `>` simples deixaria a violação acontecer na fronteira.
         *
         * Chamado apenas pelo ramo em que `current.status == DONE` —
         * o `when` do [invoke] já garante `target != DONE`, então
         * nenhuma transição intermediária paga o custo desta consulta
         * e `DONE -> DONE` (que **diminui** a contagem) nunca é
         * bloqueada aqui.
         *
         * @throws ProjectTaskLimitReachedException se o projeto já
         *   estiver no teto.
         */
        private suspend fun ensureReopenFitsActiveCap(task: Task) {
            val active = repository.countActiveByProject(task.projectId)
            if (active >= MAX_ACTIVE_TASKS_PER_PROJECT) {
                throw ProjectTaskLimitReachedException(
                    projectId = task.projectId,
                    limit = MAX_ACTIVE_TASKS_PER_PROJECT,
                )
            }
        }

        private fun reconcileReminder(task: Task) {
            if (task.status == TaskStatus.DONE) {
                deadlineScheduler.cancel(task.id)
                return
            }
            val triggerAt = task.dueDate?.minus(DeadlineNotificationScheduler.REMINDER_LEAD)
            if (triggerAt != null && triggerAt.isAfter(Instant.now())) {
                deadlineScheduler.schedule(task.id, triggerAt)
            }
        }
    }
