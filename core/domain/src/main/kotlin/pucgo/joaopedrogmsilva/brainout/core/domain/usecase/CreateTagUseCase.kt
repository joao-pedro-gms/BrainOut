// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import javax.inject.Inject
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TagRepository

/**
 * Caso de uso responsável por criar uma nova [Tag].
 *
 * Constrói a entidade via [Tag.create] (valida nome e cor) e
 * delega a persistência ao [TagRepository]. O índice único
 * `(owner_id, name)` do Room propaga colisões como
 * `SQLiteConstraintException`; uma camada superior (ex.:
 * `ViewModel`) deve capturar e converter para erro de domínio
 * quando necessário.
 */
class CreateTagUseCase @Inject constructor(
    private val repository: TagRepository,
) {
    /**
     * @param ownerId [Tag.ownerId] — id do usuário dono da tag.
     * @param name Nome (1..30 caracteres, sem espaços nas pontas).
     * @param color Cor no formato `#RRGGBB`.
     */
    suspend operator fun invoke(ownerId: String, name: String, color: String): Tag =
        repository.create(Tag.create(ownerId = ownerId, name = name, color = color))
}
