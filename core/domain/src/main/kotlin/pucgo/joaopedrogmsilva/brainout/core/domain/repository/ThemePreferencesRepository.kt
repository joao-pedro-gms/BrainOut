// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pucgo.joaopedrogmsilva.brainout.core.domain.model.ThemeMode

/**
 * Contrato de persistência da preferência de tema (NB-06, issue #93,
 * E4.5). Implementação DataStore em `:core:data`
 * (`ThemePreferencesRepositoryImpl`).
 *
 * Escopo deliberado — infraestrutura mínima, sem antecipar NB-27:
 *
 * - **Uma única chave, por dispositivo.** O tema não depende do
 *   usuário ativo: sobrevive a logout/login, como qualquer
 *   preferência de interface (ao contrário de
 *   [ListingPreferencesRepository], que é por conta).
 * - **Somente leitura/escrita do modo.** Não expõe telas, rótulos ou
 *   confirmação de modo efetivo — isso é NB-27.
 * - **Reativo.** [observe] emite o valor atual e cada mudança, para a
 *   raiz do app trocar o tema sem reiniciar («mudança instantânea» é
 *   capacidade nova, marcada como trabalho no plano §14).
 *
 * Default de fábrica: [ThemeMode.SYSTEM] — mesmo comportamento do
 * app antes do NB-06 (alternância automática), então a presença
 * desta porta não muda tela nenhuma por si só.
 */
interface ThemePreferencesRepository {

    /** Observa o modo persistido; emite [ThemeMode.SYSTEM] antes da primeira escrita. */
    fun observe(): Flow<ThemeMode>

    /** Grava a escolha manual ([ThemeMode.LIGHT]/[ThemeMode.DARK]) ou volta para [ThemeMode.SYSTEM]. */
    suspend fun set(mode: ThemeMode)
}
