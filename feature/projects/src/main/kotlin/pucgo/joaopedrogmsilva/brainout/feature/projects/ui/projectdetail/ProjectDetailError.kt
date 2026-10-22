// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

/**
 * Erro do detalhe de projeto, classificado.
 *
 * Antes o ViewModel emitia a **mensagem em português** e a UI
 * reconhecia o tipo comparando a string — inclusive extraindo o limite
 * de tarefas por prefixo e sufixo, e reconhecendo RN02 por uma
 * tag embutida no texto. Tocar no texto quebrava a classificação, e o
 * limite exibido vinha da constante global, não do limite real.
 *
 * O tipo carrega a informação estruturada; a escolha do texto acontece
 * na UI, em pt/en — o mesmo desenho de `AuthErrorType` em
 * `:feature:auth`.
 */
sealed interface ProjectDetailError {
    /**
     * Falha de leitura (Room). A UI mostra o banner com "Tentar
     * novamente" — é o único erro que se resolve com retry.
     */
    data object LoadFailed : ProjectDetailError

    /** RN01 — o projeto já tem o limite de tarefas ativas. */
    data class TaskLimitReached(
        val limit: Int,
    ) : ProjectDetailError

    /** Título vazio. */
    data object EmptyTitle : ProjectDetailError

    /** Título inválido (regra do domínio). */
    data object InvalidTitle : ProjectDetailError

    /** Transição de status não permitida. */
    data object InvalidTransition : ProjectDetailError

    /** RN02 — prioridade não pode mudar em tarefa concluída. */
    data object PriorityLocked : ProjectDetailError

    /**
     * O projeto não existe mais (foi excluído com a tela aberta).
     * O domínio lança [pucgo.joaopedrogmsilva.brainout.core.domain.error.ProjectNotFoundException]
     * em vez de deixar a violação de chave estrangeira do Room
     * escapar como `SQLiteConstraintException` (DEF-56).
     */
    data object ProjectNotFound : ProjectDetailError

    /** Falha ao gravar (banco, I/O) numa ação do usuário. */
    data object ActionFailed : ProjectDetailError
}
