// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.model

/**
 * Prioridade de uma [Task].
 *
 * Mapeada em valores numéricos 0..4 conforme requisito do marco E1.4.
 * Persistência e ordenação devem usar [priorityCode] para garantir
 * estabilidade.
 */
enum class TaskPriority(
    val priorityCode: Int,
) {
    LOW(0),
    MEDIUM(1),
    HIGH(2),
    URGENT(3),
    CRITICAL(4),
    ;

    companion object {
        /** Intervalo válido conforme especificado pelo domínio. */
        val VALID_RANGE: IntRange = entries.minOf { it.priorityCode }..entries.maxOf { it.priorityCode }

        /** Códigos válidos em ordem crescente (0..4) — uso agregado E2.7. */
        val VALID_CODES: List<Int> = entries.map { it.priorityCode }

        /** Retorna o [TaskPriority] correspondente ao [code] (0..4). */
        fun fromCode(code: Int): TaskPriority =
            entries.firstOrNull { it.priorityCode == code }
                ?: throw IllegalArgumentException(
                    "Prioridade inválida: $code (esperado entre ${VALID_RANGE.first} e ${VALID_RANGE.last})",
                )
    }
}
