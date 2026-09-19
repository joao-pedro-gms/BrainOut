// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag

/**
 * Linha da tabela `tags` no Room.
 *
 * Mantém o mapeamento 1:1 com [Tag] do domínio. O id é uma `String`
 * (UUID gerado em [Tag.create]) e usamos como chave primária.
 *
 * O índice único composto `idx_tags_owner_name` sobre
 * `(owner_id, name)` garante a invariante de unicidade do nome por
 * usuário: o mesmo `User` não pode ter duas tags com o mesmo nome.
 * O `OnConflictStrategy.ABORT` no `TagDao.insert` propaga essa
 * violação como `SQLiteConstraintException`, que o repositório
 * converte em erro de domínio.
 *
 * @property id UUID da tag (chave primária).
 * @property ownerId [Tag.ownerId] — id do [pucgo.joaopedrogmsilva.brainout.core.domain.model.User]
 *  dono da tag.
 * @property name Nome canônico (1..30 caracteres, sem espaços nas pontas).
 * @property color Cor no formato `#RRGGBB` validado em [Tag.init].
 * @property createdAt Instante de criação em epoch millis.
 */
@Entity(
    tableName = "tags",
    indices = [
        Index(
            value = ["owner_id", "name"],
            name = "idx_tags_owner_name",
            unique = true,
        ),
    ],
)
data class TagEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "owner_id")
    val ownerId: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "color")
    val color: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
) {

    /** Converte a linha para o modelo imutável de domínio [Tag]. */
    fun toDomain(): Tag = Tag(
        id = id,
        ownerId = ownerId,
        name = name,
        color = color,
        createdAt = createdAt,
    )

    companion object {

        /** Constrói a entidade a partir de uma [Tag] de domínio. */
        fun fromDomain(tag: Tag): TagEntity = TagEntity(
            id = tag.id,
            ownerId = tag.ownerId,
            name = tag.name,
            color = tag.color,
            createdAt = tag.createdAt,
        )
    }
}
