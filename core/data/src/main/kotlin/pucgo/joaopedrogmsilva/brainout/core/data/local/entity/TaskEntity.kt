// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskPriority
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus

/**
 * Linha da tabela `tasks` no Room.
 *
 * Mantém o mapeamento 1:1 com [Task] do domínio. O id é uma `String`
 * (UUID gerado em [Task.create]) e usamos como chave primária.
 *
 * A coluna `project_id` aponta para `projects.id` com `ON DELETE CASCADE`
 * — apagar um projeto remove automaticamente suas tarefas. Isso
 * evita tarefas órfãs e mantém a consistência da UI.
 *
 * `priority_code` persiste o [TaskPriority.priorityCode] (0..4) como
 * inteiro, garantindo estabilidade frente a reordenamentos futuros do
 * enum. `status` persiste o [TaskStatus.name] textual (TODO/DOING/DONE)
 * para sobreviver a upgrades da enum.
 *
 * `completed_at` (RN03 — E2.5) foi adicionado em v3 como `INTEGER`
 * nullable em epoch millis. Tarefas preexistentes ficam com `null`
 * (não inventamos data retroativa). É populado quando a tarefa
 * entra em [TaskStatus.DONE] e zerado quando é reaberta.
 *
 * @property id UUID da tarefa (chave primária).
 * @property projectId [Task.projectId] — id do projeto dono da tarefa.
 * @property title Título (1..200 caracteres, validado em [Task.init]).
 * @property priorityCode [TaskPriority.priorityCode] — 0..4 conforme E1.4.
 * @property status Nome do [TaskStatus] (TODO, DOING ou DONE).
 * @property assigneeId [Task.assigneeId] opcional — id do responsável.
 * @property dueDate Prazo opcional em epoch millis.
 * @property createdAt Instante de criação em epoch millis.
 * @property completedAt Instante em que a tarefa passou a [TaskStatus.DONE].
 *  Null enquanto não concluída, ou em registros legados.
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["project_id"], name = "idx_tasks_project_id"),
        Index(value = ["status"], name = "idx_tasks_status"),
    ],
)
data class TaskEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "project_id")
    val projectId: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "priority_code")
    val priorityCode: Int,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "assignee_id")
    val assigneeId: String?,
    @ColumnInfo(name = "due_date")
    val dueDate: Instant?,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "completed_at", defaultValue = "NULL")
    val completedAt: Instant? = null,
) {

    /**
     * Converte a linha para o modelo imutável de domínio [Task].
     *
     * Usa [TaskPriority.fromCodeOrThrow] para que prioridade fora da
     * faixa válida (0..4) falhe explicitamente em vez de retornar null
     * — protege contra corrupção do banco.
     */
    fun toDomain(): Task = Task(
        id = id,
        projectId = projectId,
        title = title,
        priority = TaskPriority.fromCodeOrThrow(priorityCode),
        status = TaskStatus.valueOf(status),
        assigneeId = assigneeId,
        dueDate = dueDate,
        createdAt = createdAt,
        completedAt = completedAt,
    )

    companion object {

        /** Constrói a entidade a partir de uma [Task] de domínio. */
        fun fromDomain(task: Task): TaskEntity = TaskEntity(
            id = task.id,
            projectId = task.projectId,
            title = task.title,
            priorityCode = task.priority.priorityCode,
            status = task.status.name,
            assigneeId = task.assigneeId,
            dueDate = task.dueDate,
            createdAt = task.createdAt,
            completedAt = task.completedAt,
        )
    }
}
