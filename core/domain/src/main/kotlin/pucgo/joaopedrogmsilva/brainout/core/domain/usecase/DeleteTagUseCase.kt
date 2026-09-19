// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import javax.inject.Inject
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TagRepository

/**
 * Caso de uso responsável por remover uma [pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag].
 *
 * Associações em `project_tags` são removidas via `ON DELETE CASCADE`
 * do schema Room — não é necessário tratamento adicional.
 */
class DeleteTagUseCase @Inject constructor(
    private val repository: TagRepository,
) {
    /** @param tagId Identificador da tag a remover. */
    suspend operator fun invoke(tagId: String) = repository.delete(tagId)
}
