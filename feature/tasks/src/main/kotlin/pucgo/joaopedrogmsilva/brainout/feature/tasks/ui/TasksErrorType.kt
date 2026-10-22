// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.tasks.ui

/**
 * Erro das telas de Tarefas e Painel, classificado.
 *
 * Antes cada ViewModel emitia a **mensagem em português** e a UI
 * deduzia o tipo comparando a string. O tipo é compartilhado pelas duas
 * telas porque o *significado* do erro é o mesmo — "a leitura do Room
 * falhou" —; o **texto** continua sendo de cada tela
 * (`tasks_error_load_failed` e `dashboard_error_load_failed`).
 */
enum class TasksErrorType {
    /**
     * Falha de leitura (Room). A UI mostra o banner com "Tentar
     * novamente" — é o único erro que se resolve com retry.
     */
    LoadFailed,
}
