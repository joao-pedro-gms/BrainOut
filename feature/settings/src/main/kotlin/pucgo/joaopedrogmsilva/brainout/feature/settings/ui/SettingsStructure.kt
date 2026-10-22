// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Modelo da lista agrupada de configurações — usado pela SettingsScreen.
// Mantém a estrutura declarativa no mesmo arquivo para evitar uma camada
// de "state class" só por formalidade (a navegação do esqueleto E1.3 não
// precisa de ViewModel próprio).

package pucgo.joaopedrogmsilva.brainout.feature.settings.ui

import androidx.annotation.StringRes
import pucgo.joaopedrogmsilva.brainout.feature.settings.R

/**
 * Natureza de uma linha de Configurações.
 *
 * O catálogo é interno do módulo: o `NavHost` só precisa saber
 * **encerrar a sessão**, e é isso que a tela despacha. As demais
 * linhas são tratadas dentro da própria tela (backup) ou ainda não
 * têm destino (DEF-44).
 */
internal enum class SettingsRowKind {
    /** Abre outra rota. Hoje só "Sair" — o `NavHost` executa. */
    SignOut,

    /** Executa uma ação local (as duas linhas de backup). */
    Local,

    /** Ainda não tem destino: o toque responde "Em breve". */
    Placeholder,
}

/**
 * Uma linha de Configurações.
 *
 * @property kind o que o toque na linha faz.
 * @property id identificador estável da linha (usado no test tag).
 * @property labelRes rótulo localizado.
 */
internal data class SettingsOption(
    val kind: SettingsRowKind,
    val id: String,
    @StringRes val labelRes: Int,
)

internal data class SettingsSection(
    @StringRes val titleRes: Int,
    val options: List<SettingsOption>,
)

/**
 * Estrutura estática das seções mostradas na `SettingsScreen`.
 *
 * Mantida no escopo do módulo porque só a própria tela consome — o
 * `:app` não precisa conhecer os tipos, apenas passar as callbacks.
 */
internal val SettingsSections: List<SettingsSection> =
    listOf(
        SettingsSection(
            titleRes = R.string.settings_section_account,
            options =
                listOf(
                    SettingsOption(SettingsRowKind.Placeholder, "profile", R.string.settings_option_profile),
                    SettingsOption(
                        SettingsRowKind.Placeholder,
                        "notifications",
                        R.string.settings_option_notifications,
                    ),
                ),
        ),
        SettingsSection(
            titleRes = R.string.settings_section_appearance,
            options =
                listOf(
                    SettingsOption(SettingsRowKind.Placeholder, "theme", R.string.settings_option_theme),
                ),
        ),
        SettingsSection(
            titleRes = R.string.settings_section_session,
            options =
                listOf(
                    SettingsOption(SettingsRowKind.SignOut, "signout", R.string.settings_option_signout),
                ),
        ),
    )
