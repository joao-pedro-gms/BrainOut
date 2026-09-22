// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.sync

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Fachada da fila de sincronização para a camada de apresentação
 * (E3.4, item 2).
 *
 * Expõe a contagem de operações pendentes de sincronização
 * (`pending_ops`) como [Flow] reativo — emitida a cada mudança da
 * tabela (invalidation tracker do Room). A UI traduz o valor em
 * "X alterações aguardando sincronização".
 *
 * Fica em `:core:data` (e não o DAO direto nos feature modules) para
 * manter a tabela e o payload da fila como detalhe interno da camada
 * de dados: os ViewModels consomem um número, não o schema.
 */
@Singleton
class PendingSyncMonitor @Inject constructor(
    private val pendingOpDao: pucgo.joaopedrogmsilva.brainout.core.data.local.dao.PendingOpDao,
) {

    /**
     * Contagem de operações pendentes de sincronização. 0 = fila
     * vazia (tudo sincronizado); > 0 = há alterações locais ainda
     * não confirmadas pela retaguarda.
     */
    fun observePendingCount(): Flow<Int> = pendingOpDao.observeCount()
}
