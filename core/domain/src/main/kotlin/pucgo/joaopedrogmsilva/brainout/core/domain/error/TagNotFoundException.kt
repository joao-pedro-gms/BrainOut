// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.error

/**
 * Tag referenciada por uma operação não existe (R5).
 *
 * Estende [DomainException] para que camadas superiores capturem a
 * hierarquia de domínio de forma agregada — não `RuntimeException`
 * crua (regra do `core/domain/AGENTS.md`).
 */
class TagNotFoundException(tagId: String) :
    DomainException("Tag não encontrada: $tagId")
