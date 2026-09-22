// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Identifica as posições da bottom bar da Home mais o
// filtro interno "Ativos / Concluídos" (RN03 — E2.5).
//
// A posição "Configurações" é uma rota externa (settings) e não
// é uma tab interna do Scaffold — mas precisa estar presente para
// renderizar o item correspondente na NavigationBar.
//
// E2.7 — a posição "Painel" também é rota externa (dashboard,
// em :feature:tasks), acessível a partir da Home.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.annotation.StringRes
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

/**
 * Posições da bottom bar da [HomeScreen].
 *
 * - [Projects] é a aba interna — altera o conteúdo do Scaffold
 *   sem trocar a rota.
 * - [Tasks], [Dashboard] e [Settings] disparam navegação para rotas
 *   externas (`tasks`, `dashboard` e `settings`, ver
 *   `BrainOutNavHost`). Após a navegação, o estado local de tab perde
 *   relevância — a próxima vez que o usuário voltar para Home, ele
 *   reencontra o estado salvo via `rememberSaveable`.
 */
enum class HomeTab(@StringRes val labelRes: Int) {
    Projects(R.string.bottom_tab_projects),
    Tasks(R.string.bottom_tab_tasks),
    Dashboard(R.string.bottom_tab_dashboard),
    Settings(R.string.bottom_tab_settings)
}

/**
 * Filtro da lista de projetos dentro da aba [HomeTab.Projects]
 * (RN03 — E2.5). É uma enumeração separada de [HomeTab] porque
 * não ocupa um item da NavigationBar — é um filtro embutido na
 * lista de projetos, presente apenas na aba "Projetos".
 *
 * - [Active] exibe projetos com `is_completed = false`.
 * - [Completed] exibe projetos com `is_completed = true` — a aba
 *   dedicada pedida pelo ROADMAP E2.5.
 */
enum class HomeProjectFilter(@StringRes val labelRes: Int) {
    Active(R.string.home_filter_active),
    Completed(R.string.home_filter_completed),
}
