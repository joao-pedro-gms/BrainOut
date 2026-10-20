// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.model

import java.time.Instant

/**
 * Resumo do conteúdo de um backup local.
 *
 * Descreve **quantos** registros de cada tipo o arquivo carrega — é o
 * contrato mínimo que a tela precisa para informar o usuário do que
 * será exportado/restaurado, sem expor o conteúdo (nomes de projeto,
 * títulos de tarefa) fora do app.
 *
 * @property createdAt instante em que o backup foi gerado (não o do
 *  último registro exportado).
 * @property users contas locais incluídas no backup.
 * @property projects projetos incluídos no backup.
 * @property tasks tarefas incluídas no backup.
 * @property tags etiquetas incluídas no backup.
 */
data class BackupSummary(
    val createdAt: Instant,
    val users: Int,
    val projects: Int,
    val tasks: Int,
    val tags: Int,
)
