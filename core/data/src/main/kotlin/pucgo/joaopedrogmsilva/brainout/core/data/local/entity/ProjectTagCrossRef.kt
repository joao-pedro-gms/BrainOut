// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
@file:Suppress("ConstructorParameterNaming")
package pucgo.joaopedrogmsilva.brainout.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Tabela de junção N:N entre [ProjectEntity] e [TagEntity].
 *
 * Representa o conjunto de tags associadas a cada projeto. A chave
 * primária composta `(project_id, tag_id)` impede duplicações da mesma
 * associação — o `OnConflictStrategy.IGNORE` no `ProjectDao.insertProjectTags`
 * tira proveito disso para tornar o insert idempotente.
 *
 * Ambas as FKs usam `ON DELETE CASCADE`:
 * - Apagar um [ProjectEntity] remove todas as suas associações aqui.
 * - Apagar uma [TagEntity] remove todas as suas associações aqui.
 *
 * Isso mantém a invariante de consistência: nunca existe uma linha
 * em `project_tags` apontando para um `project_id` ou `tag_id` que
 * não existe.
 *
 * @property project_id FK para [ProjectEntity.id].
 * @property tag_id FK para [TagEntity.id].
 */
@Entity(
    tableName = "project_tags",
    primaryKeys = ["project_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["tag_id"], name = "idx_project_tags_tag_id"),
    ],
)
data class ProjectTagCrossRef(
    val project_id: String,
    val tag_id: String,
)
