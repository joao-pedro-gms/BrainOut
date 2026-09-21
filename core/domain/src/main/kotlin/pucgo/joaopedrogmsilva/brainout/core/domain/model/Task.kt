// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.model

import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidStateTransitionException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.TaskPriorityChangeForbiddenException
import java.time.Instant
import java.util.UUID

/**
 * Tarefa pertencente a um [Project].
 *
 * Entidade imutável: mutações retornam novas instâncias via [copy] ou
 * métodos de domínio ([transitionTo], [changePriority], [reassign]).
 *
 * Regras de domínio aplicadas em [init] e métodos:
 * - [title] entre 1 e 200 caracteres após trim.
 * - [priority] sempre em [TaskPriority.VALID_RANGE].
 * - [status] segue a matriz de [TaskStatus.canTransitionTo].
 * - RN02 (E2.4): alterar prioridade em uma tarefa já concluída é
 *   proibido — [changePriority] lança [BusinessRuleException]
 *   quando [status] é [TaskStatus.DONE].
 * - RN03 (E2.5): [completedAt] é populado quando [status] é
 *   [TaskStatus.DONE] e zerado (null) quando a tarefa volta a ser
 *   ativa. Tarefas antigas (criadas antes desta versão) permanecem
 *   com `completedAt = null`.
 *
 * @property id Identificador único da tarefa.
 * @property projectId Identificador do [Project] ao qual pertence.
 * @property title Título (1..200 caracteres, não vazio).
 * @property priority Prioridade ([TaskPriority] 0..4).
 * @property status Estado atual ([TaskStatus]).
 * @property assigneeId [User.id] do responsável, ou null.
 * @property dueDate Data limite opcional.
 * @property createdAt Instante de criação em UTC.
 * @property completedAt Instante em que a tarefa passou a [TaskStatus.DONE]
 *  (RN03). Null enquanto a tarefa não foi concluída ou se a tarefa
 *  existia antes da introdução da coluna.
 */
data class Task(
    val id: String,
    val projectId: String,
    val title: String,
    val priority: TaskPriority,
    val status: TaskStatus,
    val assigneeId: String?,
    val dueDate: Instant?,
    val createdAt: Instant,
    val completedAt: Instant? = null,
) {
    init {
        require(id.isNotBlank()) { "id não pode ser vazio" }
        require(projectId.isNotBlank()) { "projectId não pode ser vazio" }
        requireValidTitle(title)
        requireValidPriority(priority)
        // RN03: completedAt só faz sentido para tarefas concluídas.
        // Em migração, tarefas antigas podem vir DONE sem completedAt
        // (legacy null), e tarefas ativas nunca podem ter
        // completedAt definido — quem controla o registro é
        // [transitionTo], não a entrada direta.
        if (completedAt != null && status != TaskStatus.DONE) {
            throw InvalidModelException(
                "completedAt só pode ser definido em tarefas com status DONE",
            )
        }
    }

    companion object {
        const val MAX_TITLE_LENGTH: Int = 200

        fun create(
            projectId: String,
            title: String,
            priority: TaskPriority = TaskPriority.MEDIUM,
            status: TaskStatus = TaskStatus.TODO,
            assigneeId: String? = null,
            dueDate: Instant? = null,
            completedAt: Instant? = null,
            now: Instant = Instant.now(),
        ): Task = Task(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            title = title,
            priority = priority,
            status = status,
            assigneeId = assigneeId,
            dueDate = dueDate,
            createdAt = now,
            completedAt = completedAt,
        )

        fun requireValidTitle(raw: String) {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) {
                throw InvalidModelException("Título da tarefa não pode ser vazio")
            }
            if (trimmed.length > MAX_TITLE_LENGTH) {
                throw InvalidModelException(
                    "Título da tarefa deve ter no máximo $MAX_TITLE_LENGTH caracteres",
                )
            }
        }

        fun requireValidPriority(priority: TaskPriority) {
            if (priority.priorityCode !in TaskPriority.VALID_RANGE) {
                throw InvalidModelException(
                    "Prioridade fora do intervalo permitido: ${priority.priorityCode}",
                )
            }
        }
    }

    /**
     * Retorna nova instância com o estado alterado, respeitando a matriz
     * de transições de [TaskStatus]. Transição para o mesmo estado é
     * no-op e devolve a própria instância.
     *
     * RN03: a transição para DONE registra [now] em [completedAt]; a
     * reabertura (DONE → DOING ou DOING → TODO) limpa o campo. Outras
     * transições não tocam em [completedAt].
     */
    fun transitionTo(target: TaskStatus): Task {
        if (target == status) return this
        if (!status.canTransitionTo(target)) {
            throw InvalidStateTransitionException(
                "Transição de status inválida: ${status.name} -> ${target.name}",
            )
        }
        val newCompletedAt = when {
            // entrada em DONE (vindo de TODO/DOING) → carimba completedAt
            target == TaskStatus.DONE && completedAt == null -> Instant.now()
            // saída de DONE → limpa completedAt
            status == TaskStatus.DONE && target != TaskStatus.DONE -> null
            // outras transições preservam o valor (idempotente em ciclos)
            else -> completedAt
        }
        return copy(status = target, completedAt = newCompletedAt)
    }

    /**
     * Retorna nova instância com a prioridade alterada, validando o
     * intervalo válido.
     *
     * **RN02 (E2.4):** alterar a prioridade de uma tarefa já
     * concluída é proibido. O domínio lança
     * [BusinessRuleException] quando [status] é [TaskStatus.DONE],
     * garantindo que o chip de prioridade de uma tarefa concluída
     * permaneça imutável na UI e na persistência. Tarefas ativas
     * (TODO/DOING) podem ter a prioridade alterada normalmente.
     */
    fun changePriority(newPriority: TaskPriority): Task {
        requireValidPriority(newPriority)
        if (status == TaskStatus.DONE) {
            throw TaskPriorityChangeForbiddenException(id)
        }
        if (newPriority == priority) return this
        return copy(priority = newPriority)
    }

    /** Atribui ou remove (atribuindo null) o responsável pela tarefa. */
    fun reassign(newAssigneeId: String?): Task {
        if (newAssigneeId == assigneeId) return this
        return copy(assigneeId = newAssigneeId)
    }

    /** Atualiza o título, validando invariantes. */
    fun rename(newTitle: String): Task {
        requireValidTitle(newTitle)
        if (newTitle == title) return this
        return copy(title = newTitle)
    }

    /** Atualiza a data limite (pode ser null para remover). */
    fun changeDueDate(newDueDate: Instant?): Task {
        if (newDueDate == dueDate) return this
        return copy(dueDate = newDueDate)
    }
}
