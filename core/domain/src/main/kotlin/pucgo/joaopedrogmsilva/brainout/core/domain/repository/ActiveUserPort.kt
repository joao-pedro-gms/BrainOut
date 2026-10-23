// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.repository

import pucgo.joaopedrogmsilva.brainout.core.domain.model.User

/**
 * Porta do **usuário ativo** — quem está autenticado agora.
 *
 * Existe para que o domínio possa perguntar "esta pessoa tem a
 * permissão?" sem conhecer a origem da sessão: a implementação em
 * `:core:data` lê o `SessionStore`/`Room`, mas a regra de permissão
 * vive no domínio.
 *
 * A porta é `suspend` e **pontual** (não um `Flow`): quem consulta a
 * permissão está prestes a executar uma mutação, e precisa do valor
 * agora — não de uma assinatura reativa.
 */
interface ActiveUserPort {
    /**
     * Usuário autenticado no momento, ou `null` quando não há sessão.
     */
    suspend fun currentUser(): User?
}
