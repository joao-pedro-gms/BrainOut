// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.error

class TagNotFoundException(tagId: String) :
    RuntimeException("Tag não encontrada: $tagId")
