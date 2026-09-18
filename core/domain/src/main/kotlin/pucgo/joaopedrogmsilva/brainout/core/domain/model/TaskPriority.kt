// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.model

/**
 * Prioridade de uma [Task].
 *
 * Mapeada em valores numéricos 0..4 conforme requisito do marco E1.4.
 * Persistência e ordenação devem usar [priorityCode] para garantir
 * estabilidade.
 */
enum class TaskPriority(val priorityCode: Int) {
    LOW(0),
    MEDIUM(1),
    HIGH(2),
    URGENT(3),
    CRITICAL(4),
    ;

    companion object {
        /** Limite inferior válido da prioridade conforme E1.4. */
        const val LOW_CODE: Int = 0

        /** Segundo nível da escala de prioridade. */
        const val MEDIUM_CODE: Int = 1

        /** Terceiro nível da escala de prioridade. */
        const val HIGH_CODE: Int = 2

        /** Quarto nível da escala de prioridade. */
        const val URGENT_CODE: Int = 3

        /** Limite superior válido da prioridade conforme E1.4. */
        const val CRITICAL_CODE: Int = 4

        /** Retorna o [TaskPriority] correspondente ao [code] (0..4). */
        fun fromCode(code: Int): TaskPriority = entries.firstOrNull { it.priorityCode == code }
            ?: throw IllegalArgumentException(
                "Prioridade inválida: $code (esperado entre $LOW_CODE e $CRITICAL_CODE)",
            )

        /** Intervalo válido conforme especificado pelo domínio. */
        val VALID_RANGE: IntRange = entries.minOf { it.priorityCode }..entries.maxOf { it.priorityCode }
    }
}
