// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import pucgo.joaopedrogmsilva.brainout.core.data.local.LocalTransaction

/**
 * [LocalTransaction] que executa o bloco sem abrir transação.
 *
 * As suítes de repositório usam DAO falso, então não há rollback do
 * Room a reproduzir: o que elas verificam é a **atomicidade** da
 * gravação local — que a transação real garante e o fake não altera.
 * A implementação estava copiada em três suítes.
 *
 * A escrita dupla (Room + `pending_ops`) deixou de existir com a
 * remoção da camada de rede (#212) — não há mais ordem de
 * enfileiramento para capturar.
 */
internal class FakeLocalTransaction : LocalTransaction {
    override suspend fun <T> run(block: suspend () -> T): T = block()
}
