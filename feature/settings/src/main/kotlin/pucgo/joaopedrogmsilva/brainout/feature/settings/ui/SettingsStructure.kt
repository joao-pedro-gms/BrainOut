// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Modelo da lista agrupada de configurações — usado pela SettingsScreen.
// Mantém a estrutura declarativa no mesmo arquivo para evitar uma camada
// de "state class" só por formalidade (a navegação do esqueleto E1.3 não
// precisa de ViewModel próprio).

package pucgo.joaopedrogmsilva.brainout.feature.settings.ui

import androidx.annotation.StringRes
import pucgo.joaopedrogmsilva.brainout.feature.settings.R

/**
 * Tipos de ação possíveis em uma linha da tela de configurações.
 *
 * - [Profile]/[Notifications]/[Theme] são placeholders navegáveis —
 *   não abrem sub-telas no E1.3 (ficam para E2.x).
 * - [SignOut] é a única ação que efetivamente troca a rota raiz
 *   (`settings → login` com `popUpTo(home) { inclusive = true }`).
 */
enum class SettingsActionType {
    Profile,
    Notifications,
    Theme,
    SignOut
}

data class SettingsOption(
    val type: SettingsActionType,
    @StringRes val labelRes: Int
)

data class SettingsSection(
    @StringRes val titleRes: Int,
    val options: List<SettingsOption>
)

/**
 * Estrutura estática das seções mostradas na `SettingsScreen`.
 *
 * Mantida no escopo do módulo porque só a própria tela consome — o
 * `:app` não precisa conhecer os tipos, apenas passar as callbacks.
 */
val SettingsSections: List<SettingsSection> = listOf(
    SettingsSection(
        titleRes = R.string.settings_section_account,
        options = listOf(
            SettingsOption(SettingsActionType.Profile, R.string.settings_option_profile),
            SettingsOption(SettingsActionType.Notifications, R.string.settings_option_notifications)
        )
    ),
    SettingsSection(
        titleRes = R.string.settings_section_appearance,
        options = listOf(
            SettingsOption(SettingsActionType.Theme, R.string.settings_option_theme)
        )
    ),
    SettingsSection(
        titleRes = R.string.settings_section_session,
        options = listOf(
            SettingsOption(SettingsActionType.SignOut, R.string.settings_option_signout)
        )
    )
)
