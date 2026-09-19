// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project

/**
 * Linha da tabela `projects` no Room.
 *
 * Mantém o mapeamento 1:1 com [Project] do domínio. O id é uma `String`
 * (UUID gerado em [Project.create]) e usamos como chave primária.
 *
 * O índice `idx_projects_owner_id` acelera a listagem por proprietário
 * usada pelo `HomeViewModel` (`observeAllForOwner`).
 *
 * @property id UUID do projeto (chave primária).
 * @property ownerId [Project.ownerId] — id do [pucgo.joaopedrogmsilva.brainout.core.domain.model.User]
 *  que possui o projeto (papel OWNER).
 * @property name Nome (1..120 caracteres, validado em [Project.init]).
 * @property description Descrição opcional (até 500 caracteres).
 * @property createdAt Instante de criação em epoch millis.
 * @property isCompleted Flag de conclusão do projeto.
 */
@Entity(
    tableName = "projects",
    indices = [
        Index(
            value = ["owner_id"],
            name = "idx_projects_owner_id",
        ),
    ],
)
data class ProjectEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "owner_id")
    val ownerId: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "description")
    val description: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,
) {

    /** Converte a linha para o modelo imutável de domínio [Project]. */
    fun toDomain(): Project = Project(
        id = id,
        name = name,
        description = description,
        ownerId = ownerId,
        createdAt = createdAt,
        isCompleted = isCompleted,
    )

    companion object {

        /** Constrói a entidade a partir de um [Project] de domínio. */
        fun fromDomain(project: Project): ProjectEntity = ProjectEntity(
            id = project.id,
            ownerId = project.ownerId,
            name = project.name,
            description = project.description,
            createdAt = project.createdAt,
            isCompleted = project.isCompleted,
        )
    }
}
