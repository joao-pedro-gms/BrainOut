// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

/**
 * Linha do JOIN entre `tags` e `project_tags`: uma tag e o projeto que
 * a referencia.
 *
 * Existe para que a observação das tags de **vários** projetos aconteça
 * em **uma** consulta do Room, em vez de uma consulta por projeto
 * combinada no repositório.
 *
 * @property tag A tag associada.
 * @property projectId Id do projeto dono da associação.
 */
data class ProjectTagRow(
    @Embedded val tag: TagEntity,
    @ColumnInfo(name = "project_id") val projectId: String,
)
