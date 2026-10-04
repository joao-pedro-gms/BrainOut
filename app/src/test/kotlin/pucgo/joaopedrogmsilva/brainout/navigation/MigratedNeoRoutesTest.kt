// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Guarda o critério de aceite do NB-06: a lista de rotas migradas ao
 * tema neo começa **vazia** — nenhuma tela é redesenhada
 * antecipadamente (plano §3.2.4, issue #93).
 *
 * Quando uma rota for migrada de fato (NB-19 em diante), a tarefa de
 * migração acrescenta a rota aqui **e** atualiza este teste junto com
 * o envolvimento da rota em [NeoThemeScope] — a lista não pode crescer
 * sem o conteúdo da tela ter mudado.
 */
class MigratedNeoRoutesTest {

    @Test
    fun `nenhuma rota esta migrada ao tema neo no NB-06`() {
        assertThat(migratedNeoRoutes).isEmpty()
    }
}
