// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.model

import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidModelException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidStateTransitionException
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
 *
 * @property id Identificador único da tarefa.
 * @property projectId Identificador do [Project] ao qual pertence.
 * @property title Título (1..200 caracteres, não vazio).
 * @property priority Prioridade ([TaskPriority] 0..4).
 * @property status Estado atual ([TaskStatus]).
 * @property assigneeId [User.id] do responsável, ou null.
 * @property dueDate Data limite opcional.
 * @property createdAt Instante de criação em UTC.
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
) {
    init {
        require(id.isNotBlank()) { "id não pode ser vazio" }
        require(projectId.isNotBlank()) { "projectId não pode ser vazio" }
        requireValidTitle(title)
        requireValidPriority(priority)
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
     */
    fun transitionTo(target: TaskStatus): Task {
        if (target == status) return this
        if (!status.canTransitionTo(target)) {
            throw InvalidStateTransitionException(
                "Transição de status inválida: ${status.name} -> ${target.name}",
            )
        }
        return copy(status = target)
    }

    /**
     * Retorna nova instância com a prioridade alterada, validando o
     * intervalo válido.
     */
    fun changePriority(newPriority: TaskPriority): Task {
        requireValidPriority(newPriority)
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
