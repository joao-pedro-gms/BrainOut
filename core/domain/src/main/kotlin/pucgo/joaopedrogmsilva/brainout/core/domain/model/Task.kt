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
 * métodos de domínio ([transitionTo], [changePriority], [rename]).
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
        ): Task =
            Task(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                // DEF-57 — a validação olha o título aparado, então o
                // título gravado também é: sem isto, o banco guardava os
                // espaços que o usuário digitou.
                title = title.trim(),
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
     * RN03: a transição para DONE registra [now] em [completedAt]; toda
     * outra transição devolve a tarefa ativa, então o carimbo é zerado.
     *
     * @param now Instante carimbado em [completedAt] ao entrar em
     *   [TaskStatus.DONE]. É injetável — como o `now` de [create] — para
     *   que o teste afirme o valor exato em vez de um intervalo.
     */
    fun transitionTo(
        target: TaskStatus,
        now: Instant = Instant.now(),
    ): Task {
        if (target == status) return this
        if (!status.canTransitionTo(target)) {
            throw InvalidStateTransitionException(
                "Transição de status inválida: ${status.name} -> ${target.name}",
            )
        }
        // RN03: só existe carimbo em tarefa concluída — o construtor
        // rejeita `completedAt` fora de DONE —, então nenhuma transição
        // precisa preservar o valor: entrar em DONE carimba [now] e
        // qualquer outra saída devolve a tarefa ativa, sem carimbo.
        val newCompletedAt = if (target == TaskStatus.DONE) now else null
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

    /** Atualiza o título, validando invariantes. */
    fun rename(newTitle: String): Task {
        // DEF-57 — o título é aparado antes de validar e de gravar: a
        // validação já olhava o valor aparado, então guardar o texto
        // cru deixava o banco com os espaços que o usuário digitou.
        val trimmed = newTitle.trim()
        requireValidTitle(trimmed)
        if (trimmed == title) return this
        return copy(title = trimmed)
    }
}
