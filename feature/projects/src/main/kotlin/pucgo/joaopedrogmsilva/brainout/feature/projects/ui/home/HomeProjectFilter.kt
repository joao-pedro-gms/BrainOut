// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.annotation.StringRes
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

/** Filtro interno da lista, independente da navegação global. */
enum class HomeProjectFilter(
    @StringRes val labelRes: Int,
) {
    Active(R.string.home_filter_active),
    Completed(R.string.home_filter_completed),
}
