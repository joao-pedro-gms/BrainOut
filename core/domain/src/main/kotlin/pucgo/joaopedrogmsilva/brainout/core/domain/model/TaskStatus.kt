// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.model

/**
 * Estado de uma [Task] dentro do fluxo de trabalho.
 *
 * Transições válidas:
 * - [TODO] -> [DOING]
 * - [DOING] -> [DONE]
 * - [DOING] -> [TODO]  (reabertura parcial)
 * - [DONE] -> [DOING]  (reabertura total)
 *
 * Transições proibidas (regra do domínio E1.4):
 * - [DONE] -> [TODO]   (não é permitido regredir direto)
 * - Qualquer transição para o mesmo estado é no-op (ver [Task.transitionTo]).
 */
enum class TaskStatus {
    TODO,
    DOING,
    DONE,
    ;

    /**
     * Indica se a transição deste estado para [target] é permitida.
     *
     * Implementa a matriz:
     * - TODO  -> DOING ✓ | DONE ✗
     * - DOING -> TODO  ✓ | DONE ✓
     * - DONE  -> DOING ✓ | TODO ✗
     */
    fun canTransitionTo(target: TaskStatus): Boolean = when (this) {
        TODO -> target == DOING
        DOING -> target == TODO || target == DONE
        DONE -> target == DOING
    }
}
