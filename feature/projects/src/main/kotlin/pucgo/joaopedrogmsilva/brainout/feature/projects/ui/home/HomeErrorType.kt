// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

/**
 * Erro da Home, classificado.
 *
 * Antes o ViewModel emitia a **mensagem em português** e a UI deduzia
 * o tipo comparando a string — a mesma estratégia que já foi superada
 * em `:feature:auth` (`AuthErrorType`) e no detalhe de projeto
 * (`ProjectDetailError`).
 *
 * O tipo carrega a informação estruturada; a escolha do texto acontece
 * na UI, em pt/en.
 */
enum class HomeErrorType {
    /**
     * Falha de leitura (Room). A UI mostra o banner com "Tentar
     * novamente" — é o único erro que se resolve com retry.
     */
    LoadFailed,

    /**
     * Falha ao gravar numa ação do usuário (criar/editar/excluir). Vai
     * para o snackbar, não para o banner.
     */
    ActionFailed,

    /** Nome do projeto obrigatório — validação inline do diálogo. */
    NameRequired,

    /**
     * Já existe uma tag com esse nome para este usuário. O domínio
     * traduz a violação do índice UNIQUE para
     * [pucgo.joaopedrogmsilva.brainout.core.domain.error.DuplicateTagNameException]
     * (DEF-61).
     */
    DuplicateTagName,
}
