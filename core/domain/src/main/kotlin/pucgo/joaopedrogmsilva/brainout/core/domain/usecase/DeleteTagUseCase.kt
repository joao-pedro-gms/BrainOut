// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TagRepository
import javax.inject.Inject

/**
 * Caso de uso responsável por remover uma [pucgo.joaopedrogmsilva.brainout.core.domain.model.Tag].
 *
 * Associações em `project_tags` são removidas em cascata pela
 * implementação — não é necessário tratamento adicional aqui.
 */
class DeleteTagUseCase
    @Inject
    constructor(
        private val repository: TagRepository,
    ) {
        /** @param tagId Identificador da tag a remover. */
        suspend operator fun invoke(tagId: String) = repository.delete(tagId)
    }
